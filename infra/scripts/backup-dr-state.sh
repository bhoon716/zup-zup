#!/usr/bin/env bash
set -euo pipefail
umask 077

docker_bin="${DOCKER_BIN:-docker}"
compose_dir="${COMPOSE_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
operation_lock_dir="${OPERATION_LOCK_DIR:-${compose_dir}/.operation-lock}"
uploads_dir="${SOURCE_UPLOADS_DIR:-/var/lib/jbnu-sugang-helper/uploads}"
grafana_dir="${SOURCE_GRAFANA_DIR:-/var/lib/jbnu-sugang-helper/grafana}"
npm_data_dir="${SOURCE_NPM_DATA_DIR:-${compose_dir}/nginx-proxy-manager/data}"
npm_letsencrypt_dir="${SOURCE_NPM_LETSENCRYPT_DIR:-${compose_dir}/nginx-proxy-manager/letsencrypt}"
backup_root="${BACKUP_ROOT:-/var/backups/jbnu-sugang-helper/dr-state}"
encryption_password_file="${BACKUP_ENCRYPTION_PASSWORD_FILE:-/etc/jbnu-sugang-helper/backup-passphrase}"
authentication_key_file="${BACKUP_AUTHENTICATION_KEY_FILE:-/etc/jbnu-sugang-helper/backup-authentication-key}"
retention_days="${DR_BACKUP_RETENTION_DAYS:-14}"
health_attempts="${DB_HEALTH_MAX_ATTEMPTS:-30}"
health_wait_seconds="${DB_HEALTH_WAIT_SECONDS:-2}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
staging_dir="$(mktemp -d)"
stopped_services=()
database_name=""
operation_lock_acquired=false

require_root() {
  if [ "$(id -u)" -ne 0 ] && [ "${ALLOW_NON_ROOT:-0}" != "1" ]; then
    echo "run this script as root" >&2
    exit 1
  fi
}

file_mode() {
  stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1"
}

file_owner() {
  stat -c '%u' "$1" 2>/dev/null || stat -f '%u' "$1"
}

validate_protected_secret_file() {
  local variable_name="$1"
  local secret_file="$2"
  if [ ! -s "${secret_file}" ] || [ ! -r "${secret_file}" ]; then
    echo "${variable_name} must be a readable, non-empty file" >&2
    exit 1
  fi

  if [ "${ALLOW_NON_ROOT:-0}" != "1" ]; then
    local owner mode
    owner="$(file_owner "${secret_file}")"
    mode="$(file_mode "${secret_file}")"
    if [ "${owner}" != "0" ] || { [ "${mode}" != "400" ] && [ "${mode}" != "600" ]; }; then
      echo "${variable_name} must be root-owned and mode 0400 or 0600" >&2
      exit 1
    fi
  fi
}

validate_numeric() {
  local name="$1"
  local value="$2"
  if [[ ! "${value}" =~ ^[0-9]+$ ]]; then
    echo "${name} must be a non-negative integer" >&2
    exit 1
  fi
}

canonical_directory() {
  local path="$1"
  if [ ! -d "${path}" ]; then
    echo "required state directory is missing: ${path}" >&2
    return 1
  fi
  (
    cd "${path}"
    pwd -P
  )
}

