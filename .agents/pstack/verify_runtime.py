#!/usr/bin/env python3
"""Check what a new Codex process discovers without starting a model turn."""

import argparse
import json
import subprocess
import sys
from pathlib import Path
from queue import Empty, Queue
from threading import Thread


ROOT = Path(__file__).resolve().parents[2]
WORKDIRS = [ROOT, ROOT / "apps/server"]
ROLES = ("poteto-agent", "comment-sicko")
SESSION_START_MATCHER = "startup|resume|clear|compact|fork"


def app_server_snapshot(executable):
    messages = Queue()
    commands = [
        ("skills/list", {"cwds": list(map(str, WORKDIRS)), "forceReload": True}),
        ("hooks/list", {"cwds": list(map(str, WORKDIRS))}),
        ("config/read", {"cwd": str(ROOT), "includeLayers": True}),
        ("model/list", {"limit": 100, "includeHidden": False}),
    ]

    with subprocess.Popen(
        [executable, "app-server", "--stdio"],
        cwd=ROOT,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        text=True,
    ) as server:
        def read_replies():
            for line in server.stdout:
                messages.put(line)
            messages.put(None)

        def send(message):
            server.stdin.write(json.dumps(message) + "\n")
            server.stdin.flush()

        def request(message_id, method, params):
            send({"id": message_id, "method": method, "params": params})
            while True:
                try:
                    line = messages.get(timeout=20)
                except Empty as error:
                    raise RuntimeError(f"Codex가 {method} 요청에 응답하지 않았습니다.") from error
                if line is None:
                    raise RuntimeError(f"Codex가 {method} 요청을 처리하는 도중 종료됐습니다.")
                reply = json.loads(line)
                if reply.get("id") != message_id:
                    continue
                if "error" in reply:
                    raise RuntimeError(f"{method}: {reply['error']['message']}")
                return reply["result"]

        reader = Thread(target=read_replies, daemon=True)
        reader.start()
        try:
            request(0, "initialize", {
                "clientInfo": {"name": "pstack-runtime-verifier", "version": "1"},
                "capabilities": {"experimentalApi": True},
            })
            send({"method": "initialized"})
            return [request(index, method, params) for index, (method, params) in enumerate(commands, 1)]
        finally:
            server.stdin.close()
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()
            reader.join(timeout=5)


def verify_skills(snapshot):
    pstack = ROOT / ".agents/pstack"
    core = sorted((pstack / "skills").glob("*/SKILL.md"))
    team_kit = sorted((pstack / "cursor-team-kit/skills").glob("*/SKILL.md"))
    if (len(core), len(team_kit)) != (58, 3):
        raise RuntimeError("pstack 스킬 58개와 Cursor Team Kit 스킬 3개가 필요합니다.")
    expected = {skill.parent.name: skill.resolve() for skill in core + team_kit}
    for name, skill in expected.items():
        link = ROOT / ".agents/skills" / name
        if not link.is_symlink() or (link / "SKILL.md").resolve() != skill:
            raise RuntimeError(f"스킬 링크가 없거나 올바르지 않습니다. {link}")

    archive = ROOT / ".agents/archeive"
    archived_names = {skill.parent.name for skill in (archive / "skills").glob("*/SKILL.md")}
    entries = {Path(entry["cwd"]): entry for entry in snapshot["data"]}
    for cwd in WORKDIRS:
        entry = entries[cwd]
        if entry["errors"]:
            raise RuntimeError(f"{cwd}: 스킬 불러오기 실패. {entry['errors']}")
        loaded = {}
        for skill in entry["skills"]:
            if skill["scope"] != "repo" or not skill["enabled"]:
                continue
            name = skill["name"].removeprefix("pstack:")
            path = Path(skill["path"]).resolve()
            if path.is_relative_to(archive) or (name in archived_names and path != expected.get(name)):
                raise RuntimeError(f"{cwd}: 보관된 스킬이 활성화됐습니다. {path}")
            loaded.setdefault(name, set()).add(path)
        mismatches = sorted(name for name, path in expected.items() if loaded.get(name) != {path})
        if mismatches:
            raise RuntimeError(f"{cwd}: 누락됐거나 다른 스킬에 가려진 pstack 스킬이 있습니다. {mismatches}")
        print(f"{cwd.relative_to(ROOT)}: 고정 스킬 {len(expected)}개를 불러왔습니다.")


