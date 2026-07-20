#!/usr/bin/env bash
set -euo pipefail

docker_bin="${DOCKER_BIN:-docker}"
compose_dir="${COMPOSE_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
health_attempts="${REDIS_SMOKE_HEALTH_MAX_ATTEMPTS:-30}"
health_wait_seconds="${REDIS_SMOKE_HEALTH_WAIT_SECONDS:-2}"
readiness_timeout_seconds="${REDIS_SMOKE_READINESS_TIMEOUT_SECONDS:-5}"
key="issue098:restart-smoke:${RANDOM}:$$"
value="redis-state-survives-restart"
redis_stopped=false

if [ ! -f "${compose_dir}/docker-compose.yml" ]; then
  echo "compose file not found: ${compose_dir}/docker-compose.yml" >&2
  exit 1
fi

if ! [[ "${readiness_timeout_seconds}" =~ ^[1-9][0-9]*$ ]]; then
  echo "REDIS_SMOKE_READINESS_TIMEOUT_SECONDS must be a positive integer" >&2
  exit 1
fi

compose() {
  (
    cd "${compose_dir}"
    "${docker_bin}" compose "$@"
  )
}

wait_for_service_health() {
  local service="$1"
  local container_id
  container_id="$(compose ps -q "${service}")"
  if [ -z "${container_id}" ]; then
    echo "${service} container was not created" >&2
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

  echo "${service} did not become healthy" >&2
  "${docker_bin}" logs "${container_id}" >&2 || true
  return 1
}

redis_cli() {
  compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --raw "$@"' redis-cli "$@"
}

cleanup() {
  status=$?
  if [ "${redis_stopped}" = true ]; then
    compose start redis >&2 || true
    if wait_for_service_health redis >&2; then
      redis_stopped=false
    fi
  fi
  redis_cli DEL "${key}" >/dev/null 2>&1 || true
  exit "${status}"
}

trap cleanup EXIT

wait_for_service_health redis
wait_for_service_health app
redis_cli SET "${key}" "${value}" EX 120 >/dev/null

redis_stopped=true
compose stop redis >&2
if compose exec -T app sh -ec 'wget -T "$1" -q -O - http://127.0.0.1:8081/actuator/health/readiness | grep -q "\"status\":\"UP\""' readiness "${readiness_timeout_seconds}"; then
  echo "app readiness stayed UP while Redis was stopped" >&2
  exit 1
fi

compose start redis >&2
wait_for_service_health redis
redis_stopped=false
wait_for_service_health app

if [ "$(redis_cli GET "${key}")" != "${value}" ]; then
  echo "Redis restart lost the smoke-test state" >&2
  exit 1
fi

echo "Redis restart and app readiness smoke passed"
