#!/usr/bin/env bash
set -euo pipefail

# This smoke runs from the Docker host after the observability profile and the
# application are started. It intentionally uses a small, digest-pinned
# BusyBox container on the internal runtime network because Loki is distroless
# and the production host must not publish Loki, Alloy, or Prometheus ports.
readonly DEFAULT_PROBE_IMAGE="busybox@sha256:162f541afa4542abb43b3bc51efd26c659dd8fa8b0db3a0b8e348908d75efed1"
readonly DEFAULT_RUNTIME_NETWORK="sugang-helper-runtime"
readonly DEFAULT_GRAFANA_URL="http://127.0.0.1:3000"
readonly DEFAULT_GRAFANA_CONTAINER="sugang-helper-grafana"

runtime_network="${RUNTIME_NETWORK_NAME:-${DEFAULT_RUNTIME_NETWORK}}"
probe_image="${DEFAULT_PROBE_IMAGE}"
grafana_url="${GRAFANA_URL:-${DEFAULT_GRAFANA_URL}}"
grafana_container="${GRAFANA_CONTAINER_NAME:-${DEFAULT_GRAFANA_CONTAINER}}"
grafana_user="${GRAFANA_ADMIN_USER:-}"
grafana_password="${GRAFANA_ADMIN_PASSWORD:-}"
timeout_seconds="${OBSERVABILITY_SMOKE_TIMEOUT_SECONDS:-120}"
http_timeout_seconds="${OBSERVABILITY_SMOKE_HTTP_TIMEOUT_SECONDS:-5}"
marker_container=""
temporary_dir="$(mktemp -d)"
deadline=0

run_with_timeout() {
  local timeout_seconds="$1"
  shift
  local child_pid
  local end_time

  end_time=$((SECONDS + timeout_seconds))
  "$@" &
  child_pid=$!
  while kill -0 "${child_pid}" >/dev/null 2>&1; do
    if [ "${SECONDS}" -ge "${end_time}" ]; then
      kill -TERM "${child_pid}" >/dev/null 2>&1 || true
      sleep 0.1
      kill -KILL "${child_pid}" >/dev/null 2>&1 || true
      wait "${child_pid}" >/dev/null 2>&1 || true
      return 124
    fi
    sleep 0.1
  done
  wait "${child_pid}"
}

run_with_smoke_deadline() {
  local remaining=$((deadline - SECONDS))
  if [ "${remaining}" -le 0 ]; then
    return 124
  fi
  run_with_timeout "${remaining}" "$@"
}

cleanup() {
  if [ -n "${marker_container}" ]; then
    run_with_timeout 5 docker rm -f -- "${marker_container}" >/dev/null 2>&1 || true
  fi
  rm -rf -- "${temporary_dir}"
}
trap cleanup EXIT

fail() {
  echo "observability smoke failed: $*" >&2
  exit 1
}

is_positive_integer() {
  [[ "$1" =~ ^[1-9][0-9]*$ ]]
}

command -v docker >/dev/null || fail "docker is required"
command -v curl >/dev/null || fail "host curl is required for the Grafana endpoint"
command -v python3 >/dev/null || fail "python3 is required to validate query JSON"
is_positive_integer "${timeout_seconds}" || fail "OBSERVABILITY_SMOKE_TIMEOUT_SECONDS must be a positive integer"
is_positive_integer "${http_timeout_seconds}" || fail "OBSERVABILITY_SMOKE_HTTP_TIMEOUT_SECONDS must be a positive integer"
[ -n "${grafana_user}" ] || fail "GRAFANA_ADMIN_USER is required for datasource API checks"
[ -n "${grafana_password}" ] || fail "GRAFANA_ADMIN_PASSWORD is required for datasource API checks"

deadline=$((SECONDS + timeout_seconds))

escape_curl_config_value() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  printf '%s' "${value}"
}

grafana_curl_config="${temporary_dir}/grafana-curl.conf"
grafana_user_config="$(escape_curl_config_value "${grafana_user}")"
grafana_password_config="$(escape_curl_config_value "${grafana_password}")"
printf 'user = "%s:%s"\n' "${grafana_user_config}" "${grafana_password_config}" >"${grafana_curl_config}"
chmod 0600 "${grafana_curl_config}"

if ! [[ "${runtime_network}" =~ ^[A-Za-z0-9_.-]+$ ]]; then
  fail "RUNTIME_NETWORK_NAME contains unsupported characters"
fi
if ! run_with_smoke_deadline docker network inspect "${runtime_network}" >/dev/null 2>&1; then
  fail "runtime network is missing: ${runtime_network}"
fi
if ! run_with_smoke_deadline docker image inspect "${probe_image}" >/dev/null 2>&1; then
  fail "pinned observability probe image is not available locally"
fi

if ! run_with_smoke_deadline docker inspect "${grafana_container}" >/dev/null 2>&1; then
  fail "Grafana container is missing: ${grafana_container}"
fi

probe_http() {
  run_with_smoke_deadline docker run --rm \
    --network "${runtime_network}" \
    "${probe_image}" \
    wget -q -O - -T "${http_timeout_seconds}" "$@"
}

grafana_curl() {
  curl --config "${grafana_curl_config}" \
    --fail --silent --show-error --max-time "${http_timeout_seconds}" "$@"
}

