#!/usr/bin/env bash
set -euo pipefail
umask 077

log_restore_event() {
  python3 - "$1" "$2" "$3" "${4:-}" <<'PYLOG'
import json, sys
print(json.dumps({"event": "restore_drill", "status": sys.argv[1], "stage": sys.argv[2],
                  "message": sys.argv[3], "target": sys.argv[4]}, ensure_ascii=False))
PYLOG
}
log_table_count() {
  python3 - "$1" "$2" <<'PYLOG'
import json, sys
print(json.dumps({"event": "restore_table_verified", "table": sys.argv[1], "rows": int(sys.argv[2])}))
PYLOG
}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-$HOME/Backups/jbnu-sugang-helper}"
DUMP_FILE="${1:-}"
RESTORE_WAIT_ATTEMPTS="${RESTORE_WAIT_ATTEMPTS:-120}"
RESTORE_MYSQL_IMAGE="${RESTORE_MYSQL_IMAGE:-mysql:8.4}"
[[ "$RESTORE_WAIT_ATTEMPTS" =~ ^[1-9][0-9]{0,2}$ ]] && [ "$RESTORE_WAIT_ATTEMPTS" -le 600 ] || {
  log_restore_event failed preflight 'RESTORE_WAIT_ATTEMPTS must be between 1 and 600' >&2; exit 1;
}
[[ "$RESTORE_MYSQL_IMAGE" =~ ^mysql(:8\.4(\.[0-9]+)?|@sha256:[a-f0-9]{64})$ ]] || {
  log_restore_event failed preflight 'Use a locally prepared official mysql:8.4 image or mysql@sha256 digest' >&2; exit 1;
}

if [ -z "$DUMP_FILE" ]; then
  # cp -p preserves creation-time mtime during sync. Never select partial/staging paths.
  if ! DUMP_FILE="$(python3 - "${LOCAL_BACKUP_DIR}/mysql" 2>/dev/null <<'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1]).resolve()
candidates = []
for path in root.rglob('*.sql.gz'):
    if path.name.startswith('admin_audit_'):
        continue
    relative = path.relative_to(root)
    if any(part.startswith('.') for part in relative.parts):
        continue
    if any(parent.is_symlink() for parent in (path, *path.parents)):
        continue
    checksum = pathlib.Path(str(path) + '.sha256')
    if path.is_file() and checksum.is_file() and not checksum.is_symlink():
        if path.parent.name.startswith('full_backup_'):
            metadata = path.parent / 'backup.manifest'
            if not metadata.is_file() or not pathlib.Path(str(metadata) + '.sha256').is_file():
                continue
            entries = dict(line.split('=', 1) for line in metadata.read_text().splitlines() if '=' in line)
            uploads = entries.get('uploads_file', '')
            if entries.get('uploads') == 'included' and pathlib.Path(uploads).name == uploads:
                if not (path.parent / uploads).is_file() or not (path.parent / (uploads + '.sha256')).is_file():
                    continue
        candidates.append(path)
if not candidates:
    raise SystemExit('No complete .sql.gz/checksum pair found; choose an explicit backup path')
print(max(candidates, key=lambda p: (p.stat().st_mtime_ns, p.name, str(p))))
PY
)"; then
    log_restore_event failed selection 'No complete readable backup found; choose an explicit backup path' >&2
    exit 1
  fi
fi
[ -f "$DUMP_FILE" ] && [[ "$DUMP_FILE" == *.sql.gz ]] || {
  log_restore_event failed preflight 'Provide a readable .sql.gz backup file' >&2; exit 1;
}

verify_checksum() {
  local file="$1" expected actual
  [ -f "$file" ] && [ -f "${file}.sha256" ] || { log_restore_event failed checksum 'Missing checksum; restore refused' >&2; return 1; }
  expected="$(awk 'NR == 1 {print $1}' "${file}.sha256")"
  [[ "$expected" =~ ^[a-f0-9]{64}$ ]] || { log_restore_event failed checksum 'Invalid checksum' >&2; return 1; }
  actual="$(shasum -a 256 "$file" | awk '{print $1}')"
  [ "$expected" = "$actual" ] || { log_restore_event failed checksum 'Checksum mismatch; restore refused' >&2; return 1; }
}