def verify_hooks(snapshot):
    hooks_path = ROOT / ".codex/hooks.json"
    entries = {Path(entry["cwd"]): entry for entry in snapshot["data"]}
    for cwd in WORKDIRS:
        entry = entries[cwd]
        if entry["errors"]:
            raise RuntimeError(f"{cwd}: 훅 불러오기 실패. {entry['errors']}")
        matches = [
            hook for hook in entry["hooks"]
            if Path(hook["sourcePath"]).resolve() == hooks_path
            and hook["eventName"] == "sessionStart"
            and hook["matcher"] == SESSION_START_MATCHER
        ]
        if len(matches) != 1 or not matches[0]["enabled"]:
            raise RuntimeError(f"{cwd}: 활성화된 pstack SessionStart 훅이 한 개여야 합니다.")
        print(f"{cwd.relative_to(ROOT)}: SessionStart 훅을 찾았습니다. 신뢰 상태 {matches[0]['trustStatus']}")


def verify_roles(snapshot):
    agents = snapshot["config"].get("agents") or {}
    for role in ROLES:
        config_file = (agents.get(role) or {}).get("config_file")
        expected = ROOT / ".codex/agents" / f"{role}.toml"
        if config_file is None or (ROOT / ".codex" / config_file).resolve() != expected:
            raise RuntimeError(f"{role} 에이전트 설정이 {expected} 파일을 가리켜야 합니다.")
        if not expected.is_file():
            raise RuntimeError(f"에이전트 설정 파일이 없습니다. {expected}")
    print(f"프로젝트에 설정된 에이전트 {', '.join(ROLES)}")


def verify_project_instructions(executable):
    for cwd in WORKDIRS:
        result = subprocess.run(
            [executable, "debug", "prompt-input", "--disable", "hooks", "pstack runtime verification"],
            cwd=cwd,
            capture_output=True,
            text=True,
            check=True,
            timeout=30,
        )
        items = json.loads(result.stdout)
        instructions = [part.get("text", "") for item in items for part in item.get("content", [])]
        marker = f"# AGENTS.md instructions for {ROOT}"
        if not any(marker in part and "pstack" in part.lower() for part in instructions):
            raise RuntimeError(f"{cwd}: Codex 입력에서 프로젝트 pstack 지침을 찾을 수 없습니다.")
        print(f"{cwd.relative_to(ROOT)}: AGENTS.md의 pstack 지침이 모델 입력에 포함됐습니다.")


def verify_models(snapshot):
    supported = {
        entry["model"]: {effort["reasoningEffort"] for effort in entry["supportedReasoningEfforts"]}
        for entry in snapshot["data"]
    }
    configured = set()
    sheet = ROOT / ".codex/pstack-models.md"
    for line in sheet.read_text().splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        role, values = line.split(":", 1)
        for value in values.split(","):
            selection = value.strip()
            if selection in {"auto", "inherit-parent"}:
                continue
            model, _, effort = selection.partition(" @")
            if effort not in supported.get(model, ()):
                raise RuntimeError(f"{role} 역할의 모델 또는 추론 수준을 현재 CLI에서 쓸 수 없습니다. {selection}")
            configured.add(selection)
    print(f"현재 Codex 모델 목록에서 확인한 역할 설정 {', '.join(sorted(configured))}")


def main():
    parser = argparse.ArgumentParser(description="새 Codex 프로세스가 불러오는 pstack 구성을 검증합니다.")
    parser.add_argument("--codex", default="codex", help="검증할 Codex 실행 파일을 지정합니다.")
    args = parser.parse_args()
    skills, hooks, config, models = app_server_snapshot(args.codex)
    verify_skills(skills)
    verify_hooks(hooks)
    verify_roles(config)
    verify_models(models)
    verify_project_instructions(args.codex)


if __name__ == "__main__":
    try:
        main()
    except (KeyError, OSError, RuntimeError, ValueError, subprocess.SubprocessError) as error:
        sys.exit(f"pstack 런타임 검증 실패. {error}")
