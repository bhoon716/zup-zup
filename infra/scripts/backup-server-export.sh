#!/usr/bin/env bash
set -euo pipefail
umask 077
log_backup_event() {
  python3 - "$1" "$2" "$3" "${4:-}" <<'PYLOG'
import json, sys
print(json.dumps({"event": "backup_export", "status": sys.argv[1], "stage": sys.argv[2],
                  "message": sys.argv[3], "bundle": sys.argv[4]}, ensure_ascii=False))
PYLOG
}
[ -n "${DB_ROOT_PASSWORD:-}" ] || {
  log_backup_event failed preflight 'DB_ROOT_PASSWORD must be supplied by the existing backup environment' >&2; exit 1;
}

RELEASE_ROOT="${RELEASE_ROOT:-/home/ubuntu/jbnu-sugang-helper}"
BACKUP_DIR="${RELEASE_ROOT}/backups/export"
BACKUP_INCLUDE_UPLOADS="${BACKUP_INCLUDE_UPLOADS:-1}"
# An explicit readable host path is optional. Default uses the app's existing mount,
# so the ordinary backup owner never needs Docker data-root filesystem permissions.
UPLOADS_VOL_PATH="${UPLOADS_VOL_PATH:-}"
[[ "$BACKUP_INCLUDE_UPLOADS" = 0 || "$BACKUP_INCLUDE_UPLOADS" = 1 ]] || {
  log_backup_event failed preflight 'BACKUP_INCLUDE_UPLOADS must be 0 (explicit DB-only) or 1 (required uploads)' >&2; exit 1;
}
mkdir -p "$BACKUP_DIR"
chmod 0700 "$BACKUP_DIR"
STAGING_DIR="$(mktemp -d "${BACKUP_DIR}/.pending.XXXXXX")"
cleanup() { rm -rf "$STAGING_DIR"; }
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
TIMESTAMP="$(date -u +'%Y%m%dT%H%M%SZ')"
BUNDLE_NAME="full_backup_${TIMESTAMP}_${STAGING_DIR##*.}"
DB_FILE="sugang_helper_${TIMESTAMP}.sql.gz"
UPLOADS_FILE="uploads_${TIMESTAMP}.tar.gz"

if ! docker ps --format '{{.Names}}' | grep -Fxq 'sugang-helper-mysql'; then
  log_backup_event failed preflight 'Required MySQL container is not running' >&2
  exit 1
fi
if [ "$BACKUP_INCLUDE_UPLOADS" = 1 ] && [ -n "$UPLOADS_VOL_PATH" ] && [ ! -d "$UPLOADS_VOL_PATH" ]; then
  log_backup_event failed preflight 'Required uploads directory is missing; no complete bundle published' >&2
  exit 1
fi

log_backup_event started export 'Creating staging files for a complete backup bundle'
if ! docker exec -e MYSQL_PWD="$DB_ROOT_PASSWORD" sugang-helper-mysql mysqldump \
  --single-transaction --source-data=2 --no-tablespaces --routines --events --triggers \
  -u root sugang_helper 2>/dev/null | gzip -9 > "${STAGING_DIR}/${DB_FILE}"; then
  log_backup_event failed export 'MySQL export failed; private error details suppressed' >&2
  exit 1
fi
gzip -t "${STAGING_DIR}/${DB_FILE}" 2>/dev/null || {
  log_backup_event failed compression 'DB gzip verification failed' >&2; exit 1;
}
(cd "$STAGING_DIR" && shasum -a 256 "$DB_FILE" > "${DB_FILE}.sha256")
printf 'schema=full-backup-v1\ncreated_at_utc=%s\ndatabase=sugang_helper\ndump_file=%s\n' \
  "$TIMESTAMP" "$DB_FILE" > "${STAGING_DIR}/backup.manifest"
archive_uploads() {
  if [ -n "$UPLOADS_VOL_PATH" ]; then
    tar -czf "${STAGING_DIR}/${UPLOADS_FILE}" -C "$UPLOADS_VOL_PATH" . 2>/dev/null
    return
  fi
  docker exec sugang-helper-app tar -czf - -C /app/data/uploads . \
    > "${STAGING_DIR}/${UPLOADS_FILE}" 2>/dev/null
}
if [ "$BACKUP_INCLUDE_UPLOADS" = 1 ]; then
  if ! archive_uploads || ! tar -tzf "${STAGING_DIR}/${UPLOADS_FILE}" >/dev/null 2>&1; then
    log_backup_event failed uploads 'Uploads archive creation or verification failed' >&2
    exit 1
  fi
  (cd "$STAGING_DIR" && shasum -a 256 "$UPLOADS_FILE" > "${UPLOADS_FILE}.sha256")
  printf 'uploads=included\nuploads_file=%s\n' "$UPLOADS_FILE" >> "${STAGING_DIR}/backup.manifest"
fi
if [ "$BACKUP_INCLUDE_UPLOADS" = 0 ]; then
  printf 'uploads=excluded\n' >> "${STAGING_DIR}/backup.manifest"
fi
(cd "$STAGING_DIR" && shasum -a 256 backup.manifest > backup.manifest.sha256)
# Audit facts have a separate long-lived retention scope. Failure still prevents bundle publication.
RELEASE_ROOT="$RELEASE_ROOT" bash "$(dirname "$0")/backup-admin-audit.sh"
mv "$STAGING_DIR" "${BACKUP_DIR}/${BUNDLE_NAME}"
log_backup_event success complete 'Complete backup bundle published' "$BUNDLE_NAME"
