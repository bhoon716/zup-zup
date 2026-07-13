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
app_env_file="${APP_ENV_FILE:-${project_root}/apps/server/.env}"
temporary_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${temporary_dir}"
  rm -f "${tmp_config:-}"
}

trap cleanup EXIT

if [ ! -f "${compose_env_file}" ]; then
  compose_env_file="${compose_directory}/.env.example"
fi
if [ ! -f "${compose_env_file}" ]; then
  echo "Compose environment file not found" >&2
  exit 1
fi

if [ ! -f "${app_env_file}" ]; then
  app_env_file="${project_root}/apps/server/.env.example"
fi
if [ ! -f "${app_env_file}" ]; then
  echo "App environment file not found" >&2
  exit 1
fi

release_dir="${temporary_dir}/release"
mkdir -p "${release_dir}/src/main/resources/db/migration"
cp "${app_env_file}" "${release_dir}/.env"

config_output="$(APP_RELEASE_DIR="${release_dir}" docker compose --env-file "${compose_env_file}" -f "${compose_file}" --profile migration config --format json)"
tmp_config="$(mktemp)"
printf '%s' "${config_output}" > "${tmp_config}"

python - "${tmp_config}" <<'PY'
import json
import sys

expected_images = {
    "prometheus": "prom/prometheus@sha256:0e698e35e50d1ddc2d11a4a55b089fe62eb71358a5c204dfafd21bdf8ffe04b8",
    "grafana": "grafana/grafana@sha256:6ea068891652aa6a65ca9065c26b89de939653803c836426970305c11fd00534",
    "nginx-proxy-manager": "jc21/nginx-proxy-manager@sha256:99a885f56ca2203a2eb352a5f9e2cd5c1e25786508debd725ad48ebe955d114f",
    "loki": "grafana/loki@sha256:d14b3a2c419b72fe27cd094c017863bd37a5ea9ac7d72f35bcd25f5bd081dc47",
    "promtail": "grafana/promtail@sha256:238d8562dc29ec83d2f5933c7fd6e9469a90ea316a04f160dcc7cf842910ca94",
}

with open(sys.argv[1], encoding="utf-8") as handle:
    compose = json.load(handle)

services = compose.get("services", {})
networks = compose.get("networks", {})

def fail(message: str) -> None:
    raise SystemExit(message)

def service_network_names(service: dict) -> set[str]:
    configured_networks = service.get("networks", {})
    if isinstance(configured_networks, dict):
        return set(configured_networks)
    return set(configured_networks)

def environment_map(service: dict) -> dict[str, str]:
    environment = service.get("environment", {})
    if isinstance(environment, dict):
        return {str(key): str(value) for key, value in environment.items()}
    result = {}
    for entry in environment:
        key, separator, value = str(entry).partition("=")
        result[key] = value if separator else ""
    return result

for service_name, expected_image in expected_images.items():
    actual_image = services.get(service_name, {}).get("image")
    if actual_image != expected_image:
        fail(f"{service_name} image is not pinned reproducibly: {actual_image!r}")

for service_name in ("db", "redis", "prometheus", "grafana", "loki", "promtail"):
    if services.get(service_name, {}).get("ports"):
        fail(f"{service_name} should not publish host ports")

expected_internal_network_members = {
    "sugang-helper-management": {"app", "prometheus"},
    "sugang-helper-observability": {"prometheus", "grafana"},
}
for network_name, expected_members in expected_internal_network_members.items():
    network = networks.get(network_name)
    if not network or not network.get("internal", False):
        fail(f"{network_name} must be an internal network")

    actual_members = {
        service_name
        for service_name, service in services.items()
        if network_name in service_network_names(service)
    }
    if actual_members != expected_members:
        fail(
            f"{network_name} members must be {sorted(expected_members)!r}: "
            f"{sorted(actual_members)!r}"
        )

npm_ports = services.get("nginx-proxy-manager", {}).get("ports", [])
expected_ports = {
    (80, "80", None),
    (81, "81", "127.0.0.1"),
    (443, "443", None),
}
actual_ports = {
    (
        int(port["target"]),
        str(port["published"]),
        port.get("host_ip"),
    )
    for port in npm_ports
}
if actual_ports != expected_ports:
    fail(f"nginx-proxy-manager ports do not match expected bindings: {sorted(actual_ports)!r}")

