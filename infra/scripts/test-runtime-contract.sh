#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_dir="${repo_root}/infra"
compose_file="${compose_dir}/docker-compose.yml"
compose_env="${compose_dir}/.env.example"
temporary_dir="$(mktemp -d)"
release_dir="${temporary_dir}/release"
config_file="${temporary_dir}/compose.json"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

mkdir -p "${release_dir}"
cp "${repo_root}/apps/server/.env.example" "${release_dir}/.env"

APP_ENV_FILE="${release_dir}/.env" \
  docker compose \
    --env-file "${compose_env}" \
    -f "${compose_file}" \
    --profile migration \
    config --format json >"${config_file}"

python - "${config_file}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    compose = json.load(handle)

services = compose.get("services", {})
if set(services) != {"app", "db", "redis", "migrate"}:
    raise SystemExit(f"runtime compose must contain only app/db/redis/migrate: {sorted(services)}")

def fail(message):
    raise SystemExit(message)

def env_map(service):
    environment = service.get("environment", {})
    if isinstance(environment, dict):
        return {str(k): str(v) for k, v in environment.items()}
    result = {}
    for entry in environment:
        key, separator, value = str(entry).partition("=")
        result[key] = value if separator else ""
    return result

for name in ("app", "db", "redis"):
    if services[name].get("restart") != "unless-stopped":
        fail(f"{name} must use restart: unless-stopped")

for name in ("app", "db", "redis"):
    logging = services[name].get("logging", {})
    if logging.get("driver") != "json-file":
        fail(f"{name} must use json-file logging")
    if logging.get("options") != {"max-size": "10m", "max-file": "5"}:
        fail(f"{name} must use bounded json-file logging")

for name in ("db", "redis"):
    if services[name].get("ports"):
        fail(f"{name} must not publish a host port")

app_ports = services["app"].get("ports", [])
expected_ports = {("127.0.0.1", "8080", 8080), ("127.0.0.1", "8081", 8081)}
actual_ports = {
    (str(port.get("host_ip")), str(port.get("published")), int(port.get("target")))
    for port in app_ports
}
if actual_ports != expected_ports:
    fail(f"app must publish only localhost app/management ports: {sorted(actual_ports)}")

if services["app"].get("depends_on", {}).get("db", {}).get("condition") != "service_healthy":
    fail("app must wait for a healthy MySQL service")
if services["app"].get("depends_on", {}).get("redis", {}).get("condition") != "service_healthy":
    fail("app must wait for a healthy Redis service")

db_environment = env_map(services["db"])
for key in ("MYSQL_ROOT_PASSWORD", "MYSQL_DATABASE", "DB_RUNTIME_USER", "DB_RUNTIME_PASSWORD"):
    if not db_environment.get(key):
        fail(f"db must receive {key}")
if any(key.startswith("DB_BACKUP") for key in db_environment):
    fail("backup account is outside the minimal runtime contract")

redis_environment = env_map(services["redis"])
if not redis_environment.get("REDIS_PASSWORD"):
    fail("redis password must be configured")
redis_command = services["redis"].get("command", [])
if isinstance(redis_command, list):
    redis_command = " ".join(str(part) for part in redis_command)
if "--appendonly no" not in str(redis_command) or "--save" not in str(redis_command):
    fail("redis must be explicitly ephemeral")
if services["redis"].get("volumes"):
    fail("redis must not mount persistent data")

app_environment = env_map(services["app"])
for forbidden in ("DB_ROOT_PASSWORD", "DB_MIGRATOR_PASSWORD", "DB_BACKUP_PASSWORD", "SPRING_FLYWAY_ENABLED"):
    if forbidden in app_environment and forbidden != "SPRING_FLYWAY_ENABLED":
        fail(f"runtime app must not receive {forbidden}")
if app_environment.get("SPRING_FLYWAY_ENABLED") != "false":
    fail("runtime app must disable automatic Flyway migration")
if app_environment.get("REDIS_HOST") != "redis":
    fail("runtime app must connect to the Compose redis service")
if app_environment.get("FIREBASE_CONFIG_PATH") != "/app/config/firebase-key.json":
    fail("runtime app must use the mounted Firebase config path")
if app_environment.get("LOG_FILE") != "/tmp/application.log":
    fail("runtime app logs must stay writable on the read-only container")
if app_environment.get("APP_CORS_REQUIRE_HTTPS") != "true":
    fail("production app must require HTTPS for CORS")
if app_environment.get("APP_AUTH_REFRESH_COOKIE_SECURE") != "true":
    fail("production app refresh cookies must be Secure")

app_health = services["app"].get("healthcheck", {})
health_test = " ".join(str(part) for part in app_health.get("test", []))
if "127.0.0.1:8081/actuator/health/readiness" not in health_test:
    fail("app healthcheck must use sanitized management readiness")

for name in ("db", "redis", "app"):
    if not services[name].get("healthcheck"):
        fail(f"{name} must define a healthcheck")

if services["migrate"].get("profiles") != ["migration"]:
    fail("migrate must be an explicit migration profile")
if services["migrate"].get("restart") != "no":
    fail("migrate must be one-shot")
if "@sha256:" not in str(services["migrate"].get("image", "")):
    fail("migrate image must be digest-pinned")

networks = compose.get("networks", {})
runtime = networks.get("sugang-helper-runtime")
if not runtime or not runtime.get("internal"):
    fail("runtime network must be internal")
egress = networks.get("sugang-helper-egress")
if not egress or egress.get("internal"):
    fail("app egress network must be externally routable")

app_networks = set(services["app"].get("networks", {}))
if app_networks != {"sugang-helper-runtime", "sugang-helper-egress"}:
    fail(f"app must attach to runtime and egress networks: {sorted(app_networks)}")
for name in ("db", "redis", "migrate"):
    if "sugang-helper-egress" in services[name].get("networks", {}):
        fail(f"{name} must not attach to the app egress network")

db_volume = compose.get("volumes", {}).get("db_data", {})
device = db_volume.get("driver_opts", {}).get("device")
if device != "/var/lib/jbnu-sugang-helper/mysql":
    fail(f"db_data must point at the OCI block-volume mount: {device!r}")

print("runtime Compose contract passed")
PY

echo "minimal OCI runtime contract passed"
