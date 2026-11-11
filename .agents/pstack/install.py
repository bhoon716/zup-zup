"""고정된 pstack 원본을 내려받아 현재 저장소에서 Codex 스킬을 연결합니다."""

import io
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile

from verify import AGENTS, BUNDLE, LOCK_PATH, SKILL_ROOTS, verify_bundle


TEAM_KIT_FILES = (
    "cursor-team-kit/LICENSE",
    "cursor-team-kit/skills/control-cli",
    "cursor-team-kit/skills/control-ui",
    "cursor-team-kit/skills/deslop",
)


def extract_upstream(workspace, lock):
    checkout = workspace / "upstream.git"
    subprocess.run(["git", "init", "--bare", "-q", str(checkout)], check=True)
    git = ["git", "-C", str(checkout)]
    revision = lock["revision"]
    subprocess.run([*git, "fetch", "--quiet", "--depth=1", lock["repository"], revision], check=True)
    paths = ["pstack", *TEAM_KIT_FILES]
    content = subprocess.check_output([*git, "archive", revision, *paths])
    with tarfile.open(fileobj=io.BytesIO(content)) as archive:
        archive.extractall(workspace, filter="data")
    tree = subprocess.check_output([*git, "ls-tree", "-rz", revision, "--", *paths])
    files = {}
    for row in tree.decode().rstrip("\0").split("\0"):
        metadata, path = row.split("\t", 1)
        mode, kind, oid = metadata.split()
        if kind != "blob" or mode not in {"100644", "100755"}:
            raise ValueError(f"지원하지 않는 pstack 원본 파일입니다. {path}")
        files[path] = {"mode": mode, "blob": oid}

    source = workspace / "pstack"
    (workspace / "cursor-team-kit").rename(source / "cursor-team-kit")
    (source / "upstream.json").write_text(json.dumps({**lock, "files": files}, indent=2) + "\n")
    patch = BUNDLE / "patches/worktree-audit.patch"
    subprocess.run(["patch", "-s", "-f", "-p1", "-i", str(patch)], cwd=source, check=True)
    return source


def publish(source):
    catalog = AGENTS / "skills"
    links = {}
    for relative in SKILL_ROOTS:
        for skill in (source / relative).glob("*/SKILL.md"):
            links[catalog / skill.parent.name] = Path("../pstack") / relative / skill.parent.name

    committed_pack = BUNDLE / "automations"
    if not committed_pack.is_dir():
        raise ValueError("저장소에서 Benny 운영 파일을 찾을 수 없습니다.")
    entries = sorted(entry for entry in source.iterdir() if entry.name != "automations")
    conflicts = [path for path in [*(BUNDLE / entry.name for entry in entries), *links] if path.exists() or path.is_symlink()]
    if conflicts:
        raise ValueError(f"기존 pstack 파일 또는 스킬과 설치 경로가 겹칩니다. {conflicts[0]}")

    catalog.mkdir(exist_ok=True)
    created = []
    try:
        for entry in entries:
            target = BUNDLE / entry.name
            shutil.move(str(entry), str(target))
            created.append(target)
        for link, target in links.items():
            link.symlink_to(target, target_is_directory=True)
            created.append(link)
        verify_bundle()
    except BaseException:
        for path in reversed(created):
            if path.is_dir() and not path.is_symlink():
                shutil.rmtree(path)
            else:
                path.unlink()
        raise


def main():
    if (BUNDLE / "upstream.json").exists():
        verify_bundle()
        return
    lock = json.loads(LOCK_PATH.read_text())
    with tempfile.TemporaryDirectory(prefix="pstack-install-") as temporary:
        publish(extract_upstream(Path(temporary), lock))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, subprocess.CalledProcessError, tarfile.TarError) as error:
        sys.exit(f"pstack 설치 실패. {error}")
