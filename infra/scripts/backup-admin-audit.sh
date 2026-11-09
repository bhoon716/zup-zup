#!/usr/bin/env bash
# Important audit facts only, never reply bodies, arbitrary JSON or diagnostic logs.
set -euo pipefail
umask 077
: "${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD must be supplied by the existing backup environment}"
RELEASE_ROOT="${RELEASE_ROOT:-/home/ubuntu/jbnu-sugang-helper}"
log_archive_completed() {
  printf '{"event":"admin_audit_archive_completed","schemaVersion":1,"rows":%s,"message":"최소 감사 기록 보관 완료"}\n' "$1"
}
archive_root="${RELEASE_ROOT}/backups/audit"
mkdir -p "$archive_root"
staging="$(mktemp -d "${archive_root}/.pending.XXXXXX")"
trap 'rm -rf "$staging"' EXIT
name="admin_audit_$(date -u +'%Y%m%dT%H%M%SZ')_$(basename "$staging" | cut -d. -f3)"
file="${name}.sql.gz"
{
  printf '%s\n' 'CREATE TABLE admin_action_logs_archive (id BIGINT PRIMARY KEY, admin_id BIGINT NOT NULL, action_type VARCHAR(50) NOT NULL, target_type VARCHAR(20) NOT NULL, target_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL);'
  docker exec -e MYSQL_PWD="${DB_ROOT_PASSWORD}" sugang-helper-mysql \
    mysql -u root --batch --raw --skip-column-names sugang_helper -e \
    "SELECT CONCAT('INSERT INTO admin_action_logs_archive VALUES (', id, ',', admin_id, ',', QUOTE(action_type), ',', QUOTE(target_type), ',', target_id, ',', QUOTE(created_at), ',', QUOTE(updated_at), ');') FROM admin_action_logs ORDER BY id;"
} | gzip -9 > "${staging}/${file}"
gzip -t "${staging}/${file}"
rows="$(gzip -dc "${staging}/${file}" | awk '/^INSERT INTO admin_action_logs_archive VALUES / {n++} END {print n+0}')"
printf 'schema=admin-audit-core-v1\ncreated_at_utc=%s\nrows=%s\nsource_table=admin_action_logs\n' \
  "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" "$rows" > "${staging}/${file}.manifest"
(cd "$staging" && shasum -a 256 "$file" > "${file}.sha256"
  shasum -a 256 "${file}.manifest" > "${file}.manifest.sha256")
# Publish the complete set at once; failed exports remain invisible to sync.
mv "$staging" "${archive_root}/${name}"
log_archive_completed "$rows"
