#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ] && [ "${ALLOW_NON_ROOT:-0}" != "1" ]; then
  echo "run this script as root to read Redis state safely" >&2
  exit 1
fi

docker_bin="${DOCKER_BIN:-docker}"
compose_dir="${COMPOSE_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
redis_state_dir="${REDIS_STATE_DIR:-/var/lib/jbnu-sugang-helper/redis}"
backup_root="${BACKUP_ROOT:-/var/backups/jbnu-sugang-helper/redis-state}"
stop_timeout_seconds="${REDIS_STOP_TIMEOUT_SECONDS:-30}"
health_attempts="${REDIS_HEALTH_MAX_ATTEMPTS:-30}"
health_wait_seconds="${REDIS_HEALTH_WAIT_SECONDS:-2}"
state_parent="$(dirname "${redis_state_dir}")"
state_name="$(basename "${redis_state_dir}")"
was_running=false
redis_stopped=false
staging_archive=""
staging_checksum=""

if [ ! -f "${compose_dir}/docker-compose.yml" ]; then
  echo "compose file not found: ${compose_dir}/docker-compose.yml" >&2
  exit 1
fi

if [ ! -d "${redis_state_dir}" ]; then
  echo "Redis state directory not found: ${redis_state_dir}" >&2
  exit 1
fi

compose() {
  (
    cd "${compose_dir}"
    "${docker_bin}" compose "$@"
  )
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

wait_for_redis_health() {
  local container_id
  container_id="$(compose ps -q redis)"
  if [ -z "${container_id}" ]; then
    echo "Redis container was not created" >&2
    return 1
  fi

  for _ in $(seq 1 "${health_attempts}"); do
    local health_status
    health_status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${health_status}" = "healthy" ]; then
      return 0
    fi
    if [ "${health_status}" = "exited" ] || [ "${health_status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep "${health_wait_seconds}"
  done

  echo "Redis did not become healthy after backup" >&2
  "${docker_bin}" logs "$(compose ps -q redis)" >&2 || true
  return 1
}

cleanup() {
  status=$?
  if [ "${was_running}" = true ] && [ "${redis_stopped}" = true ]; then
    compose start redis >&2 || true
    wait_for_redis_health >&2 || true
  fi
  [ -z "${staging_archive}" ] || rm -f "${staging_archive}"
  [ -z "${staging_checksum}" ] || rm -f "${staging_checksum}"
  exit "${status}"
}

trap cleanup EXIT

container_id="$(compose ps -aq redis)"
if [ -n "${container_id}" ] \
  && [ "$("${docker_bin}" inspect --format '{{.State.Running}}' "${container_id}")" = "true" ]; then
  was_running=true
  redis_stopped=true
  compose stop -t "${stop_timeout_seconds}" redis >&2
fi

umask 077
mkdir -p "${backup_root}"
chmod 0700 "${backup_root}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive="${backup_root}/redis-state-${timestamp}.tar.gz"
checksum="${archive}.sha256"

if [ -e "${archive}" ] || [ -e "${checksum}" ]; then
  echo "Redis backup path already exists: ${archive}" >&2
  exit 1
fi

staging_archive="$(mktemp "${backup_root}/.redis-state-${timestamp}.XXXXXX")"
staging_checksum="$(mktemp "${backup_root}/.redis-state-${timestamp}.sha256.XXXXXX")"
tar --numeric-owner -czf "${staging_archive}" -C "${state_parent}" "${state_name}"
printf '%s  %s\n' "$(sha256_file "${staging_archive}")" "$(basename "${archive}")" > "${staging_checksum}"
chmod 0600 "${staging_archive}" "${staging_checksum}"
mv "${staging_archive}" "${archive}"
mv "${staging_checksum}" "${checksum}"
staging_archive=""
staging_checksum=""

if [ "${was_running}" = true ]; then
  compose start redis >&2
  wait_for_redis_health >&2
  redis_stopped=false
fi

printf '%s\n' "${archive}"
