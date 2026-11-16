#!/usr/bin/env python3
"""현재 작업공간의 Codex 대화 기록을 찾습니다."""

import argparse
from collections import defaultdict
from contextlib import closing, contextmanager
from dataclasses import asdict, dataclass
from functools import lru_cache
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import sqlite3
import subprocess
import sys


THREAD_ID = re.compile(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
MAX_HEADER_BYTES = 2 * 1024 * 1024


class Unavailable(Exception):
    """The requested scope or authoritative rollout could not be established."""


@dataclass(frozen=True)
class Rollout:
    thread_id: str
    path: str
    cwd: str
    worktree: str
    modified_at: int
    timestamp: str | None
    parent_thread_id: str | None
    archived: bool


def worktrees(workspace):
    result = subprocess.run(
        ["git", "-C", str(workspace), "worktree", "list", "--porcelain", "-z"],
        capture_output=True,
        check=False,
    )
    if result.returncode:
        raise Unavailable("Git 작업공간이 필요합니다.")
    roots = [
        Path(os.fsdecode(record[len(b"worktree "):])).resolve()
        for record in result.stdout.split(b"\0")
        if record.startswith(b"worktree ")
    ]
    if not roots:
        raise Unavailable("Git 작업공간에 worktree가 없습니다.")
    return sorted(set(roots), key=lambda path: (-len(path.parts), str(path)))


@contextmanager
def open_rollout(path):
    if path.suffix != ".zst":
        with path.open("rb") as stream:
            yield stream
        return

    if not shutil.which("zstd"):
        raise Unavailable("압축된 대화 기록을 읽으려면 zstd가 필요합니다.")
    with subprocess.Popen(
        ["zstd", "-q", "-d", "-c", "--", str(path)],
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    ) as process:
        try:
            yield process.stdout
            if process.stdout.closed:
                if process.wait() != 0:
                    raise Unavailable("압축된 대화 기록을 해제할 수 없습니다.")
        finally:
            if process.poll() is None:
                process.terminate()


def rollout_records(stream):
    for line in stream:
        try:
            record = json.loads(line)
        except (ValueError, UnicodeError) as error:
            if not line.endswith(b"\n"):
                break  # The writer can still be appending this record.
            raise Unavailable("대화 기록에 형식이 잘못된 항목이 있습니다.") from error
        if not isinstance(record, dict):
            raise Unavailable("대화 기록에 형식이 잘못된 항목이 있습니다.")
        yield record
    stream.close()


def parent_thread_id(payload):
    parent = payload.get("parent_thread_id")
    source = payload.get("source")
    if not parent and isinstance(source, dict):
        subagent = source.get("subagent")
        spawn = subagent.get("thread_spawn") if isinstance(subagent, dict) else None
        parent = spawn.get("parent_thread_id") if isinstance(spawn, dict) else None
    return parent if isinstance(parent, str) and THREAD_ID.fullmatch(parent) else None


class History:
    def __init__(self, workspace, codex_home, include_archived=False):
        self.worktrees = worktrees(workspace)
        self.home = codex_home.resolve()
        self.stores = [(self.home / "sessions", False)]
        if include_archived:
            self.stores.append((self.home / "archived_sessions", True))

    @lru_cache(maxsize=512)
    def owner(self, cwd):
        if not isinstance(cwd, str) or not Path(cwd).is_absolute():
            return None
        resolved = Path(cwd).resolve()
        for root in self.worktrees:
            if not resolved.is_relative_to(root):
                continue
            candidate = resolved
            while candidate != root:
                if (candidate / ".git").exists():
                    return None  # An independent Git project nested in the workspace.
                candidate = candidate.parent
            return root
        return None

    def index_path(self):
        databases = []
        for path in self.home.glob("state_*.sqlite"):
            version = path.stem.removeprefix("state_")
            if version.isdecimal() and path.is_file():
                databases.append((int(version), path))
        return max(databases)[1] if databases else None

    @contextmanager
    def index(self):
        path = self.index_path()
        if path is None:
            yield None
            return
        try:
            with closing(sqlite3.connect(path.as_uri() + "?mode=ro", uri=True, timeout=2)) as database:
                if database.execute("SELECT 1 FROM sqlite_master WHERE name='threads' AND type='table'").fetchone():
                    yield database
                else:
                    yield None
        except sqlite3.Error as error:
            raise Unavailable("Codex 상태 인덱스를 읽을 수 없습니다.") from error

    def location(self, path):
        if not isinstance(path, Path) or not path.is_absolute():
            return None
        if not path.name.startswith("rollout-") or not path.name.endswith((".jsonl", ".jsonl.zst")):
            return None
        resolved = path.resolve()
        for store, archived in self.stores:
            if store.is_symlink() or not resolved.is_relative_to(store):
                continue
            if resolved.is_file():
                return resolved, archived
        return None

    def metadata(self, path):
        location = self.location(path)
        if location is None:
            return None
        path, archived = location
        with open_rollout(path) as stream:
            header = stream.readline(MAX_HEADER_BYTES)
        try:
            record = json.loads(header)
        except (ValueError, UnicodeError):
            return None
        if not isinstance(record, dict) or record.get("type") != "session_meta":
            return None
        payload = record.get("payload")
        if not isinstance(payload, dict):
            return None
        thread_id = payload.get("id")
        cwd = payload.get("cwd")
        if not isinstance(thread_id, str) or not THREAD_ID.fullmatch(thread_id):
            return None
        owner = self.owner(cwd) if isinstance(cwd, str) else None
        if owner is None:
            return None
        timestamp = payload.get("timestamp")
        return Rollout(
            thread_id, str(path), cwd, str(owner), int(path.stat().st_mtime),
            timestamp if isinstance(timestamp, str) else None, parent_thread_id(payload), archived,
        )

    def candidates(self):
        for store, _ in self.stores:
            for path in store.rglob("rollout-*.jsonl*"):
                try:
                    rollout = self.metadata(path)
                except FileNotFoundError:
                    continue  # Archive or cleanup can move a file while listing.
                if rollout is not None:
                    yield rollout

    def from_index(self, row, thread_id):
        path, cwd, archived = row
        if not isinstance(path, str) or not isinstance(cwd, str) or self.owner(cwd) is None:
            return None
        if archived and not any(archived_store for _, archived_store in self.stores):
            return None
        path = Path(path)
        if not path.exists() and path.name.endswith(".jsonl"):
            path = path.with_name(path.name + ".zst")
        rollout = self.metadata(path)
        if rollout is not None and rollout.thread_id == thread_id:
            return rollout
        return None

    def indexed_rollout(self, database, thread_id):
        if database is None:
            return None
        return database.execute(
            "SELECT rollout_path, cwd, archived FROM threads WHERE id = ?", (thread_id,),
        ).fetchone()

    def unindexed_rollout(self, options, thread_id):
        if len(options) != 1:
            return None
        rollout = next(iter(options.values()))
        if Path(rollout.path).name.removesuffix(".zst").endswith(f"-{thread_id}.jsonl"):
            return rollout
        return None

    def resolve(self, thread_id):
        if not THREAD_ID.fullmatch(thread_id or ""):
            raise Unavailable("유효한 Codex 스레드 ID 또는 현재 CODEX_THREAD_ID를 지정하세요.")
        with self.index() as database:
            row = self.indexed_rollout(database, thread_id)
        if row is not None:
            rollout = self.from_index(row, thread_id)
            if rollout is None:
                raise Unavailable("지정한 작업공간에서 인덱스가 가리키는 기록을 읽을 수 없습니다.")
            return rollout

        options = {item.path: item for item in self.candidates() if item.thread_id == thread_id}
        rollout = self.unindexed_rollout(options, thread_id)
        if rollout is None:
            raise Unavailable("작업공간에서 요청한 대화 기록을 하나로 확인할 수 없습니다.")
        return rollout

    def catalog(self):
        grouped = defaultdict(dict)
        for rollout in self.candidates():
            grouped[rollout.thread_id][rollout.path] = rollout
        resolved = []
        unresolved = []
        with self.index() as database:
            for thread_id, options in grouped.items():
                row = self.indexed_rollout(database, thread_id)
                if row is not None and row[2] and len(self.stores) == 1:
                    continue
                rollout = self.from_index(row, thread_id) if row is not None else self.unindexed_rollout(options, thread_id)
                if rollout is None:
                    unresolved.extend(options.values())
                    continue
                resolved.append(rollout)
        return sorted(resolved, key=lambda item: (-item.modified_at, item.thread_id)), unresolved

    def list(self):
        resolved, _ = self.catalog()
        return resolved

    def records(self, rollout):
        # Check the first record again on the same file handle before yielding any body.
        with open_rollout(Path(rollout.path)) as stream:
            try:
                header = json.loads(stream.readline(MAX_HEADER_BYTES))
                meta = header["payload"]
                owner = self.owner(meta["cwd"]) if isinstance(meta["cwd"], str) else None
                if header["type"] != "session_meta" or meta["id"] != rollout.thread_id or str(owner) != rollout.worktree:
                    raise ValueError
            except (KeyError, TypeError, ValueError, UnicodeError) as error:
                raise Unavailable("항목을 읽기 전에 대화 기록의 범위가 달라졌습니다.") from error
            yield header
            yield from rollout_records(stream)

    def activity(self):
        rollouts, unresolved = self.catalog()
        latest = {}
        roots = {str(root) for root in self.worktrees}
        for rollout in rollouts:
            latest.setdefault(rollout.worktree, rollout.modified_at)
            missing = roots - latest.keys()
            if not missing:
                break
            for record in self.records(rollout):
                for cwd in observed_directories(record, missing):
                    owner = self.owner(cwd)
                    if str(owner) in missing:
                        latest[str(owner)] = rollout.modified_at
                        missing.remove(str(owner))
                if not missing:
                    break
        if unresolved:
            uncertain_at = max(item.modified_at for item in unresolved)
            if any(latest.get(root, -1) < uncertain_at for root in roots):
                raise Unavailable("작업공간 대화 기록을 확인할 수 없어 최근 활동을 판단할 수 없습니다.")
        return latest


def command_directories(command, worktree_paths):
    if not isinstance(command, str):
        return
    try:
        tokens = shlex.split(command)
    except ValueError:
        tokens = ()
    for path in worktree_paths:
        if any(token == path or token.startswith(path + "/") for token in tokens):
            yield path
            continue
        literal = rf"""(?<![\w./~+-]){re.escape(path)}(?=$|[/\s"'\x60;|&<>()])"""
        if re.search(literal, command):
            yield path


def observed_directories(record, worktree_paths):
    payload = record.get("payload")
    if not isinstance(payload, dict):
        return
    if record.get("type") == "turn_context" and isinstance(payload.get("cwd"), str):
        yield payload["cwd"]
    if record.get("type") != "response_item":
        return
    if payload.get("type") == "function_call" and payload.get("name") in {"exec_command", "shell"}:
        try:
            arguments = json.loads(payload.get("arguments", ""))
        except (TypeError, ValueError):
            return
        if isinstance(arguments, dict):
            if isinstance(arguments.get("workdir"), str):
                yield arguments["workdir"]
            for key in ("cmd", "command"):
                yield from command_directories(arguments.get(key), tuple(worktree_paths))
    if payload.get("type") == "custom_tool_call" and payload.get("name") == "exec":
        source = payload.get("input")
        if isinstance(source, str):
            # The JavaScript tool wraps shell arguments in its input string.
            # Recognize a literal, never evaluate the script.
            pattern = r"""\bworkdir\s*:\s*(?:"([^"\\]*)"|'([^'\\]*)')"""
            for first, second in re.findall(pattern, source):
                yield first or second
            yield from command_directories(source, tuple(worktree_paths))


def messages(records):
    for record in records:
        payload = record.get("payload")
        if record.get("type") != "response_item" or not isinstance(payload, dict):
            continue
        if payload.get("type") != "message" or payload.get("role") not in {"user", "assistant"}:
            continue
        content = payload.get("content")
        if not isinstance(content, list):
            continue
        parts = [item["text"] for item in content if isinstance(item, dict)
                 and item.get("type") in {"input_text", "output_text"} and isinstance(item.get("text"), str)]
        if parts:
            yield {"role": payload["role"], "text": "\n".join(parts), "timestamp": record.get("timestamp")}


def search(history, query, limit, opening_only):
    needle = re.compile(re.escape(query), re.IGNORECASE)
    results = []
    for rollout in history.list():
        for message in messages(history.records(rollout)):
            if opening_only and message["role"] != "user":
                continue
            text = message["text"]
            found = needle.search(text)
            if found:
                start = max(found.start() - 100, 0)
                results.append({
                    "thread_id": rollout.thread_id, "worktree": rollout.worktree,
                    "role": message["role"], "timestamp": message["timestamp"],
                    "excerpt": text[start:min(found.end() + 140, start + 320)],
                })
                if len(results) >= limit:
                    return results
            if opening_only:
                break
    return results


def write_json(value):
    print(json.dumps(value, ensure_ascii=False))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", type=Path, default=Path.cwd())
    parser.add_argument("--codex-home", type=Path, default=Path(os.environ.get("CODEX_HOME", Path.home() / ".codex")))
    parser.add_argument("--include-archived", action="store_true")
    commands = parser.add_subparsers(dest="command", required=True)
    current = commands.add_parser("current", help="현재 스레드 메타데이터를 표시합니다.")
    current.add_argument("--thread", default=os.environ.get("CODEX_THREAD_ID"))
    commands.add_parser("list", help="작업공간에서 확인한 스레드 메타데이터를 나열합니다.")
    find = commands.add_parser("search", help="작업공간의 대화에서 일치하는 구절을 검색합니다.")
    find.add_argument("query")
    find.add_argument("--limit", type=int, default=12)
    find.add_argument("--opening-only", action="store_true")
    read = commands.add_parser("read", help="검증한 스레드 한 개를 JSONL로 출력합니다.")
    read.add_argument("thread")
    read.add_argument("--records", action="store_true", help="원본 도구 기록과 메타데이터를 포함합니다.")
    commands.add_parser("activity", help="worktree 경로별로 가장 최근 대화 기록 수정 시각을 출력합니다.")
    args = parser.parse_args(argv)
    if args.command == "search" and (not args.query or args.limit < 1):
        parser.error("검색어를 입력하고 결과 개수를 1 이상으로 지정하세요.")
    try:
        history = History(args.workspace, args.codex_home, args.include_archived)
        if args.command == "current":
            write_json(asdict(history.resolve(args.thread)))
        elif args.command == "list":
            write_json([asdict(rollout) for rollout in history.list()])
        elif args.command == "search":
            write_json(search(history, args.query, args.limit, args.opening_only))
        elif args.command == "read":
            records = history.records(history.resolve(args.thread))
            for record in records if args.records else messages(records):
                write_json(record)
        elif args.command == "activity":
            write_json(history.activity())
    except (Unavailable, OSError) as error:
        message = str(error) if isinstance(error, Unavailable) else "작업공간의 대화 기록을 읽을 수 없습니다."
        print(f"pstack 대화 기록 조회 실패. {message}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