validate_state_path() {
  local label="$1"
  local path="$2"
  local actual expected test_root
  actual="$(canonical_directory "${path}")" || return 1

  if [ "${ALLOW_NON_ROOT:-0}" = "1" ]; then
    test_root="${DR_TEST_STATE_ROOT:-}"
    if [ -z "${test_root}" ]; then
      echo "DR_TEST_STATE_ROOT is required when ALLOW_NON_ROOT=1" >&2
      return 1
    fi
    test_root="$(canonical_directory "${test_root}")" || return 1
    case "${actual}" in
      "${test_root}"/*) return 0 ;;
      *)
        echo "test state directory is outside DR_TEST_STATE_ROOT: ${actual}" >&2
        return 1
        ;;
    esac
  fi

  case "${label}" in
    uploads) expected="/var/lib/jbnu-sugang-helper/uploads" ;;
    grafana) expected="/var/lib/jbnu-sugang-helper/grafana" ;;
    npm-data) expected="$(canonical_directory "${compose_dir}/nginx-proxy-manager/data")" || return 1 ;;
    npm-letsencrypt) expected="$(canonical_directory "${compose_dir}/nginx-proxy-manager/letsencrypt")" || return 1 ;;
    *)
      echo "unknown durable-state label: ${label}" >&2
      return 1
      ;;
  esac

  if [ "${actual}" != "${expected}" ]; then
    echo "refusing unexpected ${label} directory: ${actual}" >&2
    return 1
  fi
}

compose() {
  (
    cd "${compose_dir}"
    "${docker_bin}" compose "$@"
  )
}

acquire_operation_lock() {
  if mkdir "${operation_lock_dir}" 2>/dev/null; then
    operation_lock_acquired=true
    return 0
  fi
  echo "another deployment, backup, or restore operation holds ${operation_lock_dir}" >&2
  return 1
}

release_operation_lock() {
  if [ "${operation_lock_acquired}" = true ]; then
    rmdir "${operation_lock_dir}" >/dev/null 2>&1 || true
    operation_lock_acquired=false
  fi
}

wait_for_db_health() {
  local container_id
  container_id="$(compose ps -q db)"
  if [ -z "${container_id}" ]; then
    echo "DB container was not created" >&2
    return 1
  fi

  for _ in $(seq 1 "${health_attempts}"); do
    local status
    status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${status}" = "healthy" ]; then
      return 0
    fi
    if [ "${status}" = "exited" ] || [ "${status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep "${health_wait_seconds}"
  done

  echo "DB container did not become healthy" >&2
  return 1
}

stop_writer_services() {
  local service
  for service in app grafana nginx-proxy-manager; do
    if [ -n "$(compose ps -q "${service}")" ]; then
      compose stop "${service}"
      stopped_services+=("${service}")
    fi
  done
}

restart_stopped_services() {
  if [ "${#stopped_services[@]}" -gt 0 ]; then
    compose start "${stopped_services[@]}"
    stopped_services=()
  fi
}

copy_directory() {
  local source="$1"
  local destination="$2"
  mkdir -p "${destination}"
  cp -a "${source}/." "${destination}/"
}

read_database_name() {
  compose exec -T db sh -ceu 'printf %s "$MYSQL_DATABASE"'
}

backup_mysql() {
  local db_container
  db_container="$(compose ps -q db)"
  if [ -z "${db_container}" ]; then
    echo "DB container is unavailable for backup" >&2
    return 1
  fi

  compose exec -T db sh -ceu '
    export MYSQL_PWD="$DB_BACKUP_PASSWORD"
    mysqldump --protocol=socket --user="$DB_BACKUP_USER" \
      --single-transaction --source-data=2 --no-tablespaces --routines --events --triggers \
      "$MYSQL_DATABASE"
  ' > "${staging_dir}/database/sugang_helper.sql"

  compose exec -T db sh -ceu '
    export MYSQL_PWD="$DB_BACKUP_PASSWORD"
    mysql --protocol=socket --user="$DB_BACKUP_USER" -e "FLUSH BINARY LOGS"
    mysql --protocol=socket --user="$DB_BACKUP_USER" -Nse "SHOW BINARY LOGS"
  ' | awk '{print $1}' > "${staging_dir}/database/binlog-names.txt"

  while IFS= read -r binlog; do
    if [[ ! "${binlog}" =~ ^mysql-bin\.[0-9]+$ ]]; then
      echo "refusing unexpected MySQL binary log name: ${binlog}" >&2
      return 1
    fi
    "${docker_bin}" cp "${db_container}:/var/lib/mysql/${binlog}" "${staging_dir}/database/binlogs/${binlog}"
  done < "${staging_dir}/database/binlog-names.txt"
}

archive_hmac() {
  openssl dgst -sha256 -mac HMAC \
    -macopt "key:file:${authentication_key_file}" \
    -r "$1" | awk 'NR == 1 {print $1}'
}

prune_expired_archives() {
  find "${backup_root}" -maxdepth 1 -type f -name 'dr-state-*.tar.gz.enc' -mtime "+${retention_days}" -print0 \
    | while IFS= read -r -d '' expired_archive; do
        rm -f "${expired_archive}" "${expired_archive}.sha256" "${expired_archive}.hmac"
      done
}

cleanup() {
  status=$?
  restart_stopped_services >/dev/null 2>&1 || true
  release_operation_lock
  rm -rf "${staging_dir}"
  exit "${status}"
}

trap cleanup EXIT

require_root
validate_protected_secret_file "BACKUP_ENCRYPTION_PASSWORD_FILE" "${encryption_password_file}"
validate_protected_secret_file "BACKUP_AUTHENTICATION_KEY_FILE" "${authentication_key_file}"
validate_numeric "DR_BACKUP_RETENTION_DAYS" "${retention_days}"
validate_numeric "DB_HEALTH_MAX_ATTEMPTS" "${health_attempts}"
validate_numeric "DB_HEALTH_WAIT_SECONDS" "${health_wait_seconds}"
validate_state_path uploads "${uploads_dir}"
validate_state_path grafana "${grafana_dir}"
validate_state_path npm-data "${npm_data_dir}"
validate_state_path npm-letsencrypt "${npm_letsencrypt_dir}"
acquire_operation_lock

install -d -m 0700 "${backup_root}"
mkdir -p "${staging_dir}/database/binlogs" \
  "${staging_dir}/uploads" \
  "${staging_dir}/grafana" \
  "${staging_dir}/nginx-proxy-manager/data" \
  "${staging_dir}/nginx-proxy-manager/letsencrypt"

compose up -d db
wait_for_db_health
database_name="$(read_database_name)"
if [[ ! "${database_name}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "DB container reported an invalid MYSQL_DATABASE" >&2
  exit 1
fi
stop_writer_services
backup_mysql
copy_directory "${uploads_dir}" "${staging_dir}/uploads"
copy_directory "${grafana_dir}" "${staging_dir}/grafana"
copy_directory "${npm_data_dir}" "${staging_dir}/nginx-proxy-manager/data"
copy_directory "${npm_letsencrypt_dir}" "${staging_dir}/nginx-proxy-manager/letsencrypt"

cat > "${staging_dir}/manifest.env" <<EOF
DR_FORMAT_VERSION=1
CREATED_AT_UTC=${timestamp}
DATABASE=${database_name}
CONSISTENCY=writers-stopped-logical-dump
BINLOG_RETENTION_DAYS=7
ARCHIVE_RETENTION_DAYS=${retention_days}
EOF

archive="${backup_root}/dr-state-${timestamp}.tar.gz.enc"
archive_tmp="$(mktemp "${backup_root}/.dr-state-${timestamp}.XXXXXX")"
checksum_tmp="$(mktemp "${backup_root}/.dr-state-${timestamp}.sha256.XXXXXX")"
hmac_tmp="$(mktemp "${backup_root}/.dr-state-${timestamp}.hmac.XXXXXX")"

COPYFILE_DISABLE=1 tar -C "${staging_dir}" -czf - \
  manifest.env database uploads grafana nginx-proxy-manager \
  | openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 -salt \
    -pass "file:${encryption_password_file}" -out "${archive_tmp}"
mv "${archive_tmp}" "${archive}"
chmod 0600 "${archive}"
printf '%s  %s\n' "$(sha256sum "${archive}" | awk '{print $1}')" "$(basename "${archive}")" > "${checksum_tmp}"
mv "${checksum_tmp}" "${archive}.sha256"
chmod 0600 "${archive}.sha256"
printf '%s  %s\n' "$(archive_hmac "${archive}")" "$(basename "${archive}")" > "${hmac_tmp}"
mv "${hmac_tmp}" "${archive}.hmac"
chmod 0600 "${archive}.hmac"
prune_expired_archives
restart_stopped_services

printf '%s\n' "${archive}"
