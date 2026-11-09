#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin"
# Given: synthetic files only. Every Docker/sleep command is intercepted.
cat > "$tmp/bin/docker" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$DOCKER_CALLS"
if [ "$1" = context ]; then echo "${DOCKER_ENDPOINT_FIXTURE:-unix:///fixture-only/docker.sock}"; exit; fi
if [ "$1" = create ]; then
  [ "${CREATE_FAIL:-0}" = 0 ] || exit 1
  printf '%064d\n' 1; exit
fi
if [ "$1" = start ]; then [ "${START_FAIL:-0}" = 0 ]; exit; fi
if [ "$1" = rm ]; then [ "${CLEANUP_FAIL:-0}" = 0 ]; exit; fi
if [[ "$*" == *'SELECT 1'* ]]; then [ "${READY_FAIL:-0}" = 0 ]; exit; fi
if [[ "$*" == *'SELECT VERSION()'* ]]; then echo "${MYSQL_VERSION:-8.4.7}"; exit; fi
if [[ "$*" == *'exec -i'* ]]; then
  cat > "$IMPORTED_SQL"
  [ "${IMPORT_FAIL:-0}" = 0 ]; exit
fi
if [[ "$*" == *information_schema.statistics* ]]; then echo "${AUDIT_PRIMARY_KEY:-id}"; exit; fi
if [[ "$*" == *'COLUMN_NAME'* ]]; then
  echo "${AUDIT_COLUMNS:-id:bigint:NO,admin_id:bigint:NO,action_type:varchar(50):NO,target_type:varchar(20):NO,target_id:bigint:NO,created_at:datetime(6):NO,updated_at:datetime(6):NO}"
  exit
fi
if [[ "$*" == *information_schema.tables* ]] && [[ "$*" == *'count(*)'* ]]; then echo 37; exit; fi
if [[ "$*" == *information_schema.tables* ]]; then
  if [ "${AUDIT_MODE:-0}" = 1 ]; then echo admin_action_logs_archive; exit; fi
  grep -vx "${MISSING_TABLE:-not_a_table}" "$FIXTURE_TABLES"
  exit
fi
if [[ "$*" == *'WHERE success = 0'* ]]; then echo "${FAILED_MIGRATIONS:-0}"; exit; fi
if [[ "$*" == *'ORDER BY installed_rank DESC'* ]]; then echo "${FLYWAY_VERSION:-32}"; exit; fi
if [[ "$*" == *'SELECT count(*)'* ]]; then
  if [[ "$*" == *"sugang_helper.${MISSING_TABLE:-never_match};"* ]]; then exit 1; fi
  if [[ "$*" == *"${FAILED_TABLE_QUERY:-never_match}"* ]]; then exit 1; fi
  echo "${RESTORED_ROWS:-1}"; exit
fi
printf 'Unexpected Docker command in test: %s\n' "$*" >&2
exit 1
STUB
printf '#!/usr/bin/env bash\nexit 0\n' > "$tmp/bin/sleep"
chmod +x "$tmp/bin/docker" "$tmp/bin/sleep"
export PATH="$tmp/bin:$PATH" DOCKER_CALLS="$tmp/calls" IMPORTED_SQL="$tmp/imported.sql"
export FIXTURE_TABLES="$tmp/tables" RESTORE_WAIT_ATTEMPTS=2
cat > "$FIXTURE_TABLES" <<'TABLES'
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_CONTEXT
BATCH_JOB_EXECUTION_PARAMS
BATCH_JOB_EXECUTION_SEQ
BATCH_JOB_INSTANCE
BATCH_JOB_SEQ
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
BATCH_STEP_EXECUTION_SEQ
admin_action_logs
announcements
course_emoji_reviews
course_review_reactions
course_reviews
course_schedules
course_seat_histories
courses
crawler_run_failures
crawler_settings
crawler_status
custom_schedule_times
custom_schedules
dday_settings
feedback_attachments
feedback_replies
feedbacks
flyway_schema_history
notification_histories
schedules
seat_notification_deliveries
seat_notification_outbox
subscriptions
timetable_entries
timetables
user_devices
users
wishlists
TABLES
# The fixture inventory stays in sync with CREATE/DROP table migrations, not a reduced toy schema.
python3 - "$root/apps/server/src/main/resources/db/migration" "$FIXTURE_TABLES" <<'PYSCHEMA'
import pathlib, re, sys
names = {'flyway_schema_history'}
for path in sorted(pathlib.Path(sys.argv[1]).glob('V*.sql'), key=lambda p: int(p.name.split('__')[0][1:])):
    for operation, name in re.findall(r'(CREATE|DROP) TABLE (?:IF (?:NOT )?EXISTS )?`?([A-Za-z_][A-Za-z0-9_]*)', path.read_text(), re.I):
        if operation.upper() == 'DROP':
            names.discard(name)
            continue
        names.add(name)