verify_checksum "$DUMP_FILE"
gzip -t "$DUMP_FILE" 2>/dev/null || { log_restore_event failed compression 'Invalid gzip backup' >&2; exit 1; }
verify_full_bundle() {
  local manifest="$(dirname "$DUMP_FILE")/backup.manifest" dump_name uploads_mode uploads_name
  if [ ! -f "$manifest" ] && [[ "$(basename "$(dirname "$DUMP_FILE")")" != full_backup_* ]]; then
    log_restore_event limited bundle 'Legacy DB-only backup: attachment bundle is not verified'
    return
  fi
  verify_checksum "$manifest"
  grep -qx 'schema=full-backup-v1' "$manifest" && grep -qx 'database=sugang_helper' "$manifest" || {
    log_restore_event failed manifest 'Unsupported full backup manifest' >&2; return 1;
  }
  dump_name="$(sed -n 's/^dump_file=//p' "$manifest")"
  [ "$dump_name" = "$(basename "$DUMP_FILE")" ] || {
    log_restore_event failed manifest 'Backup manifest dump filename mismatch' >&2; return 1;
  }
  uploads_mode="$(sed -n 's/^uploads=//p' "$manifest")"
  if [ "$uploads_mode" = excluded ]; then
    ! grep -q '^uploads_file=' "$manifest" || { log_restore_event failed manifest 'Unexpected excluded uploads entry' >&2; return 1; }
    log_restore_event limited bundle 'Explicit DB-only bundle: attachments excluded'
    return
  fi
  [ "$uploads_mode" = included ] || { log_restore_event failed manifest 'Invalid uploads mode' >&2; return 1; }
  uploads_name="$(sed -n 's/^uploads_file=//p' "$manifest")"
  [[ "$uploads_name" =~ ^uploads_[0-9]{8}T[0-9]{6}Z\.tar\.gz$ ]] || {
    log_restore_event failed manifest 'Invalid uploads filename' >&2; return 1;
  }
  verify_checksum "$(dirname "$DUMP_FILE")/$uploads_name"
  gzip -t "$(dirname "$DUMP_FILE")/$uploads_name" 2>/dev/null && \
    tar -tzf "$(dirname "$DUMP_FILE")/$uploads_name" >/dev/null 2>&1 || {
    log_restore_event failed uploads 'Invalid uploads archive' >&2; return 1;
  }
  log_restore_event verified bundle 'DB and uploads bundle integrity verified; no attachments extracted'
}
AUDIT_ROWS=""
if [[ "$(basename "$DUMP_FILE")" == admin_audit_* ]]; then
  verify_checksum "${DUMP_FILE}.manifest"
  grep -qx 'schema=admin-audit-core-v1' "${DUMP_FILE}.manifest" || { log_restore_event failed manifest 'Invalid audit schema version' >&2; exit 1; }
  AUDIT_ROWS="$(sed -n 's/^rows=//p' "${DUMP_FILE}.manifest")"
  [[ "$AUDIT_ROWS" =~ ^[0-9]+$ ]] || { log_restore_event failed manifest 'Invalid audit row count' >&2; exit 1; }
fi

if [ -z "$AUDIT_ROWS" ]; then verify_full_bundle; fi

effective_docker_endpoint() {
  if [ -n "${DOCKER_CONTEXT:-}" ]; then
    docker context inspect "$DOCKER_CONTEXT" --format '{{.Endpoints.docker.Host}}' 2>/dev/null
    return
  fi
  if [ -n "${DOCKER_HOST:-}" ]; then
    printf '%s\n' "$DOCKER_HOST"
    return
  fi
  docker context inspect --format '{{.Endpoints.docker.Host}}' 2>/dev/null
}
if ! DOCKER_ENDPOINT="$(effective_docker_endpoint)"; then
  log_restore_event failed preflight 'Cannot determine the current Docker endpoint' >&2
  exit 1
