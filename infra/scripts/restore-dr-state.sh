#!/usr/bin/env bash
set -euo pipefail
umask 077

if [ "$#" -ne 1 ]; then
  echo "usage: $0 <encrypted-dr-archive>" >&2
  exit 1
fi

docker_bin="${DOCKER_BIN:-docker}"
compose_dir="${COMPOSE_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
operation_lock_dir="${OPERATION_LOCK_DIR:-${compose_dir}/.operation-lock}"
uploads_dir="${SOURCE_UPLOADS_DIR:-/var/lib/jbnu-sugang-helper/uploads}"
grafana_dir="${SOURCE_GRAFANA_DIR:-/var/lib/jbnu-sugang-helper/grafana}"
npm_data_dir="${SOURCE_NPM_DATA_DIR:-${compose_dir}/nginx-proxy-manager/data}"
npm_letsencrypt_dir="${SOURCE_NPM_LETSENCRYPT_DIR:-${compose_dir}/nginx-proxy-manager/letsencrypt}"
encryption_password_file="${BACKUP_ENCRYPTION_PASSWORD_FILE:-/etc/jbnu-sugang-helper/backup-passphrase}"
authentication_key_file="${BACKUP_AUTHENTICATION_KEY_FILE:-/etc/jbnu-sugang-helper/backup-authentication-key}"
health_attempts="${DB_HEALTH_MAX_ATTEMPTS:-30}"
health_wait_seconds="${DB_HEALTH_WAIT_SECONDS:-2}"
archive="$1"
checksum_file="${archive}.sha256"
hmac_file="${archive}.hmac"
staging_dir="$(mktemp -d)"
plain_archive="${staging_dir}/archive.tar.gz"
extracted_dir="${staging_dir}/extracted"
pre_restore_dump="${staging_dir}/pre-restore.sql"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
stopped_services=()
restored_destinations=()
previous_destinations=()
database_replaced=false
restore_completed=false
database_name=""
manifest_database=""
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

