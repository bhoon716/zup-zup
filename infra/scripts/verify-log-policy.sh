#!/usr/bin/env bash
set -euo pipefail

compose_file="${1:-docker-compose.yml}"

if [ ! -f "${compose_file}" ]; then
  echo "compose file not found: ${compose_file}" >&2
  exit 1
fi

compose_directory="$(cd "$(dirname "${compose_file}")" && pwd)"
project_root="$(cd "${compose_directory}/.." && pwd)"
compose_env_file="${COMPOSE_ENV_FILE:-${compose_directory}/.env}"
app_release_dir="${APP_RELEASE_DIR:-${project_root}/apps/server}"
temporary_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${temporary_dir}"
}

trap cleanup EXIT

if [ ! -f "${compose_env_file}" ]; then
  compose_env_file="${compose_directory}/.env.example"
fi
if [ ! -f "${compose_env_file}" ]; then
  echo "Compose environment file not found" >&2
  exit 1
fi

if [ ! -f "${app_release_dir}/.env" ]; then
  if [ ! -f "${project_root}/apps/server/.env.example" ]; then
    echo "App environment file not found" >&2
    exit 1
  fi
  app_release_dir="${temporary_dir}/release"
  mkdir -p "${app_release_dir}"
  cp "${project_root}/apps/server/.env.example" "${app_release_dir}/.env"
fi

config_output="$(APP_RELEASE_DIR="${app_release_dir}" docker compose --env-file "${compose_env_file}" -f "${compose_file}" config)"

grep -F -- "--binlog-expire-logs-seconds=604800" <<<"${config_output}" >/dev/null

grep -F -- "driver: json-file" <<<"${config_output}" >/dev/null
grep -F -- "max-size: 10m" <<<"${config_output}" >/dev/null
grep -F -- "max-file: \"5\"" <<<"${config_output}" >/dev/null

echo "log policy verification passed"
