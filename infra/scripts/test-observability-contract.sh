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
import re
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
    "docker-log-proxy",
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

for name in ("loki", "alloy", "docker-log-proxy", "prometheus"):
    if services[name].get("ports"):
        raise SystemExit(f"{name} must not publish a host port")

grafana_ports = services["grafana"].get("ports", [])
if len(grafana_ports) != 1 or str(grafana_ports[0].get("host_ip")) != "127.0.0.1":
    raise SystemExit("Grafana must publish exactly one localhost-only port")

grafana_environment = services["grafana"].get("environment", {})
grafana_domain_settings = {
    "GF_SERVER_DOMAIN": "grafana.zup-zup.com",
    "GF_SERVER_ROOT_URL": "https://grafana.zup-zup.com/",
    "GF_SECURITY_COOKIE_SECURE": "true",
    "GF_LIVE_ALLOWED_ORIGINS": "https://grafana.zup-zup.com,https://jbnu-sugang-helper-grafana.duckdns.org",
}
for key, expected in grafana_domain_settings.items():
    if grafana_environment.get(key) != expected:
        raise SystemExit(f"Grafana domain contract is missing or changed: {key}")

alloy_mounts = {str(item.get("target")): item for item in services["alloy"].get("volumes", [])}
if "/var/run/docker.sock" in alloy_mounts:
    raise SystemExit("Alloy must not mount the Docker socket")
proxy = services["docker-log-proxy"]
proxy_mounts = {str(item.get("target")): item for item in proxy.get("volumes", [])}
if proxy_mounts.get("/var/run/docker.sock", {}).get("read_only") is not True:
    raise SystemExit("Only the log proxy should mount the Docker socket")
if set(proxy.get("networks", {})) != {"sugang-helper-docker-logs"}:
    raise SystemExit("The log proxy must be isolated from the shared runtime network")
if not compose["networks"]["sugang-helper-docker-logs"].get("internal"):
    raise SystemExit("The Docker log network must be internal")
if services["alloy"]["depends_on"].get("docker-log-proxy", {}).get("condition") != "service_healthy":
    raise SystemExit("Alloy must wait for the Docker log proxy")

runtime_network = compose.get("networks", {}).get("sugang-helper-runtime", {})
if not runtime_network.get("internal"):
    raise SystemExit("observability services must use the internal runtime network")

alloy = (repo_root / "infra/alloy/config.alloy").read_text(encoding="utf-8")
for required in (
    'discovery.docker "containers"',
    'loki.source.docker "containers"',
    'host             = "http://docker-log-proxy:2375"',
    'refresh_interval = "5s"',
    'target_label  = "job"',
    'source_labels = ["__meta_docker_container_label_com_docker_compose_project"]',
    'regex = "sugang-helper"',
    'action = "keep"',
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

prometheus_mounts = {str(item.get("target")): item for item in services["prometheus"].get("volumes", [])}
if prometheus_mounts.get("/etc/prometheus/rules", {}).get("read_only") is not True:
    raise SystemExit("Prometheus rules must be mounted read-only")
for dashboard in (repo_root / "infra/grafana/dashboards").glob("*.json"):
    definition = json.loads(dashboard.read_text(encoding="utf-8"))
    if definition.get("uid") == "zup-zup-operations":
        raise SystemExit("The imported operations dashboard must not be file-provisioned")

if "GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH" in grafana_environment:
    raise SystemExit("Imported dashboards must use database preferences rather than a home JSON file")
provider = (repo_root / "infra/grafana/provisioning/dashboards/dashboard.yml").read_text(encoding="utf-8")
if "disableDeletion: true" not in provider:
    raise SystemExit("Removing a dashboard file must preserve its database definition")

loki_dashboard = json.loads((repo_root / "infra/grafana/dashboards/loki-dashboard.json").read_text(encoding="utf-8"))
log_queries = [target["expr"] for panel in loki_dashboard["panels"] for target in panel.get("targets", [])]
if len(log_queries) != 4 or any('{app="jbnu-sugang-helper",job=~"$job"}' not in query for query in log_queries):
    raise SystemExit("Loki queries must retain a non-empty app matcher when All expands to .*")
job_variable = next(variable for variable in loki_dashboard["templating"]["list"] if variable["name"] == "job")
if job_variable["allValue"] != ".*" or 'app="jbnu-sugang-helper"' not in job_variable["query"]:
    raise SystemExit("Loki job choices must use the same app scope without excluding missing jobs from All")

firstline = re.search(r'firstline\s*=\s*("(?:\\.|[^"\\])*")', alloy)
if not firstline:
    raise SystemExit("App logs must retain JSON and legacy multiline boundaries")
pattern = json.loads(firstline.group(1))
for line in ('{"level":"INFO","message":"one event"}', "2026-11-06 12:00:00 INFO legacy"):
    if not re.match(pattern, line):
        raise SystemExit("App multiline must recognize both JSON and legacy log starts")
if re.match(pattern, "\tat app.Main.run(Main.java:1)"):
    raise SystemExit("Legacy stack frames must remain attached to their log event")

trace_query = next(panel["targets"][0]["expr"] for panel in loki_dashboard["panels"] if panel["id"] == 4)
if "${correlationId:regex}" not in trace_query or " |~ `" not in trace_query:
    raise SystemExit("Trace search must escape the ID and support JSON plus legacy logs")
trace_pattern = trace_query.split(" |~ `", 1)[1].removesuffix("`")
trace_pattern = trace_pattern.replace("${correlationId:regex}", re.escape("request.123"))
for line in ('{"correlationId":"request.123"}', '{"correlationId": "request.123"}', "[corr=request.123] legacy"):
    if not re.search(trace_pattern, line):
        raise SystemExit("Trace search must match both JSON and legacy logs")
if re.search(trace_pattern, '{"correlationId":"requestX123"}'):
    raise SystemExit("Trace search must treat punctuation in the ID literally")

empty_trace_pattern = trace_query.split(" |~ `", 1)[1].removesuffix("`").replace("${correlationId:regex}", "")
for line in ('{"correlationId":"request.123"}', "[corr=request.123] legacy"):
    if not re.search(empty_trace_pattern, line):
        raise SystemExit("Empty trace input must retain both JSON and legacy request logs")

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
  -v "${repo_root}/infra/prometheus:/etc/prometheus:ro" \
  "${prometheus_image}" check config /etc/prometheus/prometheus.yml

docker run --rm \
  --entrypoint /bin/promtool \
  -v "${repo_root}/infra/prometheus:/etc/prometheus:ro" \
  --workdir /etc/prometheus \
  "${prometheus_image}" test rules tests/alerts.test.yml

docker run --rm \
  --entrypoint /bin/promtool \
  -v "${repo_root}/infra/prometheus:/etc/prometheus:ro" \
  --workdir /etc/prometheus \
  "${prometheus_image}" test rules tests/host-alerts.test.yml

echo "observability config validation passed"
