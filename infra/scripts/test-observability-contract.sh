#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_file="${repo_root}/infra/docker-compose.yml"
compose_env="${repo_root}/infra/.env.example"
temporary_dir="$(mktemp -d)"
compose_json="${temporary_dir}/compose.json"
images_file="${temporary_dir}/images"

cleanup() {
  rm -rf -- "${temporary_dir}"
}
trap cleanup EXIT

APP_ENV_FILE="${repo_root}/apps/server/.env.example" \
  docker compose \
    --env-file "${compose_env}" \
    -f "${compose_file}" \
    --profile observability \
    config --format json >"${compose_json}"

python3 - "${compose_json}" "${repo_root}" "${images_file}" <<'PY'
import json
import sys
from pathlib import Path

compose_path = Path(sys.argv[1])
repo_root = Path(sys.argv[2])
images_path = Path(sys.argv[3])
compose = json.loads(compose_path.read_text(encoding="utf-8"))
services = compose.get("services", {})

required_services = {
    "loki",
    "alloy",
    "prometheus",
    "grafana",
    "observability-probe-tools",
}
missing = required_services - set(services)
if missing:
    raise SystemExit(f"missing observability services: {sorted(missing)}")

for name in required_services:
    image = str(services[name].get("image", ""))
    if "@sha256:" not in image:
        raise SystemExit(f"{name} image must be digest pinned")
    if not services[name].get("healthcheck") and name != "observability-probe-tools":
        raise SystemExit(f"{name} must define a healthcheck")

for name in ("loki", "alloy", "prometheus"):
    if services[name].get("ports"):
        raise SystemExit(f"{name} must not publish a host port")

grafana_ports = services["grafana"].get("ports", [])
if len(grafana_ports) != 1 or str(grafana_ports[0].get("host_ip")) != "127.0.0.1":
    raise SystemExit("Grafana must publish exactly one localhost-only port")

alloy_mounts = {str(item.get("target")): item for item in services["alloy"].get("volumes", [])}
if alloy_mounts.get("/var/run/docker.sock", {}).get("read_only") is not True:
    raise SystemExit("Alloy must mount the Docker socket read-only")

runtime_network = compose.get("networks", {}).get("sugang-helper-runtime", {})
if not runtime_network.get("internal"):
    raise SystemExit("observability services must use the internal runtime network")

alloy = (repo_root / "infra/alloy/config.alloy").read_text(encoding="utf-8")
for required in (
    'discovery.docker "containers"',
    'loki.source.docker "containers"',
    'refresh_interval = "5s"',
    'target_label  = "job"',
    'url = "http://loki:3100/loki/api/v1/push"',
):
    if required not in alloy:
        raise SystemExit(f"Alloy pipeline is missing: {required}")

prometheus = (repo_root / "infra/prometheus/prometheus.yml").read_text(encoding="utf-8")
for required in ("job_name: sugang-helper-app", "app:8081", "/actuator/prometheus"):
    if required not in prometheus:
        raise SystemExit(f"Prometheus config is missing: {required}")

datasources = (repo_root / "infra/grafana/provisioning/datasources/datasource.yml").read_text(encoding="utf-8")
for required in ("http://prometheus:9090", "http://loki:3100"):
    if required not in datasources:
        raise SystemExit(f"Grafana datasource is missing: {required}")

for dashboard in (repo_root / "infra/grafana/dashboards").glob("*.json"):
    json.loads(dashboard.read_text(encoding="utf-8"))

images_path.write_text(
    "\n".join(
        str(services[name]["image"])
        for name in ("alloy", "loki", "prometheus")
    ) + "\n",
    encoding="utf-8",
)
PY

alloy_image="$(sed -n '1p' "${images_file}")"
loki_image="$(sed -n '2p' "${images_file}")"
prometheus_image="$(sed -n '3p' "${images_file}")"

docker run --rm \
  -v "${repo_root}/infra/alloy/config.alloy:/etc/alloy/config.alloy:ro" \
  "${alloy_image}" validate /etc/alloy/config.alloy

docker run --rm \
  -v "${repo_root}/infra/loki/loki-config.yaml:/etc/loki/local-config.yaml:ro" \
  "${loki_image}" \
  -config.file=/etc/loki/local-config.yaml -verify-config=true

docker run --rm \
  --entrypoint /bin/promtool \
  -v "${repo_root}/infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  "${prometheus_image}" check config /etc/prometheus/prometheus.yml

echo "observability config validation passed"