if any(
    volume.get("source") == "/var/run/docker.sock"
    for service in services.values()
    for volume in service.get("volumes", [])
):
    fail("docker socket must not be mounted")

grafana_volumes = services.get("grafana", {}).get("volumes", [])
if not any(
    volume.get("type") == "bind"
    and volume.get("source") == "/var/lib/jbnu-sugang-helper/grafana"
    and volume.get("target") == "/var/lib/grafana"
    for volume in grafana_volumes
):
    fail("grafana state volume is missing")

promtail_volumes = services.get("promtail", {}).get("volumes", [])
if not any(volume.get("target") == "/var/log" and volume.get("read_only") for volume in promtail_volumes):
    fail("promtail log mount must be read only")

if not any(
    volume.get("type") == "bind"
    and volume.get("source") == "/var/lib/jbnu-sugang-helper/promtail"
    and volume.get("target") == "/var/lib/promtail"
    for volume in promtail_volumes
):
    fail("promtail state volume is missing")

app_volumes = services.get("app", {}).get("volumes", [])
app = services.get("app", {})
if any(int(port.get("target", -1)) == 8081 for port in app.get("ports", [])):
    fail("app management port 8081 must not publish to the host")

healthcheck = app.get("healthcheck", {})
healthcheck_test = healthcheck.get("test", [])
if isinstance(healthcheck_test, list):
    healthcheck_test = " ".join(str(part) for part in healthcheck_test)
if "127.0.0.1:8081/actuator/health/readiness" not in str(healthcheck_test):
    fail("app healthcheck must use the internal readiness endpoint")

redis = services.get("redis", {})
redis_healthcheck = redis.get("healthcheck", {})
redis_healthcheck_test = redis_healthcheck.get("test", [])
if isinstance(redis_healthcheck_test, list):
    redis_healthcheck_test = " ".join(str(part) for part in redis_healthcheck_test)
if not all(token in str(redis_healthcheck_test) for token in ("REDISCLI_AUTH", "redis-cli", "ping")):
    fail("redis healthcheck must authenticate and verify PING")

redis_command = redis.get("command", [])
if isinstance(redis_command, list):
    redis_command = " ".join(str(part) for part in redis_command)
if not all(token in str(redis_command) for token in ("--appendonly", "yes", "--appendfsync", "everysec")):
    fail("redis must persist state using AOF with appendfsync everysec")

redis_volumes = redis.get("volumes", [])
if not any(
    volume.get("type") == "bind"
    and volume.get("source") == "/var/lib/jbnu-sugang-helper/redis"
    and volume.get("target") == "/data"
    and not volume.get("read_only", False)
    for volume in redis_volumes
):
    fail("redis state must use its writable host bind mount")

redis_dependency = app.get("depends_on", {}).get("redis", {})
if redis_dependency.get("condition") != "service_healthy":
    fail("app must wait for Redis to become healthy")

db = services.get("db", {})
db_environment = environment_map(db)
for required_key in (
    "MYSQL_ROOT_PASSWORD",
    "MYSQL_DATABASE",
    "DB_RUNTIME_USER",
    "DB_RUNTIME_PASSWORD",
    "DB_MIGRATOR_USER",
    "DB_MIGRATOR_PASSWORD",
    "DB_BACKUP_USER",
    "DB_BACKUP_PASSWORD",
):
    if not db_environment.get(required_key):
        fail(f"db must receive {required_key}")

db_command = db.get("command", [])
if isinstance(db_command, list):
    db_command = " ".join(str(part) for part in db_command)
if not all(token in str(db_command) for token in (
    "--server-id=1",
    "--log-bin=mysql-bin",
    "--binlog-format=ROW",
    "--binlog-expire-logs-seconds=604800",
)):
    fail("db must retain row-format binary logs for seven days")

db_volumes = db.get("volumes", [])
if not any(
    volume.get("target") == "/docker-entrypoint-initdb.d/01-provision-service-accounts.sh"
    and volume.get("read_only", False)
    for volume in db_volumes
):
    fail("db must mount the idempotent service-account provisioner read-only")

db_healthcheck = db.get("healthcheck", {})
db_healthcheck_test = db_healthcheck.get("test", [])
if isinstance(db_healthcheck_test, list):
    db_healthcheck_test = " ".join(str(part) for part in db_healthcheck_test)
