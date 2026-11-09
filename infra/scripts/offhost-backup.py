#!/usr/bin/env python3
"""운영 백업을 맥북에 저장하고 서버에 스냅샷 시각을 기록한다."""
from datetime import datetime, timezone
import fcntl
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile

REMOTE_EXPORT = "/home/ubuntu/jbnu-sugang-helper/backups/export"
REMOTE_HELPER = "/usr/local/libexec/sugang-helper-offhost-backup.py"
MAC_BACKUP_DIR = Path.home() / "Backups/jbnu-sugang-helper/full"
RECEIPT_PATH = Path("/var/lib/sugang-helper-host-jobs/mac-backup.prom")
BUNDLE_NAME = re.compile(r"full_backup_(\d{8}T\d{6}Z)_[A-Za-z0-9]{6}")
SSH_OPTIONS = ("-oBatchMode=yes", "-oStrictHostKeyChecking=yes", "-oConnectTimeout=8",
               "-oServerAliveInterval=15", "-oServerAliveCountMax=2")


def checksum_path(path):
    return path.with_name(path.name + ".sha256")


def digest(path):
    with path.open("rb") as handle:
        result = hashlib.sha256()
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            result.update(block)
        return result.hexdigest()


def verify_checksum(path):
    checksum = checksum_path(path)
    if path.is_symlink() or checksum.is_symlink() or not path.is_file() or not checksum.is_file():
        raise ValueError("백업 파일 또는 체크섬이 없습니다.")
    fields = checksum.read_text().split()
    if len(fields) != 2 or fields[1] != path.name or fields[0] != digest(path):
        raise ValueError("백업 파일의 체크섬이 일치하지 않습니다.")


def verify_bundle(path, stamp):
    dump = path / f"sugang_helper_{stamp}.sql.gz"
    uploads = path / f"uploads_{stamp}.tar.gz"
    manifest = path / "backup.manifest"
    payloads = (dump, uploads, manifest)
    expected = set(payloads) | {checksum_path(file) for file in payloads}
    if path.is_symlink() or set(path.iterdir()) != expected:
        raise ValueError("전체 백업에 필요한 파일 구성이 일치하지 않습니다.")
    for file in payloads:
        verify_checksum(file)
    metadata = dict(line.split("=", 1) for line in manifest.read_text().splitlines())
    if metadata != {"schema": "full-backup-v1", "database": "sugang_helper", "created_at_utc": stamp,
                    "dump_file": dump.name, "uploads": "included", "uploads_file": uploads.name}:
        raise ValueError("전체 백업의 메타데이터가 일치하지 않습니다.")


def ssh_options():
    identity = os.environ.get("OFFHOST_BACKUP_SSH_IDENTITY")
    if identity:
        return (*SSH_OPTIONS, "-i", identity, "-oIdentitiesOnly=yes")
    return SSH_OPTIONS


def ssh(*command, stdout=None):
    return subprocess.run(("/usr/bin/ssh", *ssh_options(), "zupzup", *command), check=True,
                          text=True, stdout=stdout, timeout=30)


def latest_bundle():
    names = ssh("/bin/ls", "-1", REMOTE_EXPORT, stdout=subprocess.PIPE).stdout.splitlines()
    latest = max((name for name in names if BUNDLE_NAME.fullmatch(name)), default="")
    if not latest:
        raise ValueError("서버에 완성된 전체 백업이 없습니다.")
    return latest


def download_bundle(bundle, stage):
    subprocess.run(("/usr/bin/scp", "-q", "-p", "-r", *ssh_options(),
                    f"zupzup:{REMOTE_EXPORT}/{bundle}", str(stage)), check=True, timeout=600)


def acknowledge(stamp):
    ssh("sudo", "-n", "/usr/bin/python3", REMOTE_HELPER, "receipt", stamp)


def save_archive(destination, archive, bundle, stamp):
    with tempfile.TemporaryDirectory(prefix=".incoming.", dir=destination) as temporary:
        stage = Path(temporary)
        download_bundle(bundle, stage)
        verify_bundle(stage / bundle, stamp)
        packaged = stage / archive.name
        with tarfile.open(packaged, "w:gz", compresslevel=1) as contents:
            contents.add(stage / bundle, arcname=bundle)
        checksum = checksum_path(packaged)
        checksum.write_text(f"{digest(packaged)}  {archive.name}\n")
        packaged.replace(archive)
        checksum.replace(checksum_path(archive))
    print(f"백업 압축본 저장 완료: {archive.name}")


def sync(destination):
    destination.mkdir(parents=True, exist_ok=True, mode=0o700)
    with (destination / ".sync.lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        bundle = latest_bundle()
        stamp = BUNDLE_NAME.fullmatch(bundle).group(1)
        archive = destination / f"{bundle}.tar.gz"
        try:
            verify_checksum(archive)
        except (OSError, ValueError):
            save_archive(destination, archive, bundle, stamp)
            verify_checksum(archive)
        acknowledge(stamp)


def record_receipt(stamp):
    snapshot = datetime.strptime(stamp, "%Y%m%dT%H%M%SZ").replace(tzinfo=timezone.utc)
    if snapshot > datetime.now(timezone.utc):
        raise ValueError("미래 시점의 백업은 도착 확인할 수 없습니다.")
    contents = f"sugang_mac_backup_snapshot_timestamp_seconds {int(snapshot.timestamp())}\n"
    if RECEIPT_PATH.is_file() and RECEIPT_PATH.read_text() == contents:
        return
    with tempfile.NamedTemporaryFile(mode="w", dir=RECEIPT_PATH.parent, delete=False) as temporary:
        temporary.write(contents)
        os.fchmod(temporary.fileno(), 0o644)
        path = Path(temporary.name)
    try:
        path.replace(RECEIPT_PATH)
    finally:
        path.unlink(missing_ok=True)


def prune_exports(root):
    verified = []
    for path in sorted(root.iterdir(), reverse=True):
        match = BUNDLE_NAME.fullmatch(path.name)
        if not match or not path.is_dir() or path.is_symlink():
            continue
        try:
            verify_bundle(path, match.group(1))
        except (OSError, ValueError):
            continue
        verified.append(path)
    for path in verified[3:]:
        shutil.rmtree(path)


def main(args):
    if args == ["sync"]:
        sync(MAC_BACKUP_DIR)
        return
    if len(args) == 2 and args[0] == "receipt":
        record_receipt(args[1])
        return
    if args == ["prune"]:
        prune_exports(Path(REMOTE_EXPORT))
        return
    raise ValueError("지원하지 않는 백업 명령입니다.")


if __name__ == "__main__":
    os.umask(0o077)
    try:
        main(sys.argv[1:])
    except (OSError, ValueError, subprocess.SubprocessError, tarfile.TarError) as error:
        sys.exit(f"백업 작업 실패: {error}")