assert sorted(names) == pathlib.Path(sys.argv[2]).read_text().splitlines()
PYSCHEMA
make_dump() {
  mkdir -p "$(dirname "$1")"
  printf '%s\n' "${2:-CREATE TABLE fixture (id INT);}" | gzip > "$1"
  shasum -a 256 "$1" > "$1.sha256"
}
run_restore() {
  : > "$DOCKER_CALLS"
  : > "$IMPORTED_SQL"
  bash "$root/infra/scripts/verify-mac-restore.sh" "$@" > "$tmp/out" 2>&1
}
expect_failure() {
  if run_restore "$@"; then echo "Expected restore failure: $*" >&2; exit 1; fi
  ! grep -q '"status": "success"'  "$tmp/out"
}
expect_cleanup() { grep -q '^rm -fv 0000000000000000000000000000000000000000000000000000000000000001$' "$DOCKER_CALLS"; }
full="$tmp/full.sql.gz"
make_dump "$full"
# When/then: missing core tables must fail instead of silently becoming zero rows.
MISSING_TABLE=users expect_failure "$full"
expect_cleanup
# Complete current schema, including legitimately empty user/course tables, is readable.
RESTORED_ROWS=0 run_restore "$full"
grep -q '"status": "success"'  "$tmp/out"
expect_cleanup
for table in $(cat "$FIXTURE_TABLES"); do grep -Fq "SELECT count(*) FROM sugang_helper.\`$table\`;" "$DOCKER_CALLS"; done
for failure in 'FAILED_TABLE_QUERY=feedbacks' 'FAILED_MIGRATIONS=1' 'FLYWAY_VERSION=31' 'MYSQL_VERSION=9.0.0' 'RESTORED_ROWS=not-a-number' 'IMPORT_FAIL=1' 'START_FAIL=1' 'READY_FAIL=1'; do
  export "$failure"
  expect_failure "$full"
  expect_cleanup
  unset "${failure%%=*}"
done
# The permanent TCP server is awaited; temp socket-only initialization cannot pass.
grep -q -- '--protocol=TCP -h127.0.0.1' "$DOCKER_CALLS"
test "$(grep -c 'SELECT 1' "$DOCKER_CALLS")" = 2
! grep -q 'exec -i' "$DOCKER_CALLS"
CREATE_FAIL=1 expect_failure "$full"
! grep -q '^rm ' "$DOCKER_CALLS"
CLEANUP_FAIL=1 expect_failure "$full"
# No networking, host ports, host mounts, production volumes, implicit image pulls.
run_restore "$full"
grep '^create ' "$DOCKER_CALLS" | grep -q -- '--network none'
grep '^create ' "$DOCKER_CALLS" | grep -q -- '--pull=never'
grep '^create ' "$DOCKER_CALLS" | grep -q -- '--event-scheduler=OFF'
! grep -Eq -- '(^| )(-p|--publish|--publish-all|-v|--volume|--mount)( |[=])' "$DOCKER_CALLS"
! grep -q -- '-pdrill\|sugang-helper-mysql' "$DOCKER_CALLS"
RESTORE_MYSQL_IMAGE="mysql@sha256:$(printf '%064d' 1)" run_restore "$full"
RESTORE_MYSQL_IMAGE=untrusted.example/mysql:8.4 expect_failure "$full"
test ! -s "$DOCKER_CALLS"
# A local backup must never create a container on a remote/production Docker context.
DOCKER_CONTEXT='' DOCKER_HOST=tcp://remote.example:2376 expect_failure "$full"
test ! -s "$DOCKER_CALLS"
DOCKER_ENDPOINT_FIXTURE=ssh://remote.example DOCKER_HOST='' expect_failure "$full"
! grep -q '^create ' "$DOCKER_CALLS"
# DOCKER_CONTEXT has precedence over DOCKER_HOST, matching Docker's documented CLI behavior.
DOCKER_CONTEXT=remote DOCKER_HOST=unix:///fixture.sock DOCKER_ENDPOINT_FIXTURE=ssh://remote.example expect_failure "$full"
grep -q '^context inspect remote ' "$DOCKER_CALLS"
! grep -q '^create ' "$DOCKER_CALLS"
# Latest selection ignores hidden partials and missing checksums, regardless of subdirectory order.
export LOCAL_BACKUP_DIR="$tmp/backups"
make_dump "$LOCAL_BACKUP_DIR/mysql/z-old/sugang_helper_20260101.sql.gz" 'old'
make_dump "$LOCAL_BACKUP_DIR/mysql/a-new/sugang_helper_20260201.sql.gz" 'new-complete'
touch -t 202601010000 "$LOCAL_BACKUP_DIR/mysql/z-old/"*.sql.gz
touch -t 202602010000 "$LOCAL_BACKUP_DIR/mysql/a-new/"*.sql.gz
make_dump "$LOCAL_BACKUP_DIR/mysql/.pending.new/sugang_helper_20990101.sql.gz" 'pending'
make_dump "$LOCAL_BACKUP_DIR/mysql/newest-partial.sql.gz" 'partial'
rm "$LOCAL_BACKUP_DIR/mysql/newest-partial.sql.gz.sha256"
make_dump "$LOCAL_BACKUP_DIR/mysql/full_backup_incomplete/sugang_helper_20990101.sql.gz" 'partial-bundle'
run_restore
grep -qx new-complete "$IMPORTED_SQL"
printf corruption >> "$LOCAL_BACKUP_DIR/mysql/a-new/"*.sql.gz
expect_failure
test ! -s "$DOCKER_CALLS"
# New full bundles require manifest and attachment integrity before any Docker invocation.
bundle="$tmp/full_backup_20260101T000000Z_fixture"
mkdir -p "$bundle" "$tmp/upload-fixture"
printf 'synthetic-attachment' > "$tmp/upload-fixture/file.txt"
bundle_dump="$bundle/sugang_helper_20260101T000000Z.sql.gz"
make_dump "$bundle_dump"
archive="$bundle/uploads_20260101T000000Z.tar.gz"
tar -czf "$archive" -C "$tmp/upload-fixture" .
shasum -a 256 "$archive" > "$archive.sha256"
printf 'schema=full-backup-v1\ndatabase=sugang_helper\ndump_file=%s\nuploads=included\nuploads_file=%s\n' \
  "$(basename "$bundle_dump")" "$(basename "$archive")" > "$bundle/backup.manifest"