fi
[[ "$DOCKER_ENDPOINT" == unix:///* ]] || {
  log_restore_event failed preflight 'Restore drill requires a local Unix Docker endpoint; remote contexts are refused' >&2
  exit 1
}
log_restore_event started restore 'Verified dump selected' "$DUMP_FILE"
CONTAINER_NAME="mac-restore-drill-mysql-$(date +'%s')-$$"
CONTAINER_ID=""
TEST_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_hex(24))')"

remove_container() {
  [ -n "$CONTAINER_ID" ] || return 0
  if ! docker rm -fv "$CONTAINER_ID" >/dev/null 2>&1; then
    log_restore_event failed cleanup 'Temporary container cleanup failed' "$CONTAINER_ID" >&2
    return 1
  fi
  CONTAINER_ID=""
}
cleanup() {
  local status=$?
  trap - EXIT
  remove_container || status=1
  if [ "$status" -ne 0 ]; then log_restore_event failed verify 'Restore verification did not complete' >&2; fi
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# Create and start separately: cleanup only targets an ID this invocation created.
# No network egress, host ports, bind mounts or production volumes. No implicit pull.
if ! CONTAINER_ID="$(docker create --name "$CONTAINER_NAME" --network none --pull=never \
  -e MYSQL_ROOT_PASSWORD="$TEST_PASSWORD" -e MYSQL_DATABASE=sugang_helper \
  "$RESTORE_MYSQL_IMAGE" --event-scheduler=OFF --local-infile=OFF \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci 2>/dev/null)"; then
  CONTAINER_ID=""
  log_restore_event failed container 'Container creation failed; inspect this exact name if Docker reported an uncertain result' "$CONTAINER_NAME" >&2
  exit 1
fi
[[ "$CONTAINER_ID" =~ ^[a-f0-9]{64}$ ]] || { CONTAINER_ID=""; log_restore_event failed container 'Invalid temporary container ID' >&2; exit 1; }
docker start "$CONTAINER_ID" >/dev/null 2>&1 || {
  log_restore_event failed container 'Temporary MySQL container failed to start' >&2; exit 1;
}

mysql_query() {
  docker exec -e MYSQL_PWD="$TEST_PASSWORD" "$CONTAINER_ID" \
    mysql --protocol=TCP -h127.0.0.1 --connect-timeout=2 -uroot \
    --batch --skip-column-names --binary-mode=1 --local-infile=0 sugang_helper -e "$1" 2>/dev/null
}
MYSQL_READY=0
for ((attempt = 0; attempt < RESTORE_WAIT_ATTEMPTS; attempt++)); do
  if mysql_query 'SELECT 1' >/dev/null; then MYSQL_READY=1; break; fi
  sleep 1
done
[ "$MYSQL_READY" = 1 ] || { log_restore_event failed readiness 'MySQL readiness timed out' >&2; exit 1; }
MYSQL_VERSION="$(mysql_query 'SELECT VERSION()')"
[[ "$MYSQL_VERSION" == 8.4.* ]] || { log_restore_event failed readiness 'Restore requires MySQL 8.4' >&2; exit 1; }

# Never print import errors or rows: a MySQL error may contain private dump contents.
if ! gzip -dc "$DUMP_FILE" | docker exec -i -e MYSQL_PWD="$TEST_PASSWORD" "$CONTAINER_ID" \
  mysql --protocol=TCP -h127.0.0.1 --connect-timeout=2 -uroot \
  --binary-mode=1 --local-infile=0 sugang_helper >/dev/null 2>&1; then
  log_restore_event failed import 'SQL import failed; private SQL details suppressed' >&2
  exit 1
fi
TABLES="$(mysql_query "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema='sugang_helper' AND TABLE_TYPE='BASE TABLE' ORDER BY BINARY TABLE_NAME;")"

verify_audit() {
  local rows columns primary_key
  [ "$TABLES" = admin_action_logs_archive ] || { log_restore_event failed schema 'Unexpected audit tables' >&2; return 1; }
  columns="$(mysql_query "SELECT GROUP_CONCAT(CONCAT(COLUMN_NAME, ':', COLUMN_TYPE, ':', IS_NULLABLE) ORDER BY ORDINAL_POSITION SEPARATOR ',') FROM information_schema.columns WHERE TABLE_SCHEMA='sugang_helper' AND TABLE_NAME='admin_action_logs_archive';")"
  [ "$columns" = 'id:bigint:NO,admin_id:bigint:NO,action_type:varchar(50):NO,target_type:varchar(20):NO,target_id:bigint:NO,created_at:datetime(6):NO,updated_at:datetime(6):NO' ] || {
    log_restore_event failed schema 'Audit column schema mismatch' >&2; return 1;
  }
  primary_key="$(mysql_query "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.statistics WHERE TABLE_SCHEMA='sugang_helper' AND TABLE_NAME='admin_action_logs_archive' AND INDEX_NAME='PRIMARY';")"
  [ "$primary_key" = id ] || { log_restore_event failed schema 'Audit primary key missing' >&2; return 1; }
  rows="$(mysql_query 'SELECT count(*) FROM sugang_helper.admin_action_logs_archive;')"
  [ "$rows" = "$AUDIT_ROWS" ] || { log_restore_event failed counts 'Audit row count does not match manifest' >&2; return 1; }
}

verify_full_database() {
  local required table rows failed version expected_version
  # Current V32 schema. Intentional schema changes must update this contract and tests.
  required=(BATCH_JOB_EXECUTION BATCH_JOB_EXECUTION_CONTEXT BATCH_JOB_EXECUTION_PARAMS
    BATCH_JOB_EXECUTION_SEQ BATCH_JOB_INSTANCE BATCH_JOB_SEQ BATCH_STEP_EXECUTION
    BATCH_STEP_EXECUTION_CONTEXT BATCH_STEP_EXECUTION_SEQ admin_action_logs announcements
    course_emoji_reviews course_review_reactions course_reviews course_schedules
    course_seat_histories courses crawler_run_failures crawler_settings crawler_status
    custom_schedule_times custom_schedules dday_settings feedback_attachments feedback_replies
    feedbacks flyway_schema_history notification_histories schedules seat_notification_deliveries
    seat_notification_outbox subscriptions timetable_entries timetables user_devices users wishlists)
  [ "$TABLES" = "$(printf '%s\n' "${required[@]}" | LC_ALL=C sort)" ] || {
    log_restore_event failed schema 'Full database table inventory does not match the current schema' >&2; return 1;
  }
  failed="$(mysql_query 'SELECT count(*) FROM sugang_helper.flyway_schema_history WHERE success = 0;')"
  [ "$failed" = 0 ] || { log_restore_event failed schema 'Failed Flyway migration found' >&2; return 1; }
  if ! expected_version="$(python3 - "${SCRIPT_DIR}/../../apps/server/src/main/resources/db/migration" 2>/dev/null <<'PY'
import pathlib, re, sys
versions = [int(match.group(1)) for p in pathlib.Path(sys.argv[1]).glob('V*__*.sql')
            if (match := re.fullmatch(r'V(\d+)__.*\.sql', p.name))]
if not versions:
    raise SystemExit('No migration contract found; use this script from the matching repository checkout')
print(max(versions))
PY
)"; then
    log_restore_event failed schema 'No migration files found; run from the matching repository checkout' >&2
    return 1
  fi
  version="$(mysql_query 'SELECT version FROM sugang_helper.flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;')"
  [ "$version" = "$expected_version" ] || { log_restore_event failed schema 'Flyway version differs from this checkout; use the matching release' >&2; return 1; }
  for table in "${required[@]}"; do
    rows="$(mysql_query "SELECT count(*) FROM sugang_helper.\`${table}\`;")" || {
      log_restore_event failed counts 'Table read failed' "$table" >&2; return 1;
    }
    [[ "$rows" =~ ^[0-9]+$ ]] || { log_restore_event failed counts 'Invalid row count' "$table" >&2; return 1; }
    log_table_count "$table" "$rows"
  done
  log_restore_event verified counts 'Counts prove readability, not source-snapshot equality or application compatibility'
}

if [ -n "$AUDIT_ROWS" ]; then verify_audit; fi
if [ -z "$AUDIT_ROWS" ]; then verify_full_database; fi
if ! remove_container; then trap - EXIT; exit 1; fi
trap - EXIT
log_restore_event success complete 'Isolated restore verification and temporary volume cleanup completed'
