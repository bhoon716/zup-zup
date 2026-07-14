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
config_json="${temporary_dir}/compose-config.json"
APP_RELEASE_DIR="${app_release_dir}" \
  docker compose --env-file "${compose_env_file}" -f "${compose_file}" config --format json >"${config_json}"

grep -F -- "--binlog-expire-logs-seconds=604800" <<<"${config_output}" >/dev/null

grep -F -- "driver: json-file" <<<"${config_output}" >/dev/null
grep -F -- "max-size: 10m" <<<"${config_output}" >/dev/null
grep -F -- "max-file: \"5\"" <<<"${config_output}" >/dev/null

python - "${config_json}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    services = json.load(handle).get("services", {})

long_running_services = (
    "db",
    "redis",
    "prometheus",
    "alertmanager",
    "grafana",
    "nginx-proxy-manager",
    "loki",
    "promtail",
    "app",
)
expected_logging = {
    "driver": "json-file",
    "options": {"max-size": "10m", "max-file": "5"},
}

for service_name in long_running_services:
    if services.get(service_name, {}).get("logging") != expected_logging:
        raise SystemExit(f"{service_name} must use the bounded json-file log policy")
PY

echo "log policy verification passed"
