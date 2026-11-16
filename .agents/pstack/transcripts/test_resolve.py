from contextlib import closing
import json
import os
from pathlib import Path
import shutil
import sqlite3
import subprocess
import tempfile
import unittest

from resolve import History, Unavailable, messages, search


THREAD = "01234567-89ab-4cde-8f01-0123456789ab"
OTHER_THREAD = "01234567-89ab-4cde-8f01-0123456789ac"
ARCHIVED_THREAD = "01234567-89ab-4cde-8f01-0123456789ad"


def chat(role, text):
    return {
        "type": "response_item",
        "payload": {"type": "message", "role": role, "content": [{"type": "input_text", "text": text}]},
    }


class TranscriptTest(unittest.TestCase):
    def setUp(self):
        sandbox = tempfile.TemporaryDirectory()
        self.addCleanup(sandbox.cleanup)
        self.root = Path(sandbox.name).resolve()
        self.repo = self.root / "project"
        self.repo.mkdir()
        self.git("init", "-q")
        self.git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid", "commit", "-qm", "base", "--allow-empty")
        self.home = self.root / "codex"
        (self.home / "sessions").mkdir(parents=True)
        (self.home / "archived_sessions").mkdir()

    def git(self, *args):
        subprocess.run(["git", "-C", str(self.repo), *args], capture_output=True, check=True)

    def write(self, thread, cwd=None, *, records=(), archived=False, replacement=False):
        folder = self.home / ("archived_sessions" if archived else "sessions")
        if not archived:
            folder /= "2026/11/11"
            folder.mkdir(parents=True, exist_ok=True)
        suffix = f"{thread}_new" if replacement else thread
        path = folder / f"rollout-2026-11-11T00-00-00-{suffix}.jsonl"
        header = {"type": "session_meta", "payload": {"id": thread, "cwd": str(cwd or self.repo)}}
        path.write_text("".join(json.dumps(record) + "\n" for record in (header, *records)))
        return path

    def index(self, thread, path, cwd=None, archived=False):
        with closing(sqlite3.connect(self.home / "state_5.sqlite")) as db, db:
            db.execute("CREATE TABLE IF NOT EXISTS threads (id TEXT PRIMARY KEY, rollout_path TEXT, cwd TEXT, archived INTEGER)")
            db.execute("INSERT INTO threads VALUES (?, ?, ?, ?)", (thread, str(path), str(cwd or self.repo), archived))

    def history(self, include_archived=False):
        return History(self.repo, self.home, include_archived)

    def test_exact_index_revision_wins(self):
        self.write(THREAD, records=[chat("user", "reverted prompt")])
        replacement = self.write(THREAD, records=[chat("user", "current prompt")], replacement=True)
        self.index(THREAD, replacement)
        history = self.history()
        current = history.resolve(THREAD)
        self.assertEqual(current.path, str(replacement))
        self.assertEqual(history.list(), [current])
        self.assertEqual(search(history, "reverted", 5, False), [])
        self.assertEqual(search(history, "current", 5, True)[0]["excerpt"], "current prompt")

    def test_no_index_rejects_ambiguity_and_replacement_only(self):
        normal = self.write(THREAD)
        self.assertEqual(self.history().resolve(THREAD).path, str(normal))
        self.write(THREAD, replacement=True)
        with self.assertRaises(Unavailable):
            self.history().resolve(THREAD)
        self.assertEqual(self.history().list(), [])
        with self.assertRaises(Unavailable):
            self.history().activity()
        normal.unlink()
        with self.assertRaises(Unavailable):
            self.history().resolve(THREAD)

    def test_missing_index_path_does_not_resurrect_stale_revision(self):
        normal = self.write(THREAD)
        self.index(THREAD, normal.with_name("rollout-missing.jsonl"))
        with self.assertRaises(Unavailable):
            self.history().resolve(THREAD)
        self.assertEqual(self.history().list(), [])

    def test_archived_history_is_opt_in(self):
        archived = self.write(ARCHIVED_THREAD, archived=True, records=[chat("user", "archived text")])
        self.index(ARCHIVED_THREAD, archived, archived=True)
        with self.assertRaises(Unavailable):
            self.history().resolve(ARCHIVED_THREAD)
        self.assertEqual(search(self.history(), "archived", 5, False), [])
        self.assertEqual(self.history(True).resolve(ARCHIVED_THREAD).path, str(archived))
        self.assertEqual(search(self.history(True), "archived", 5, False)[0]["thread_id"], ARCHIVED_THREAD)

    def test_header_scope_excludes_sibling_and_nested_git_projects(self):
        sibling = self.root / "project-other"
        sibling.mkdir()
        outside = self.write(THREAD, sibling, records=[chat("user", "foreign private text")])
        self.index(THREAD, outside)
        nested = self.repo / "nested"
        nested.mkdir()
        subprocess.run(["git", "-C", str(nested), "init", "-q"], check=True, capture_output=True)
        self.write(OTHER_THREAD, nested, records=[chat("user", "foreign nested text")])
        with self.assertRaises(Unavailable):
            self.history().resolve(THREAD)
        self.assertEqual(search(self.history(), "foreign", 5, False), [])

    def test_index_metadata_and_storage_are_both_required(self):
        outside_store = self.root / "rollout-outside.jsonl"
        local = self.write(THREAD)
        local.rename(outside_store)
        local.symlink_to(outside_store)
        self.index(THREAD, local)
        with self.assertRaises(Unavailable):
            self.history().resolve(THREAD)

    def test_records_recheck_header_and_ignore_only_partial_last_record(self):
        path = self.write(THREAD, records=[chat("developer", "internal"), chat("assistant", "visible")])
        history = self.history()
        current = history.resolve(THREAD)
        with path.open("a") as stream:
            stream.write('{"type":')
        self.assertEqual([item["text"] for item in messages(history.records(current))], ["visible"])
        path.unlink()
        self.write(THREAD, self.root / "other")
        with self.assertRaises(Unavailable):
            list(history.records(current))

    def test_activity_binds_current_and_command_worktree(self):
        worktree = self.root / "another worktree"
        self.git("worktree", "add", "--detach", str(worktree), "HEAD")
        cmd = {
            "type": "response_item",
            "payload": {"type": "custom_tool_call", "name": "exec",
                        "input": f'await tools.exec_command({{cmd:"pwd",workdir:"{worktree}"}})'},
        }
        root_session = self.write(THREAD, records=[cmd])
        older_session = self.write(OTHER_THREAD, worktree)
        os.utime(older_session, (10, 10))
        os.utime(root_session, (20, 20))
        self.assertEqual(self.history().activity(), {str(self.repo): 20, str(worktree): 20})

    def test_older_ambiguous_rollout_does_not_hide_newer_known_activity(self):
        old = self.write(THREAD, replacement=True)
        new = self.write(OTHER_THREAD)
        os.utime(old, (10, 10))
        os.utime(new, (20, 20))
        self.assertEqual(self.history().activity(), {str(self.repo): 20})

    @unittest.skipUnless(shutil.which("zstd"), "zstd is not installed")
    def test_compressed_indexed_rollout(self):
        path = self.write(THREAD, records=[chat("user", "compressed data")])
        self.index(THREAD, path)
        subprocess.run(["zstd", "-q", "--rm", str(path)], check=True, capture_output=True)
        current = self.history().resolve(THREAD)
        self.assertTrue(current.path.endswith(".zst"))
        self.assertEqual(list(messages(self.history().records(current)))[0]["text"], "compressed data")


if __name__ == "__main__":
    unittest.main()
