#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin"
cat > "$tmp/bin/docker" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$DOCKER_CALLS"
if [ "${FAIL_EXPORT:-0}" = 1 ]; then exit 1; fi
printf "INSERT INTO admin_action_logs_archive VALUES (1,2,'STATUS_CHANGE','FEEDBACK',3,'2026-01-01 00:00:00','2026-01-01 00:00:00');\n"
STUB
chmod +x "$tmp/bin/docker"
export DOCKER_CALLS="$tmp/calls"
export PATH="$tmp/bin:$PATH"
export RELEASE_ROOT="$tmp/release"
export DB_ROOT_PASSWORD='fixture-only'
bash "$root/infra/scripts/backup-admin-audit.sh" > "$tmp/output.json"
python3 - "$tmp/output.json" <<'PYJSON'
import json, sys
event = json.load(open(sys.argv[1]))
assert event["event"] == "admin_audit_archive_completed"
assert event["rows"] == 1
PYJSON
archive="$(find "$RELEASE_ROOT/backups/audit" -name '*.sql.gz' | head -1)"
test -n "$archive"
gzip -t "$archive"
grep -q '^rows=1$' "${archive}.manifest"
grep -q '^schema=admin-audit-core-v1$' "${archive}.manifest"
(cd "$(dirname "$archive")" && shasum -a 256 -c "$(basename "$archive").sha256")
# Core metadata only: user content/legacy arbitrary metadata must never be selected.
! grep -q 'meta_data' "$DOCKER_CALLS"
gzip -dc "$archive" | grep -q 'CREATE TABLE admin_action_logs_archive'
if FAIL_EXPORT=1 bash "$root/infra/scripts/backup-admin-audit.sh" >/dev/null 2>&1; then
  echo 'failed export must not succeed' >&2; exit 1
fi
test "$(find "$RELEASE_ROOT/backups/audit" -name '*.sql.gz' | wc -l | tr -d ' ')" = 1
unset DB_ROOT_PASSWORD
if bash "$root/infra/scripts/backup-admin-audit.sh" >/dev/null 2>&1; then
  echo 'missing credential must fail closed' >&2; exit 1
fi
echo 'Admin audit archive contract passed'
