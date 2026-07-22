#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
readonly repo_root
readonly compose_file="${repo_root}/infra/docker-compose.yml"
readonly compose_env="${repo_root}/infra/.env.example"
readonly app_env="${repo_root}/apps/server/.env.example"
readonly docker_bin="${DOCKER_BIN:-docker}"
readonly project_name="issue135-redis-recovery-${RANDOM}-$$"
readonly redis_container_name="${project_name}-redis"
readonly redis_volume_name="${project_name}-data"
readonly runtime_network_name="${project_name}-runtime"
readonly redis_password="issue135-redis-password"

if [ ! -f "${compose_file}" ] || [ ! -f "${compose_env}" ] || [ ! -f "${app_env}" ]; then
  echo "Redis recovery test requires the repository Compose and environment templates" >&2
  exit 1
fi

compose() {
  APP_ENV_FILE="${app_env}" \
    REDIS_CONTAINER_NAME="${redis_container_name}" \
    REDIS_VOLUME_NAME="${redis_volume_name}" \
    RUNTIME_NETWORK_NAME="${runtime_network_name}" \
    REDIS_PASSWORD="${redis_password}" \
    "${docker_bin}" compose \
      --project-name "${project_name}" \
      --env-file "${compose_env}" \
      -f "${compose_file}" \
      "$@"
}

cleanup() {
  local status=$?
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  exit "${status}"
}
trap cleanup EXIT

wait_for_redis_health() {
  local container_id
  container_id="$(compose ps -q redis)"
  if [ -z "${container_id}" ]; then
    echo "Redis container was not created" >&2
    return 1
  fi

  for _ in $(seq 1 30); do
    local health_status
    health_status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${health_status}" = "healthy" ]; then
      return 0
    fi
    if [ "${health_status}" = "exited" ] || [ "${health_status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep 1
  done

  echo "Redis did not become healthy" >&2
  "${docker_bin}" logs "${container_id}" >&2 || true
  return 1
}

assert_isolated_runtime_network() {
  local container_id
  local attached_networks
  container_id="$(compose ps -q redis)"
  attached_networks="$("${docker_bin}" inspect --format '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' "${container_id}")"
  if ! printf '%s\n' "${attached_networks}" | grep -Fxq "${runtime_network_name}"; then
    echo "Redis recovery test must use its isolated runtime network" >&2
    return 1
  fi
}

redis_cli() {
  compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --raw "$@"' redis-cli "$@"
}

assert_value() {
  local key="$1"
  local expected="$2"
  local actual
  actual="$(redis_cli GET "${key}")"
  if [ "${actual}" != "${expected}" ]; then
    echo "Redis key ${key} was not preserved after recreation" >&2
    return 1
  fi
}

compose up -d redis
wait_for_redis_health
assert_isolated_runtime_network

persistence_info="$(redis_cli INFO persistence)"
if ! printf '%s\n' "${persistence_info}" | tr -d '\r' | grep -Fxq 'aof_enabled:1'; then
  echo "Redis AOF is not enabled" >&2
  printf '%s\n' "${persistence_info}" >&2
  exit 1
fi
redis_cli SET "BL:issue135:logout" logout EX 3600 >/dev/null
redis_cli SET "RT:issue135:refresh" 'v2:family:digest' EX 1209600 >/dev/null
redis_cli SET "spring:session:sessions:issue135" authenticated-session EX 3600 >/dev/null

compose stop redis >/dev/null
compose rm -f redis >/dev/null
compose up -d redis
wait_for_redis_health

assert_value "BL:issue135:logout" logout
assert_value "RT:issue135:refresh" 'v2:family:digest'
assert_value "spring:session:sessions:issue135" authenticated-session

echo "Redis logout, refresh registry, and session state survive container recreation"
