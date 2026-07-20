#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_dir="${repo_root}/infra"
compose_env="${compose_dir}/.env.example"
temporary_dir="$(mktemp -d)"
release_dir="${temporary_dir}/release"
services_file="${temporary_dir}/services"
config_file="${temporary_dir}/compose.json"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

mkdir -p "${release_dir}"
cp "${repo_root}/apps/server/.env.example" "${release_dir}/.env"

APP_ENV_FILE="${release_dir}/.env" docker compose \
  --env-file "${compose_env}" \
  -f "${compose_dir}/docker-compose.yml" \
  -f "${compose_dir}/docker-compose.override.yml" \
  config --services >"${services_file}"

expected_services=$'app\ndb\nredis'
actual_services="$(sort "${services_file}")"
if [ "${actual_services}" != "${expected_services}" ]; then
  echo "local default Compose services must be app, db, and redis" >&2
  printf 'actual services:\n%s\n' "${actual_services}" >&2
  exit 1
fi

APP_ENV_FILE="${release_dir}/.env" docker compose \
  --env-file "${compose_env}" \
  -f "${compose_dir}/docker-compose.yml" \
  -f "${compose_dir}/docker-compose.override.yml" \
  --profile migration \
  config --format json >"${config_file}"

python - "${config_file}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    compose = json.load(handle)

services = compose["services"]
if set(services) != {"app", "db", "redis", "migrate"}:
    raise SystemExit(f"local Compose must contain only app/db/redis/migrate: {sorted(services)}")

def env_map(service):
    environment = service.get("environment", {})
    if isinstance(environment, dict):
        return {str(k): str(v) for k, v in environment.items()}
    return {str(entry).split("=", 1)[0]: str(entry).split("=", 1)[1] for entry in environment}

app_env = env_map(services["app"])
if app_env.get("LOG_FILE") != "/tmp/application.log":
    raise SystemExit("local app logs must use the container temporary directory")
if app_env.get("APP_CORS_ALLOWED_ORIGINS") != "http://localhost:3000":
    raise SystemExit("local CORS must allow only localhost frontend origin")
if app_env.get("APP_CORS_REQUIRE_HTTPS") != "false":
    raise SystemExit("local CORS must allow HTTP localhost development")
if app_env.get("APP_AUTH_REFRESH_COOKIE_SECURE") != "false":
    raise SystemExit("local refresh cookies must allow HTTP localhost development")

def volume_source(service_name, target):
    for volume in services[service_name].get("volumes", []):
        if volume.get("target") == target:
            return volume.get("source")
    return None

if volume_source("db", "/var/lib/mysql") != "local_db_data":
    raise SystemExit("local DB must use its named volume")
if volume_source("app", "/app/data/uploads") != "local_app_uploads":
    raise SystemExit("local uploads must use their named volume")
if services["redis"].get("volumes"):
    raise SystemExit("local Redis must remain ephemeral")

if services["app"].get("build", {}).get("dockerfile") != "Dockerfile.local":
    raise SystemExit("local Compose must build through Dockerfile.local so one command is sufficient")

volumes = compose.get("volumes", {})
expected_volume_names = {
    "local_db_data": "sugang-helper-local-db-data",
    "local_app_uploads": "sugang-helper-local-app-uploads",
}
for name, expected_name in expected_volume_names.items():
    if name not in volumes:
        raise SystemExit(f"missing local named volume {name}")
    if volumes[name].get("name") != expected_name:
        raise SystemExit(f"local volume {name} has unexpected Docker name")

print("local minimal Compose contract passed")
PY

echo "local Compose contract passed"
