#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ] && [ "${ALLOW_NON_ROOT:-0}" != "1" ]; then
  echo "run this script as root to replace Redis state safely" >&2
  exit 1
fi

if [ "${CONFIRM_REDIS_RESTORE:-}" != "RESTORE" ]; then
  echo "set CONFIRM_REDIS_RESTORE=RESTORE to replace the current Redis state" >&2
  exit 1
fi

if [ "$#" -ne 1 ]; then
  echo "usage: $0 <redis-state-archive>" >&2
  exit 1
fi

archive="$1"
checksum="${archive}.sha256"
docker_bin="${DOCKER_BIN:-docker}"
compose_dir="${COMPOSE_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
redis_state_dir="${REDIS_STATE_DIR:-/var/lib/jbnu-sugang-helper/redis}"
health_attempts="${REDIS_HEALTH_MAX_ATTEMPTS:-30}"
health_wait_seconds="${REDIS_HEALTH_WAIT_SECONDS:-2}"
state_parent="$(dirname "${redis_state_dir}")"
state_name="$(basename "${redis_state_dir}")"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
staging_dir=""
previous_state_dir=""
state_swapped=false
restore_succeeded=false
was_running=false
redis_stopped=false

if [ ! -f "${compose_dir}/docker-compose.yml" ]; then
  echo "compose file not found: ${compose_dir}/docker-compose.yml" >&2
  exit 1
fi

if [ ! -f "${archive}" ] || [ ! -f "${checksum}" ]; then
  echo "Redis archive or checksum is missing" >&2
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

  echo "Restored Redis did not become healthy" >&2
  "${docker_bin}" logs "$(compose ps -q redis)" >&2 || true
  return 1
}

rollback_state() {
  if [ "${state_swapped}" != true ] || [ -z "${previous_state_dir}" ] || [ ! -d "${previous_state_dir}" ]; then
    return
  fi

  echo "Restored Redis failed; returning to the previous state" >&2
  compose stop redis >&2 || true
  if [ -d "${redis_state_dir}" ]; then
    mv "${redis_state_dir}" "${state_parent}/${state_name}.failed-restore-${timestamp}" >&2 || true
  fi
  mv "${previous_state_dir}" "${redis_state_dir}" >&2 || true
  compose up -d --no-deps redis >&2 || true
  if wait_for_redis_health >&2; then
    redis_stopped=false
  fi
}

cleanup() {
  status=$?
  if [ "${restore_succeeded}" != true ]; then
    rollback_state
  fi
  if [ "${was_running}" = true ] && [ "${redis_stopped}" = true ]; then
    compose start redis >&2 || true
    if wait_for_redis_health >&2; then
      redis_stopped=false
    fi
  fi
  [ -z "${staging_dir}" ] || rm -rf "${staging_dir}"
  exit "${status}"
}

trap cleanup EXIT

expected_checksum="$(awk 'NR == 1 {print $1}' "${checksum}")"
actual_checksum="$(sha256_file "${archive}")"
if ! [[ "${expected_checksum}" =~ ^[0-9a-fA-F]{64}$ ]] || [ "${expected_checksum}" != "${actual_checksum}" ]; then
  echo "Redis archive checksum does not match" >&2
  exit 1
fi

has_state_root=false
while IFS= read -r entry; do
  if [ "${entry}" != "${state_name}" ] && [[ "${entry}" != "${state_name}/"* ]]; then
    echo "Redis archive contains an unexpected path: ${entry}" >&2
    exit 1
  fi
  if [[ "${entry}" = *"/../"* ]] || [[ "${entry}" = "../"* ]] || [[ "${entry}" = */.. ]]; then
    echo "Redis archive contains an unsafe path: ${entry}" >&2
    exit 1
  fi
  has_state_root=true
done < <(tar -tzf "${archive}")

if [ "${has_state_root}" != true ]; then
  echo "Redis archive does not contain a Redis state directory" >&2
  exit 1
fi

mkdir -p "${state_parent}"
staging_dir="$(mktemp -d "${state_parent}/.redis-restore.XXXXXX")"
tar -xzf "${archive}" -C "${staging_dir}"
if [ ! -d "${staging_dir}/${state_name}" ]; then
  echo "Redis archive did not extract its state directory" >&2
  exit 1
fi

container_id="$(compose ps -aq redis)"
if [ -n "${container_id}" ] \
  && [ "$("${docker_bin}" inspect --format '{{.State.Running}}' "${container_id}")" = "true" ]; then
  was_running=true
  redis_stopped=true
  compose stop redis >&2
fi

if [ -e "${redis_state_dir}" ]; then
  previous_state_dir="${state_parent}/${state_name}.before-restore-${timestamp}"
  mv "${redis_state_dir}" "${previous_state_dir}"
fi
state_swapped=true
mv "${staging_dir}/${state_name}" "${redis_state_dir}"
if [ "$(id -u)" -eq 0 ]; then
  chown 999:1000 "${redis_state_dir}"
fi
chmod 0700 "${redis_state_dir}"

compose up -d --no-deps redis >&2
wait_for_redis_health >&2
redis_stopped=false
restore_succeeded=true

printf 'Redis state restored from %s\n' "${archive}"
if [ -n "${previous_state_dir}" ]; then
  printf 'Previous state retained at %s for manual cleanup after validation\n' "${previous_state_dir}"
fi