wait_for() {
  local description="$1"
  local remaining
  shift
  while [ "${SECONDS}" -lt "${deadline}" ]; do
    if "$@" >/dev/null 2>&1; then
      return 0
    fi
    remaining=$((deadline - SECONDS))
    if [ "${remaining}" -le 0 ]; then
      break
    fi
    if [ "${remaining}" -gt 2 ]; then
      sleep 2
    else
      sleep 0.1
    fi
  done
  fail "${description} did not succeed within ${timeout_seconds}s"
}

check_loki_ready() {
  probe_http http://loki:3100/ready
}

check_alloy_ready() {
  probe_http http://alloy:12345/-/ready
}

check_prometheus_ready() {
  probe_http http://prometheus:9090/-/ready
}

check_app_ready() {
  probe_http http://app:8081/actuator/health/readiness
}

check_grafana_ready() {
  grafana_curl "${grafana_url}/api/health"
}

parse_prometheus_up() {
  python3 - "$1" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    payload = json.load(handle)

if payload.get("status") != "success":
    raise SystemExit(1)
for result in payload.get("data", {}).get("result", []):
    value = result.get("value", [])
    if len(value) >= 2 and str(value[1]) == "1":
        raise SystemExit(0)
raise SystemExit(1)
PY
}

check_prometheus_app_target() {
  local response_file="${temporary_dir}/prometheus-target.json"
  probe_http \
    'http://prometheus:9090/api/v1/query?query=up%7Bjob%3D%22sugang-helper-app%22%7D' \
    >"${response_file}" \
    && parse_prometheus_up "${response_file}"
}

check_grafana_datasources() {
  local uid
  local response
  for uid in prometheus loki; do
    response="$(grafana_curl "${grafana_url}/api/datasources/uid/${uid}/health")" || return 1
    python3 - "${response}" <<'PY' || return 1
import json
import sys

payload = json.loads(sys.argv[1])
if payload.get("status") != "OK":
    raise SystemExit(1)
PY
  done
}

check_grafana_prometheus_query() {
  local response_file="${temporary_dir}/grafana-prometheus-query.json"
  grafana_curl \
    "${grafana_url}/api/datasources/proxy/uid/prometheus/api/v1/query?query=up%7Bjob%3D%22sugang-helper-app%22%7D" \
    >"${response_file}" \
    && parse_prometheus_up "${response_file}"
}

build_loki_query_url() {
  python3 - "$1" "$2" <<'PY'
import sys
import time
from urllib.parse import quote

marker = sys.argv[1]
base_url = sys.argv[2]
query = '{job="observability-smoke"} |= "' + marker + '"'
now = time.time_ns()
start = now - 5 * 60 * 1_000_000_000
end = now + 60 * 1_000_000_000
print(
    f"{base_url}?query={quote(query, safe='')}&limit=10"
    f"&start={start}&end={end}"
)
PY
}

parse_loki_marker() {
  python3 - "$1" "$2" <<'PY'
import json
import sys

marker = sys.argv[2]
with open(sys.argv[1], encoding="utf-8") as handle:
    payload = json.load(handle)

if payload.get("status") != "success":
    raise SystemExit(1)
for stream in payload.get("data", {}).get("result", []):
    for entry in stream.get("values", []):
        if len(entry) >= 2 and marker in str(entry[1]):
            raise SystemExit(0)
raise SystemExit(1)
PY
}

check_loki_marker() {
  local response_file="${temporary_dir}/loki-marker.json"
  local query_url
  query_url="$(build_loki_query_url "${marker_text}" "http://loki:3100/loki/api/v1/query_range")"
  probe_http "${query_url}" >"${response_file}" \
    && parse_loki_marker "${response_file}" "${marker_text}"
}

check_grafana_loki_marker() {
  local response_file="${temporary_dir}/grafana-loki-marker.json"
  local query_url
  query_url="$(build_loki_query_url "${marker_text}" "${grafana_url}/api/datasources/proxy/uid/loki/loki/api/v1/query_range")"
  grafana_curl "${query_url}" >"${response_file}" \
    && parse_loki_marker "${response_file}" "${marker_text}"
}

wait_for "Loki HTTP readiness" check_loki_ready
wait_for "Alloy HTTP readiness" check_alloy_ready
wait_for "Prometheus HTTP readiness" check_prometheus_ready
wait_for "application readiness from the runtime network" check_app_ready
wait_for "Grafana HTTP health" check_grafana_ready
wait_for "Prometheus app target up" check_prometheus_app_target
wait_for "Grafana Prometheus datasource health" check_grafana_datasources
wait_for "Grafana Prometheus query proxy" check_grafana_prometheus_query

marker_id="$(python3 -c 'import uuid; print(uuid.uuid4().hex)')"
marker_text="observability-smoke-${marker_id}"
marker_container="sugang-observability-smoke-${marker_id}"

run_with_smoke_deadline docker run -d \
  --name "${marker_container}" \
  --network "${runtime_network}" \
  --label com.docker.compose.project=sugang-helper \
  --label com.docker.compose.service=observability-smoke \
  --log-driver=json-file \
  --log-opt max-size=10m \
  --log-opt max-file=5 \
  --entrypoint /bin/sh \
  "${probe_image}" \
  -c "printf '%s\\n' '${marker_text}'; sleep 30" \
  >/dev/null \
  || fail "could not start the Docker JSON marker container"

wait_for "Docker JSON to Alloy to Loki marker round-trip" check_loki_marker
wait_for "Grafana Loki datasource marker query" check_grafana_loki_marker

echo "observability data-plane smoke passed (marker=${marker_text})"
