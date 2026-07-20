#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_dir="${repo_root}/infra"
compose_file="${compose_dir}/docker-compose.yml"
compose_env="${compose_dir}/.env.example"
temporary_dir="$(mktemp -d)"
config_file="${temporary_dir}/compose.json"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

APP_ENV_FILE="${repo_root}/apps/server/.env.example" docker compose \
  --env-file "${compose_env}" \
  -f "${compose_file}" \
  --profile observability \
  config --format json >"${config_file}"

python3 - "${config_file}" "${repo_root}" <<'PY'
import json
import sys
from pathlib import Path

with open(sys.argv[1], encoding="utf-8") as handle:
    compose = json.load(handle)
repo_root = Path(sys.argv[2])
services = compose.get("services", {})

def fail(message):
    raise SystemExit(message)

expected = {"app", "db", "redis", "loki", "alloy", "grafana", "prometheus"}
if set(services) != expected:
    fail(f"observability profile must contain app/db/redis/loki/alloy/grafana/prometheus: {sorted(services)}")

for name in ("loki", "alloy", "grafana", "prometheus"):
    if services[name].get("profiles") != ["observability"]:
        fail(f"{name} must be opt-in through the observability profile")
    if services[name].get("restart") != "unless-stopped":
        fail(f"{name} must use restart: unless-stopped")
    if "sugang-helper-runtime" not in services[name].get("networks", {}):
        fail(f"{name} must stay on the internal runtime network")
    if not services[name].get("healthcheck"):
        fail(f"{name} must define a healthcheck")

if not str(services["loki"].get("image", "")).startswith("grafana/loki@sha256:"):
    fail("Loki image must be digest pinned")
loki_mounts = {str(item.get("target")): item for item in services["loki"].get("volumes", [])}
if "/etc/loki/local-config.yaml" not in loki_mounts or "/var/lib/loki" not in loki_mounts:
    fail("Loki config and persistent data mounts are required")

prometheus = services["prometheus"]
if not str(prometheus.get("image", "")).startswith("prom/prometheus@sha256:"):
    fail("Prometheus image must be digest pinned")
if prometheus.get("ports"):
    fail("Prometheus must not publish a host port")
prometheus_mounts = {str(item.get("target")): item for item in prometheus.get("volumes", [])}
if "/etc/prometheus/prometheus.yml" not in prometheus_mounts or "/prometheus" not in prometheus_mounts:
    fail("Prometheus config and persistent data mounts are required")
prometheus_command = " ".join(str(item) for item in prometheus.get("command", []))
if "--config.file=/etc/prometheus/prometheus.yml" not in prometheus_command:
    fail("Prometheus must use the staged config file")
if "--storage.tsdb.path=/prometheus" not in prometheus_command:
    fail("Prometheus must use the persistent data path")
if not prometheus.get("healthcheck"):
    fail("Prometheus must define a healthcheck")
prometheus_config = (repo_root / "infra/prometheus/prometheus.yml").read_text(encoding="utf-8")
for required in ("job_name: sugang-helper-app", "metrics_path: /actuator/prometheus", "app:8081"):
    if required not in prometheus_config:
        fail(f"Prometheus config must scrape the app Actuator endpoint: {required}")

if not str(services["alloy"].get("image", "")).startswith("grafana/alloy@sha256:"):
    fail("Alloy image must be digest pinned")
alloy_mounts = {str(item.get("target")): item for item in services["alloy"].get("volumes", [])}
if "/etc/alloy/config.alloy" not in alloy_mounts:
    fail("Alloy config mount is required")
if alloy_mounts.get("/var/run/docker.sock", {}).get("read_only") is not True:
    fail("Alloy must read the Docker socket without write access")
alloy_config = (repo_root / "infra/alloy/config.alloy").read_text(encoding="utf-8")
if "loki.source.docker" not in alloy_config:
    fail("Alloy must collect Docker logs with loki.source.docker")
if "loki.write" not in alloy_config:
    fail("Alloy must write logs to Loki")

if not str(services["grafana"].get("image", "")).startswith("grafana/grafana@sha256:"):
    fail("Grafana image must be digest pinned")
grafana_ports = services["grafana"].get("ports", [])
if not any(str(port.get("host_ip")) == "127.0.0.1" and str(port.get("target")) == "3000" for port in grafana_ports):
    fail("Grafana must be reachable only through localhost")
grafana_healthcheck = " ".join(str(item) for item in services["grafana"]["healthcheck"].get("test", []))
if "grep -Eq" not in grafana_healthcheck or "[[:space:]]*" not in grafana_healthcheck:
    fail("Grafana healthcheck must accept JSON whitespace around the database status")

datasource = (repo_root / "infra/grafana/provisioning/datasources/datasource.yml").read_text(encoding="utf-8")
if "type: loki" not in datasource or "isDefault: true" not in datasource:
    fail("Grafana must provision Loki as its default datasource")
if "type: prometheus" not in datasource or "url: http://prometheus:9090" not in datasource:
    fail("Grafana must provision the internal Prometheus datasource")
if services["grafana"].get("depends_on", {}).get("prometheus", {}).get("condition") != "service_healthy":
    fail("Grafana must wait for healthy Prometheus")

dashboard_path = repo_root / "infra/grafana/dashboards/application-metrics-dashboard.json"
if not dashboard_path.exists():
    fail("Grafana application metrics dashboard is missing")
dashboard = dashboard_path.read_text(encoding="utf-8")
for required in ("Prometheus", "http_server_requests_seconds_count", "jvm_memory_used_bytes"):
    if required not in dashboard:
        fail(f"application metrics dashboard is missing: {required}")

compose_text = (repo_root / "infra/docker-compose.yml").read_text(encoding="utf-8")
if "promtail" in compose_text:
    fail("Promtail must not remain in the production Compose contract")
for obsolete_service in ("alertmanager", "cadvisor", "node_exporter"):
    if obsolete_service in compose_text:
        fail(f"unrequested observability service must not remain: {obsolete_service}")

for obsolete in (
    repo_root / "infra/alertmanager",
    repo_root / "infra/grafana/dashboards/jvm-dashboard.json",
    repo_root / "infra/grafana/dashboards/notification-slo-dashboard.json",
):
    if obsolete.exists():
        fail(f"obsolete observability file must be removed: {obsolete}")

print("observability Compose contract passed")
PY