shasum -a 256 "$bundle/backup.manifest" > "$bundle/backup.manifest.sha256"
run_restore "$bundle_dump"
python3 - "$tmp/out" <<'PYLOG'
import json, sys
rows = [json.loads(line) for line in open(sys.argv[1])]
assert rows[-1]['status'] == 'success'
assert sum(row['event'] == 'restore_table_verified' for row in rows) == 37
PYLOG
rm "$archive.sha256"
expect_failure "$bundle_dump"
test ! -s "$DOCKER_CALLS"
printf not-gzip > "$archive"
shasum -a 256 "$archive" > "$archive.sha256"
expect_failure "$bundle_dump"
test ! -s "$DOCKER_CALLS"
printf 'schema=full-backup-v1\ndatabase=sugang_helper\ndump_file=%s\nuploads=excluded\n' \
  "$(basename "$bundle_dump")" > "$bundle/backup.manifest"
shasum -a 256 "$bundle/backup.manifest" > "$bundle/backup.manifest.sha256"
run_restore "$bundle_dump"
grep -q 'Explicit DB-only bundle' "$tmp/out"
rm "$bundle/backup.manifest" "$bundle/backup.manifest.sha256"
expect_failure "$bundle_dump"
test ! -s "$DOCKER_CALLS"
# Audit schema and exact manifest count, including zero, remain enforced.
audit="$tmp/admin_audit_fixture.sql.gz"
make_dump "$audit"
printf 'schema=admin-audit-core-v1\nrows=1\n' > "$audit.manifest"
shasum -a 256 "$audit.manifest" > "$audit.manifest.sha256"
export AUDIT_MODE=1
run_restore "$audit"
RESTORED_ROWS=0 expect_failure "$audit"
AUDIT_COLUMNS=wrong expect_failure "$audit"
AUDIT_PRIMARY_KEY=id,admin_id expect_failure "$audit"
printf 'schema=admin-audit-core-v1\nrows=0\n' > "$audit.manifest"
shasum -a 256 "$audit.manifest" > "$audit.manifest.sha256"
RESTORED_ROWS=0 run_restore "$audit"
printf corruption >> "$audit.manifest"
expect_failure "$audit"
test ! -s "$DOCKER_CALLS"
unset AUDIT_MODE
printf corruption >> "$full"
expect_failure "$full"
test ! -s "$DOCKER_CALLS"
printf 'not-gzip' > "$full"
shasum -a 256 "$full" > "$full.sha256"
expect_failure "$full"
test ! -s "$DOCKER_CALLS"
RESTORE_WAIT_ATTEMPTS=0 expect_failure "$full"
test ! -s "$DOCKER_CALLS"
echo 'Restore isolation, complete-backup selection, schema, data-read and failure contracts passed'
