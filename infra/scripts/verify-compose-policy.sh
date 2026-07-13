#!/usr/bin/env bash
set -euo pipefail

compose_file="${1:-docker-compose.yml}"

if [ ! -f "${compose_file}" ]; then
  echo "compose file not found: ${compose_file}" >&2
  exit 1
fi

# verification 시 .env가 없으면 .env.example을 임시 복사하여 변수 파싱 에러 방지
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  cp .env.example .env
fi

if [ ! -f "../apps/server/.env" ] && [ -f "../apps/server/.env.example" ]; then
  cp ../apps/server/.env.example ../apps/server/.env
fi


config_output="$(docker compose -f "${compose_file}" config --format json)"
tmp_config="$(mktemp)"

cleanup() {
  rm -f "${tmp_config}"
}

trap cleanup EXIT
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

compose_directory="$(cd "$(dirname "${compose_file}")" && pwd)"
if ! grep -F -- 'targets: ["app:8081"]' "${compose_directory}/prometheus/prometheus.yml" >/dev/null; then
  echo "Prometheus must scrape the internal management port" >&2
  exit 1
fi

if ! grep -F -- 'url: http://prometheus:9090' "${compose_directory}/grafana/provisioning/datasources/datasource.yml" >/dev/null; then
  echo "Grafana must use the Prometheus service name on the observability network" >&2
  exit 1
fi

"$(dirname "$0")/verify-log-policy.sh" "${compose_file}"
