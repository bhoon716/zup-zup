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
  log_restore_event failed preflight 'RESTORE_WAIT_ATTEMPTS는 1부터 600 사이여야 합니다.' >&2; exit 1;
}
[[ "$RESTORE_MYSQL_IMAGE" =~ ^mysql(:8\.4(\.[0-9]+)?|@sha256:[a-f0-9]{64})$ ]] || {
  log_restore_event failed preflight '로컬에 준비한 공식 mysql:8.4 이미지나 mysql@sha256 다이제스트를 사용하세요.' >&2; exit 1;
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
    log_restore_event failed selection '읽을 수 있는 전체 백업을 찾지 못했습니다. 백업 경로를 직접 지정하세요.' >&2
    exit 1
  fi
fi
[ -f "$DUMP_FILE" ] && [[ "$DUMP_FILE" == *.sql.gz ]] || {
  log_restore_event failed preflight '읽을 수 있는 .sql.gz 백업 파일을 지정하세요.' >&2; exit 1;
}

verify_checksum() {
  local file="$1" expected actual
  [ -f "$file" ] && [ -f "${file}.sha256" ] || { log_restore_event failed checksum '체크섬이 없어 복원을 중단합니다.' >&2; return 1; }
  expected="$(awk 'NR == 1 {print $1}' "${file}.sha256")"
  [[ "$expected" =~ ^[a-f0-9]{64}$ ]] || { log_restore_event failed checksum '체크섬 형식이 올바르지 않습니다.' >&2; return 1; }
  actual="$(shasum -a 256 "$file" | awk '{print $1}')"
  [ "$expected" = "$actual" ] || { log_restore_event failed checksum '체크섬이 일치하지 않아 복원을 중단합니다.' >&2; return 1; }
}

verify_checksum "$DUMP_FILE"
gzip -t "$DUMP_FILE" 2>/dev/null || { log_restore_event failed compression 'gzip 백업 파일이 올바르지 않습니다.' >&2; exit 1; }
verify_full_bundle() {
  local manifest="$(dirname "$DUMP_FILE")/backup.manifest" dump_name uploads_mode uploads_name
  if [ ! -f "$manifest" ] && [[ "$(basename "$(dirname "$DUMP_FILE")")" != full_backup_* ]]; then
    log_restore_event limited bundle '이전 DB 전용 백업입니다. 첨부파일 묶음은 검증되지 않았습니다.'
    return
  fi
  verify_checksum "$manifest"
  grep -qx 'schema=full-backup-v1' "$manifest" && grep -qx 'database=sugang_helper' "$manifest" || {
    log_restore_event failed manifest '지원하지 않는 전체 백업 매니페스트입니다.' >&2; return 1;
  }
  dump_name="$(sed -n 's/^dump_file=//p' "$manifest")"
  [ "$dump_name" = "$(basename "$DUMP_FILE")" ] || {
    log_restore_event failed manifest '덤프 파일명이 백업 매니페스트와 일치하지 않습니다.' >&2; return 1;
  }
  uploads_mode="$(sed -n 's/^uploads=//p' "$manifest")"
  if [ "$uploads_mode" = excluded ]; then
    ! grep -q '^uploads_file=' "$manifest" || { log_restore_event failed manifest '업로드를 제외한 백업에 업로드 항목이 있습니다.' >&2; return 1; }
    log_restore_event limited bundle 'DB 전용 백업으로 지정되어 첨부파일이 제외됐습니다.'
    return
  fi
  [ "$uploads_mode" = included ] || { log_restore_event failed manifest '업로드 포함 설정이 올바르지 않습니다.' >&2; return 1; }
  uploads_name="$(sed -n 's/^uploads_file=//p' "$manifest")"
  [[ "$uploads_name" =~ ^uploads_[0-9]{8}T[0-9]{6}Z\.tar\.gz$ ]] || {
    log_restore_event failed manifest '업로드 압축본 파일명이 올바르지 않습니다.' >&2; return 1;
  }
  verify_checksum "$(dirname "$DUMP_FILE")/$uploads_name"
  gzip -t "$(dirname "$DUMP_FILE")/$uploads_name" 2>/dev/null && \
    tar -tzf "$(dirname "$DUMP_FILE")/$uploads_name" >/dev/null 2>&1 || {
    log_restore_event failed uploads '업로드 압축본이 올바르지 않습니다.' >&2; return 1;
  }
  log_restore_event verified bundle 'DB와 업로드 백업의 무결성을 검증했습니다. 첨부파일은 추출하지 않았습니다.'
}
AUDIT_ROWS=""
if [[ "$(basename "$DUMP_FILE")" == admin_audit_* ]]; then
  verify_checksum "${DUMP_FILE}.manifest"
  grep -qx 'schema=admin-audit-core-v1' "${DUMP_FILE}.manifest" || { log_restore_event failed manifest '감사 로그 스키마 버전이 올바르지 않습니다.' >&2; exit 1; }
  AUDIT_ROWS="$(sed -n 's/^rows=//p' "${DUMP_FILE}.manifest")"
  [[ "$AUDIT_ROWS" =~ ^[0-9]+$ ]] || { log_restore_event failed manifest '감사 로그 행 수가 올바르지 않습니다.' >&2; exit 1; }
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
  log_restore_event failed preflight '현재 Docker 접속 주소를 확인할 수 없습니다.' >&2
  exit 1
fi
[[ "$DOCKER_ENDPOINT" == unix:///* ]] || {
  log_restore_event failed preflight '복원 점검에는 로컬 Docker 유닉스 소켓이 필요합니다. 원격 컨텍스트에서는 실행할 수 없습니다.' >&2
  exit 1
}
log_restore_event started restore '검증된 덤프를 선택했습니다.' "$DUMP_FILE"
CONTAINER_NAME="mac-restore-drill-mysql-$(date +'%s')-$$"
CONTAINER_ID=""
TEST_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_hex(24))')"

remove_container() {
  [ -n "$CONTAINER_ID" ] || return 0
  if ! docker rm -fv "$CONTAINER_ID" >/dev/null 2>&1; then
    log_restore_event failed cleanup '임시 컨테이너를 정리하지 못했습니다.' "$CONTAINER_ID" >&2
    return 1
  fi
  CONTAINER_ID=""
}
cleanup() {
  local status=$?
  trap - EXIT
  remove_container || status=1
  if [ "$status" -ne 0 ]; then log_restore_event failed verify '복원 검증을 완료하지 못했습니다.' >&2; fi
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
  log_restore_event failed container '컨테이너를 생성하지 못했습니다. Docker 결과가 불명확하면 표시된 컨테이너 이름으로 확인하세요.' "$CONTAINER_NAME" >&2
  exit 1
fi
[[ "$CONTAINER_ID" =~ ^[a-f0-9]{64}$ ]] || { CONTAINER_ID=""; log_restore_event failed container '임시 컨테이너 ID가 올바르지 않습니다.' >&2; exit 1; }
docker start "$CONTAINER_ID" >/dev/null 2>&1 || {
  log_restore_event failed container '임시 MySQL 컨테이너를 시작하지 못했습니다.' >&2; exit 1;
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
[ "$MYSQL_READY" = 1 ] || { log_restore_event failed readiness 'MySQL 준비 상태 확인 시간이 초과됐습니다.' >&2; exit 1; }
MYSQL_VERSION="$(mysql_query 'SELECT VERSION()')"
[[ "$MYSQL_VERSION" == 8.4.* ]] || { log_restore_event failed readiness '복원에는 MySQL 8.4가 필요합니다.' >&2; exit 1; }

# Never print import errors or rows: a MySQL error may contain private dump contents.
if ! gzip -dc "$DUMP_FILE" | docker exec -i -e MYSQL_PWD="$TEST_PASSWORD" "$CONTAINER_ID" \
  mysql --protocol=TCP -h127.0.0.1 --connect-timeout=2 -uroot \
  --binary-mode=1 --local-infile=0 sugang_helper >/dev/null 2>&1; then
  log_restore_event failed import 'SQL 가져오기에 실패했습니다. 민감한 SQL 상세는 생략했습니다.' >&2
  exit 1
fi
TABLES="$(mysql_query "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema='sugang_helper' AND TABLE_TYPE='BASE TABLE' ORDER BY BINARY TABLE_NAME;")"

verify_audit() {
  local rows columns primary_key
  [ "$TABLES" = admin_action_logs_archive ] || { log_restore_event failed schema '예상한 감사 로그 테이블 구성이 아닙니다.' >&2; return 1; }
  columns="$(mysql_query "SELECT GROUP_CONCAT(CONCAT(COLUMN_NAME, ':', COLUMN_TYPE, ':', IS_NULLABLE) ORDER BY ORDINAL_POSITION SEPARATOR ',') FROM information_schema.columns WHERE TABLE_SCHEMA='sugang_helper' AND TABLE_NAME='admin_action_logs_archive';")"
  [ "$columns" = 'id:bigint:NO,admin_id:bigint:NO,action_type:varchar(50):NO,target_type:varchar(20):NO,target_id:bigint:NO,created_at:datetime(6):NO,updated_at:datetime(6):NO' ] || {
    log_restore_event failed schema '감사 로그 컬럼 스키마가 일치하지 않습니다.' >&2; return 1;
  }
  primary_key="$(mysql_query "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.statistics WHERE TABLE_SCHEMA='sugang_helper' AND TABLE_NAME='admin_action_logs_archive' AND INDEX_NAME='PRIMARY';")"
  [ "$primary_key" = id ] || { log_restore_event failed schema '감사 로그 기본 키가 없습니다.' >&2; return 1; }
  rows="$(mysql_query 'SELECT count(*) FROM sugang_helper.admin_action_logs_archive;')"
  [ "$rows" = "$AUDIT_ROWS" ] || { log_restore_event failed counts '감사 로그 행 수가 매니페스트와 일치하지 않습니다.' >&2; return 1; }
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
    log_restore_event failed schema '백업의 DB 테이블 목록이 현재 스키마와 일치하지 않습니다.' >&2; return 1;
  }
  failed="$(mysql_query 'SELECT count(*) FROM sugang_helper.flyway_schema_history WHERE success = 0;')"
  [ "$failed" = 0 ] || { log_restore_event failed schema '실패한 Flyway 마이그레이션 이력을 발견했습니다.' >&2; return 1; }
  if ! expected_version="$(python3 - "${SCRIPT_DIR}/../../apps/server/src/main/resources/db/migration" 2>/dev/null <<'PY'
import pathlib, re, sys
versions = [int(match.group(1)) for p in pathlib.Path(sys.argv[1]).glob('V*__*.sql')
            if (match := re.fullmatch(r'V(\d+)__.*\.sql', p.name))]
if not versions:
    raise SystemExit('No migration contract found; use this script from the matching repository checkout')
print(max(versions))
PY
)"; then
    log_restore_event failed schema '마이그레이션 파일이 없습니다. 백업 버전과 일치하는 체크아웃에서 실행하세요.' >&2
    return 1
  fi
  version="$(mysql_query 'SELECT version FROM sugang_helper.flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;')"
  [ "$version" = "$expected_version" ] || { log_restore_event failed schema 'Flyway 버전이 현재 체크아웃과 다릅니다. 일치하는 릴리스에서 실행하세요.' >&2; return 1; }
  for table in "${required[@]}"; do
    rows="$(mysql_query "SELECT count(*) FROM sugang_helper.\`${table}\`;")" || {
      log_restore_event failed counts '테이블을 읽지 못했습니다.' "$table" >&2; return 1;
    }
    [[ "$rows" =~ ^[0-9]+$ ]] || { log_restore_event failed counts '행 수가 올바르지 않습니다.' "$table" >&2; return 1; }
    log_table_count "$table" "$rows"
  done
  log_restore_event verified counts '행 수를 읽을 수 있음을 확인했습니다. 원본 스냅샷과의 일치 여부 및 앱 호환성은 별도 확인이 필요합니다.'
}

if [ -n "$AUDIT_ROWS" ]; then verify_audit; fi
if [ -z "$AUDIT_ROWS" ]; then verify_full_database; fi
if ! remove_container; then trap - EXIT; exit 1; fi
trap - EXIT
log_restore_event success complete '격리 복원 검증과 임시 볼륨 정리를 완료했습니다.'