validate_requested_state_paths() {
  local test_root path
  if [ "${ALLOW_NON_ROOT:-0}" = "1" ]; then
    test_root="${DR_TEST_STATE_ROOT:-}"
    if [ -z "${test_root}" ] || [ ! -d "${test_root}" ]; then
      echo "DR_TEST_STATE_ROOT must be an existing directory when ALLOW_NON_ROOT=1" >&2
      return 1
    fi
    for path in "${uploads_dir}" "${grafana_dir}" "${npm_data_dir}" "${npm_letsencrypt_dir}"; do
      case "${path}" in
        "${test_root}"/*) ;;
        *)
          echo "test restore destination is outside DR_TEST_STATE_ROOT: ${path}" >&2
          return 1
          ;;
      esac
    done
    return 0
  fi

  if [ "${uploads_dir}" != "/var/lib/jbnu-sugang-helper/uploads" ] \
    || [ "${grafana_dir}" != "/var/lib/jbnu-sugang-helper/grafana" ] \
    || [ "${npm_data_dir}" != "${compose_dir}/nginx-proxy-manager/data" ] \
    || [ "${npm_letsencrypt_dir}" != "${compose_dir}/nginx-proxy-manager/letsencrypt" ]; then
    echo "restore destinations must use the reviewed production durable-state paths" >&2
    return 1
  fi
}

prepare_restore_destinations() {
  if [ "${ALLOW_NON_ROOT:-0}" = "1" ]; then
    mkdir -p "${uploads_dir}" "${grafana_dir}" "${npm_data_dir}" "${npm_letsencrypt_dir}"
    return
  fi

  local preparer="${compose_dir}/scripts/prepare-app-host-directories.sh"
  if [ ! -x "${preparer}" ]; then
    echo "host-directory preparer is missing or not executable: ${preparer}" >&2
    return 1
  fi
  "${preparer}"
}

validate_prepared_state_path() {
  local label="$1"
  local path="$2"
  local actual expected test_root
  actual="$(canonical_directory "${path}")" || return 1

  if [ "${ALLOW_NON_ROOT:-0}" = "1" ]; then
    test_root="$(canonical_directory "${DR_TEST_STATE_ROOT}")" || return 1
    case "${actual}" in
      "${test_root}"/*) return 0 ;;
      *)
        echo "resolved test restore destination escaped DR_TEST_STATE_ROOT: ${actual}" >&2
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
    echo "resolved ${label} directory is not the reviewed production path: ${actual}" >&2
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

verify_checksum() {
  if [ ! -f "${archive}" ] || [ ! -f "${checksum_file}" ]; then
    echo "encrypted archive or checksum file not found" >&2
    return 1
  fi
  local expected actual
  expected="$(awk 'NR == 1 {print $1}' "${checksum_file}")"
  if [[ ! "${expected}" =~ ^[A-Fa-f0-9]{64}$ ]]; then
    echo "checksum file does not contain a SHA-256 digest" >&2
    return 1
  fi
  actual="$(sha256sum "${archive}" | awk '{print $1}')"
  if [ "${expected}" != "${actual}" ]; then
    echo "encrypted archive checksum verification failed" >&2
    return 1
  fi
}

archive_hmac() {
  openssl dgst -sha256 -mac HMAC \
    -macopt "key:file:${authentication_key_file}" \
    -r "$1" | awk 'NR == 1 {print $1}'
}

verify_hmac() {
  if [ ! -f "${hmac_file}" ]; then
    echo "encrypted archive HMAC file not found" >&2
    return 1
  fi
  local expected actual
  expected="$(awk 'NR == 1 {print $1}' "${hmac_file}")"
  if [[ ! "${expected}" =~ ^[A-Fa-f0-9]{64}$ ]]; then
    echo "HMAC file does not contain a SHA-256 digest" >&2
    return 1
  fi
  actual="$(archive_hmac "${archive}")"
  if [ "${expected}" != "${actual}" ]; then
    echo "encrypted archive HMAC verification failed" >&2
    return 1
  fi
}

read_database_name() {
  compose exec -T db sh -ceu 'printf %s "$MYSQL_DATABASE"'
}

validate_archive_contents() {
  python3 - "${plain_archive}" <<'PY'
import re
import stat
import sys
import tarfile
from pathlib import PurePosixPath

archive_path = sys.argv[1]
exact_paths = {
    "manifest.env",
    "database",
    "database/sugang_helper.sql",
    "database/binlogs",
    "database/binlog-names.txt",
    "uploads",
    "grafana",
    "nginx-proxy-manager",
    "nginx-proxy-manager/data",
    "nginx-proxy-manager/letsencrypt",
}
prefix_paths = (
    "uploads/",
    "grafana/",
    "nginx-proxy-manager/data/",
    "nginx-proxy-manager/letsencrypt/",
)
binlog_pattern = re.compile(r"database/binlogs/mysql-bin\.[0-9]+$")


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(1)


def is_allowed_path(name: str) -> bool:
    return (
        name in exact_paths
        or name.startswith(prefix_paths)
        or bool(binlog_pattern.fullmatch(name))
    )


try:
    with tarfile.open(archive_path, "r:gz") as archive:
        members = archive.getmembers()
except (OSError, tarfile.TarError) as error:
    fail(f"archive cannot be read safely: {error}")

has_manifest = False
has_dump = False
for member in members:
    name = member.name.rstrip("/")
    path = PurePosixPath(name)
    if not name or path.is_absolute() or ".." in path.parts:
        fail(f"archive contains an unsafe path: {member.name}")
    if not (member.isdir() or member.isfile()):
        fail(f"archive contains an unsupported tar entry type: {member.name}")
    if member.mode & (stat.S_ISUID | stat.S_ISGID):
        fail(f"archive contains a privileged file mode: {member.name}")
    if not is_allowed_path(name):
        fail(f"archive contains an unexpected path: {member.name}")
    has_manifest = has_manifest or name == "manifest.env"
    has_dump = has_dump or name == "database/sugang_helper.sql"

if not has_manifest or not has_dump:
    fail("archive is missing its manifest or database dump")
PY
}

validate_manifest() {
  local format=""
  local created_at=""
  local consistency=""
  local binlog_retention=""
  local archive_retention=""
  local key value
  local seen_format=false
  local seen_created_at=false
  local seen_database=false
  local seen_consistency=false
  local seen_binlog_retention=false
  local seen_archive_retention=false

  while IFS='=' read -r key value; do
    case "${key}" in
      DR_FORMAT_VERSION)
        [ "${seen_format}" = false ] || { echo "manifest contains a duplicate key" >&2; return 1; }
        seen_format=true
        format="${value}"
        ;;
      CREATED_AT_UTC)
        [ "${seen_created_at}" = false ] || { echo "manifest contains a duplicate key" >&2; return 1; }
        seen_created_at=true
        created_at="${value}"
        ;;
      DATABASE)
        [ "${seen_database}" = false ] || { echo "manifest contains a duplicate key" >&2; return 1; }
        seen_database=true
        manifest_database="${value}"
        ;;
      CONSISTENCY)
        [ "${seen_consistency}" = false ] || { echo "manifest contains a duplicate key" >&2; return 1; }
        seen_consistency=true
        consistency="${value}"
        ;;
      BINLOG_RETENTION_DAYS)
        [ "${seen_binlog_retention}" = false ] || { echo "manifest contains a duplicate key" >&2; return 1; }
        seen_binlog_retention=true
        binlog_retention="${value}"
        ;;
      ARCHIVE_RETENTION_DAYS)
        [ "${seen_archive_retention}" = false ] || { echo "manifest contains a duplicate key" >&2; return 1; }
        seen_archive_retention=true
        archive_retention="${value}"
        ;;
      *)
        echo "manifest contains an unexpected key: ${key}" >&2
        return 1
        ;;
    esac
  done < "${extracted_dir}/manifest.env"

  if [ "${format}" != "1" ] \
    || [[ ! "${created_at}" =~ ^[0-9]{8}T[0-9]{6}Z$ ]] \
    || [[ ! "${manifest_database}" =~ ^[A-Za-z0-9_]+$ ]] \
    || [ "${consistency}" != "writers-stopped-logical-dump" ] \
    || [[ ! "${binlog_retention}" =~ ^[0-9]+$ ]] \
    || [[ ! "${archive_retention}" =~ ^[0-9]+$ ]]; then
    echo "manifest format or recovery policy is invalid" >&2
    return 1
  fi
}

backup_current_database() {
  compose exec -T db sh -ceu '
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
    mysqldump --protocol=socket --user=root \
      --single-transaction --no-tablespaces --routines --events --triggers "$MYSQL_DATABASE"
  ' > "${pre_restore_dump}"
}

replace_database() {
  compose exec -T db sh -ceu '
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
    mysql --protocol=socket --user=root -e "DROP DATABASE IF EXISTS \`$1\`; CREATE DATABASE \`$1\`;"
  ' sh "${database_name}"
  database_replaced=true
  compose exec -T db sh -ceu '
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
    mysql --protocol=socket --user=root "$MYSQL_DATABASE"
  ' < "${extracted_dir}/database/sugang_helper.sql"
  compose exec -T db /docker-entrypoint-initdb.d/01-provision-service-accounts.sh
}

rollback_database() {
  if [ "${database_replaced}" != true ] || [ ! -s "${pre_restore_dump}" ]; then
    return 0
  fi
  compose exec -T db sh -ceu '
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
    mysql --protocol=socket --user=root -e "DROP DATABASE IF EXISTS \`$1\`; CREATE DATABASE \`$1\`;"
  ' sh "${database_name}" >/dev/null 2>&1 || return 1
  compose exec -T db sh -ceu '
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
    mysql --protocol=socket --user=root "$MYSQL_DATABASE"
  ' < "${pre_restore_dump}" >/dev/null 2>&1 || return 1
  compose exec -T db /docker-entrypoint-initdb.d/01-provision-service-accounts.sh >/dev/null 2>&1 || return 1
}

swap_state_directory() {
  local source="$1"
  local destination="$2"
  local parent base staged previous
  parent="$(dirname "${destination}")"
  base="$(basename "${destination}")"
  mkdir -p "${parent}"
  staged="$(mktemp -d "${parent}/.${base}.restore.XXXXXX")"
  cp -a "${source}/." "${staged}/"
  previous="${destination}.before-dr-${timestamp}"
  if [ -e "${destination}" ] || [ -L "${destination}" ]; then
    mv "${destination}" "${previous}"
  else
    previous=""
  fi
  mv "${staged}" "${destination}"
  restored_destinations+=("${destination}")
  previous_destinations+=("${previous}")
}

rollback_state_directories() {
  local index
  for ((index=${#restored_destinations[@]} - 1; index>=0; index--)); do
    local destination="${restored_destinations[index]}"
    local previous="${previous_destinations[index]}"
    rm -rf "${destination}" || return 1
    if [ -n "${previous}" ] && [ -e "${previous}" ]; then
      mv "${previous}" "${destination}" || return 1
    fi
  done
  return 0
}

cleanup() {
  status=$?
  local restart_writers=true
  if [ "${restore_completed}" != true ]; then
    if ! rollback_state_directories; then
      echo "FATAL: host-state rollback failed; writer services will remain stopped" >&2
      restart_writers=false
    fi
    if ! rollback_database; then
      echo "FATAL: database rollback failed; writer services will remain stopped" >&2
      restart_writers=false
    fi
  fi
  if [ "${restart_writers}" = true ]; then
    restart_stopped_services >/dev/null 2>&1 || true
  fi
  release_operation_lock
  rm -rf "${staging_dir}"
  exit "${status}"
}

trap cleanup EXIT

require_root
if [ "${CONFIRM_DR_RESTORE:-}" != "RESTORE" ]; then
  echo "set CONFIRM_DR_RESTORE=RESTORE to replace the database and durable state" >&2
  exit 1
fi
validate_protected_secret_file "BACKUP_ENCRYPTION_PASSWORD_FILE" "${encryption_password_file}"
validate_protected_secret_file "BACKUP_AUTHENTICATION_KEY_FILE" "${authentication_key_file}"
validate_numeric "DB_HEALTH_MAX_ATTEMPTS" "${health_attempts}"
validate_numeric "DB_HEALTH_WAIT_SECONDS" "${health_wait_seconds}"
validate_requested_state_paths
acquire_operation_lock

verify_checksum
verify_hmac
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 \
  -pass "file:${encryption_password_file}" -in "${archive}" -out "${plain_archive}"
validate_archive_contents
mkdir -p "${extracted_dir}"
tar -xzf "${plain_archive}" -C "${extracted_dir}"
validate_manifest

compose up -d db
wait_for_db_health
database_name="$(read_database_name)"
if [[ ! "${database_name}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "DB container reported an invalid MYSQL_DATABASE" >&2
  exit 1
fi
if [ "${manifest_database}" != "${database_name}" ]; then
  echo "archive database ${manifest_database} does not match configured database ${database_name}" >&2
  exit 1
fi

prepare_restore_destinations
validate_prepared_state_path uploads "${uploads_dir}"
validate_prepared_state_path grafana "${grafana_dir}"
validate_prepared_state_path npm-data "${npm_data_dir}"
validate_prepared_state_path npm-letsencrypt "${npm_letsencrypt_dir}"

stop_writer_services
backup_current_database
replace_database
swap_state_directory "${extracted_dir}/uploads" "${uploads_dir}"
swap_state_directory "${extracted_dir}/grafana" "${grafana_dir}"
swap_state_directory "${extracted_dir}/nginx-proxy-manager/data" "${npm_data_dir}"
swap_state_directory "${extracted_dir}/nginx-proxy-manager/letsencrypt" "${npm_letsencrypt_dir}"
restore_completed=true
restart_stopped_services

echo "DR restore completed. Previous state directories remain beside their restored paths with .before-dr-${timestamp}."
