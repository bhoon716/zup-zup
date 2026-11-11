"""고정된 pstack 원본, Codex 스킬과 기존 하네스 보관본을 검증합니다."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys


BUNDLE = Path(__file__).resolve().parent
AGENTS = BUNDLE.parent
LOCK_PATH = BUNDLE / "upstream.lock.json"
WORKTREE_AUDIT = "pstack/skills/poteto-mode/scripts/worktree-audit.sh"
SKILL_ROOTS = ("skills", "cursor-team-kit/skills")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_bundle():
    manifest = json.loads((BUNDLE / "upstream.json").read_text())
    lock = json.loads(LOCK_PATH.read_text())
    require(all(manifest.get(key) == value for key, value in lock.items()), "설치본이 저장소의 고정 버전과 다릅니다.")
    for name, entry in manifest["files"].items():
        path = BUNDLE / name.removeprefix("pstack/")
        require(path.is_file() and not path.is_symlink(), f"원본 파일이 없습니다. {name}")
        if name == WORKTREE_AUDIT:
            patch = BUNDLE / "patches/worktree-audit.patch"
            data = subprocess.check_output(["patch", "-R", "-s", "-o", "-", str(path), str(patch)])
        else:
            data = path.read_bytes()
        digest = hashlib.sha1(f"blob {len(data)}\0".encode() + data).hexdigest()
        require(digest == entry["blob"], f"원본과 파일 내용이 다릅니다. {name}")
        executable = bool(path.stat().st_mode & 0o111)
        require(executable == (entry["mode"] == "100755"), f"원본과 실행 권한이 다릅니다. {name}")

    catalog = AGENTS / "skills"
    counts = []
    for relative in SKILL_ROOTS:
        skill_root = BUNDLE / relative
        entries = sorted(skill_root.glob("*/SKILL.md"))
        counts.append(len(entries))
        for entry in entries:
            link = catalog / entry.parent.name
            require(link.is_symlink() and link.resolve() == entry.parent, f"스킬 링크가 올바르지 않습니다. {link}")
    require(counts == [58, 3], f"스킬 개수가 예상과 다릅니다. {counts}")
    benny = list((BUNDLE / "automations/benny/skills").glob("*/SKILL.md"))
    require(len(benny) == 3, "Benny 운영 파일이 누락됐습니다.")
    for entry in benny:
        require(not (catalog / entry.parent.name).exists(), "Benny 운영 파일은 직접 읽어야 합니다.")
    require(len(list((BUNDLE / "skills").glob("principle-*/SKILL.md"))) == 24, "원칙 스킬이 누락됐습니다.")
    playbooks = BUNDLE / "skills/poteto-mode/playbooks"
    require(len(list(playbooks.glob("*.md"))) == 23, "플레이북이 누락됐습니다.")
    print(f"{manifest['version']} 원본 Git 객체 {len(manifest['files'])}개, 스킬 링크 61개, "
          "원칙 24개, 플레이북 23개, Benny 파일 3개")


def archive_entry(path):
    if path.is_symlink():
        return ["symlink", os.readlink(path)]
    mode = path.stat().st_mode & 0o777
    if path.is_dir():
        return ["dir", mode]
    return ["file", mode, hashlib.sha256(path.read_bytes()).hexdigest()]


def verify_archive():
    archive = AGENTS / "archeive"
    manifest = json.loads((archive / ".manifest.json").read_text())
    actual = {str(path.relative_to(archive)) for path in archive.rglob("*")}
    require(actual - {".manifest.json"} == manifest.keys(), "기존 하네스 보관 경로가 달라졌습니다.")
    for relative, expected in manifest.items():
        require(archive_entry(archive / relative) == expected, f"보관본이 달라졌습니다. {relative}")
    print(f"보관 항목 {len(manifest)}개를 검증했습니다.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", action="store_true", help="로컬에 보관한 기존 하네스도 검증합니다.")
    args = parser.parse_args()
    try:
        verify_bundle()
        if args.archive:
            verify_archive()
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"pstack 검증 실패. {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
