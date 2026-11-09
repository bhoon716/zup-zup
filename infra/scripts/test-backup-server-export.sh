#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin" "$tmp/uploads"
printf 'synthetic attachment\n' > "$tmp/uploads/fixture.txt"
cat > "$tmp/bin/docker" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$DOCKER_CALLS"
if [ "$1" = ps ]; then echo "${DB_CONTAINER_FIXTURE:-sugang-helper-mysql}"; exit; fi
if [[ "$*" == *'sugang-helper-app tar -czf - -C /app/data/uploads .' ]]; then
  [ "${UPLOADS_FAIL:-0}" = 0 ] || exit 1
  tar -czf - -C "$SYNTHETIC_UPLOADS" .
  exit
fi
if [[ "$*" == *mysqldump* ]]; then
  [ "${DUMP_FAIL:-0}" = 0 ] || exit 1
  echo 'CREATE TABLE synthetic_fixture (id INT);'; exit
fi
if [[ "$*" == *admin_action_logs* ]]; then
  [ "${AUDIT_FAIL:-0}" = 0 ] || exit 1
  echo "INSERT INTO admin_action_logs_archive VALUES (1,2,'STATUS_CHANGE','FEEDBACK',3,'2026-01-01 00:00:00','2026-01-01 00:00:00');"
  exit
fi
exit 1
STUB
chmod +x "$tmp/bin/docker"
export PATH="$tmp/bin:$PATH" DOCKER_CALLS="$tmp/calls" DB_ROOT_PASSWORD=fixture-only-password
export UPLOADS_VOL_PATH="" SYNTHETIC_UPLOADS="$tmp/uploads"
run_export() { bash "$root/infra/scripts/backup-server-export.sh" > "$tmp/out" 2>&1; }
assert_no_bundle() {
  test "$(find "$RELEASE_ROOT/backups/export" -type f 2>/dev/null | wc -l | tr -d ' ')" = 0
  test "$(find "$RELEASE_ROOT/backups/export" -name '.pending.*' | wc -l | tr -d ' ')" = 0
}
# Given/when/then: failed export must not expose a partial final file.
export RELEASE_ROOT="$tmp/failure"
if DUMP_FAIL=1 run_export; then echo 'dump failure must fail exporter' >&2; exit 1; fi
assert_no_bundle
# A complete bundle is published once all required files and checksums are available.
export RELEASE_ROOT="$tmp/success"
run_export
python3 - "$tmp/out" <<'PYLOG'
import json, sys
rows = [json.loads(line) for line in open(sys.argv[1])]
assert rows[-1]['event'] == 'backup_export'
assert rows[-1]['status'] == 'success'
PYLOG
manifest="$(find "$RELEASE_ROOT/backups/export" -name backup.manifest)"
test -n "$manifest"
grep -qx 'schema=full-backup-v1' "$manifest"
grep -qx 'uploads=included' "$manifest"
bundle="$(dirname "$manifest")"
# The exporter follows the runner's effective identity; private outputs stay usable by that owner.
python3 - "$RELEASE_ROOT/backups/export" "$RELEASE_ROOT/backups/audit" <<'PYOWNER'
import os, pathlib, stat, sys
for directory in sys.argv[1:]:
    for path in (pathlib.Path(directory), *pathlib.Path(directory).rglob('*')):
        metadata = path.stat()
        assert metadata.st_uid == os.geteuid(), path
        if path.is_dir():
            assert stat.S_IMODE(metadata.st_mode) == 0o700, path
            continue
        assert stat.S_IMODE(metadata.st_mode) == 0o600, path
PYOWNER
(cd "$bundle" && for checksum in *.sha256; do shasum -a 256 -c "$checksum"; done)
gzip -t "$bundle/"*.sql.gz
tar -tzf "$bundle/"*.tar.gz | grep -q fixture.txt
grep 'mysqldump' "$DOCKER_CALLS" | grep -q -- '-e MYSQL_PWD='
! grep 'mysqldump' "$DOCKER_CALLS" | grep -q -- '-pfixture'
! grep -q fixture-only-password "$tmp/out"
# Default reads only the app mount via Docker, not root-only host Docker storage.
grep -q 'exec sugang-helper-app tar -czf - -C /app/data/uploads .' "$DOCKER_CALLS"
export RELEASE_ROOT="$tmp/app-uploads-failure"
if UPLOADS_FAIL=1 run_export; then echo 'unreadable or stopped app must fail full backup' >&2; exit 1; fi
assert_no_bundle
# Explicit host source remains available to operators with a readable authorized path.
export RELEASE_ROOT="$tmp/explicit-host-source"
UPLOADS_VOL_PATH="$tmp/uploads" run_export
test -n "$(find "$RELEASE_ROOT/backups/export" -name backup.manifest)"
# Missing required uploads and audit failures must not publish a DB-only success.
export RELEASE_ROOT="$tmp/no-uploads"
if UPLOADS_VOL_PATH="$tmp/absent" run_export; then echo 'missing uploads must fail' >&2; exit 1; fi
assert_no_bundle
export RELEASE_ROOT="$tmp/audit-failure"
if AUDIT_FAIL=1 run_export; then echo 'audit failure must fail' >&2; exit 1; fi
assert_no_bundle
# An explicit DB-only request is recorded; default full mode never silently degrades.
export RELEASE_ROOT="$tmp/db-only"
BACKUP_INCLUDE_UPLOADS=0 UPLOADS_VOL_PATH="$tmp/absent" run_export
manifest="$(find "$RELEASE_ROOT/backups/export" -name backup.manifest)"
grep -qx 'uploads=excluded' "$manifest"
! grep -q '^uploads_file=' "$manifest"
test "$(find "$RELEASE_ROOT/backups/export" -name '*.tar.gz' | wc -l | tr -d ' ')" = 0
export RELEASE_ROOT="$tmp/wrong-db"
if DB_CONTAINER_FIXTURE=sugang-helper-mysql-other run_export; then echo 'container name must be exact' >&2; exit 1; fi
assert_no_bundle
if BACKUP_INCLUDE_UPLOADS=unknown run_export; then echo 'invalid backup mode must fail' >&2; exit 1; fi
if DB_ROOT_PASSWORD='' run_export; then echo 'missing credentials must fail' >&2; exit 1; fi
echo 'Atomic complete backup bundle, uploads requirement, credential and failure contracts passed'
