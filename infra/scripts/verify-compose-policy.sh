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

def fail(message: str) -> None:
    raise SystemExit(message)

for service_name, expected_image in expected_images.items():
    actual_image = services.get(service_name, {}).get("image")
    if actual_image != expected_image:
        fail(f"{service_name} image is not pinned reproducibly: {actual_image!r}")

for service_name in ("db", "redis", "prometheus", "grafana", "loki", "promtail"):
    if services.get(service_name, {}).get("ports"):
        fail(f"{service_name} should not publish host ports")

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

print("compose policy verification passed")
PY

"$(dirname "$0")/verify-log-policy.sh" "${compose_file}"