if not all(token in str(db_healthcheck_test) for token in ("MYSQL_PWD", "mysql", "SELECT 1", "-uroot")):
    fail("db healthcheck must authenticate with the root credential inside the db container")

app_environment = environment_map(app)
allowed_app_database_keys = {
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "SPRING_FLYWAY_ENABLED",
}
for key in app_environment:
    if key.startswith(("DB_", "MYSQL_", "FLYWAY_")):
        fail(f"app must not receive {key}")
    if key.startswith(("SPRING_DATASOURCE_", "SPRING_FLYWAY_")) and key not in allowed_app_database_keys:
        fail(f"app must not receive {key}")

if app_environment.get("SPRING_FLYWAY_ENABLED", "").lower() != "false":
    fail("app must disable in-process Flyway")
if app_environment.get("SPRING_DATASOURCE_USERNAME") != db_environment["DB_RUNTIME_USER"]:
    fail("app must receive the runtime DB username only")
if app_environment.get("SPRING_DATASOURCE_PASSWORD") != db_environment["DB_RUNTIME_PASSWORD"]:
    fail("app must receive the runtime DB password only")

migrate = services.get("migrate", {})
if migrate.get("image") != "flyway/flyway@sha256:8ace7d9825bb3ad1d6e14ee27b3a830b638ac841ba424b99b2d92aa65a99d484":
    fail("migrator image must be pinned to the reviewed Flyway 11.7.2 multi-architecture digest")
if "migration" not in migrate.get("profiles", []):
    fail("migrator must be an explicit one-shot migration profile")
if not migrate.get("read_only", False):
    fail("migrator root filesystem must be read only")

migrate_environment = environment_map(migrate)
if migrate_environment.get("FLYWAY_USER") != db_environment["DB_MIGRATOR_USER"]:
    fail("migrator must use the migration-only DB account")
if migrate_environment.get("FLYWAY_PASSWORD") != db_environment["DB_MIGRATOR_PASSWORD"]:
    fail("migrator must use the migration-only DB password")
if migrate_environment.get("FLYWAY_CLEAN_DISABLED", "").lower() != "true":
    fail("migrator must disable Flyway clean")

migrate_volumes = migrate.get("volumes", [])
if not any(
    volume.get("target") == "/flyway/sql" and volume.get("read_only", False)
    for volume in migrate_volumes
):
    fail("migrator must receive migration SQL through a read-only mount")

if app.get("user") != "10001:10001":
    fail("app must run as the dedicated non-root UID/GID")

if not app.get("read_only", False):
    fail("app root filesystem must be read only")

if not any(
    (tmpfs.get("target") == "/tmp" if isinstance(tmpfs, dict) else str(tmpfs).startswith("/tmp"))
    for tmpfs in app.get("tmpfs", [])
):
    fail("app must provide a writable tmpfs at /tmp")

if not any(
    volume.get("type") == "bind"
    and volume.get("source") == "/var/lib/jbnu-sugang-helper/uploads"
    and volume.get("target") == "/app/data/uploads"
    and not volume.get("read_only", False)
    for volume in app_volumes
):
    fail("app upload storage must be a writable host bind mount")

print("compose policy verification passed")
PY

prepare_host_directories_script="$(dirname "$0")/prepare-app-host-directories.sh"
if ! grep -F -- 'install -d -o 999 -g 1000 -m 0700' "${prepare_host_directories_script}" >/dev/null; then
  echo "Redis state directory must be owner-only on the host" >&2
  exit 1
fi

if ! grep -F -- 'targets: ["app:8081"]' "${compose_directory}/prometheus/prometheus.yml" >/dev/null; then
  echo "Prometheus must scrape the internal management port" >&2
  exit 1
fi

if ! grep -F -- 'url: http://prometheus:9090' "${compose_directory}/grafana/provisioning/datasources/datasource.yml" >/dev/null; then
  echo "Grafana must use the Prometheus service name on the observability network" >&2
  exit 1
fi

COMPOSE_ENV_FILE="${compose_env_file}" \
APP_RELEASE_DIR="${release_dir}" \
  "$(dirname "$0")/verify-log-policy.sh" "${compose_file}"
