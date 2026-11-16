#!/usr/bin/env bash
set -euo pipefail

readonly DEFAULT_PROBE_IMAGE="busybox@sha256:162f541afa4542abb43b3bc51efd26c659dd8fa8b0db3a0b8e348908d75efed1"
readonly DEFAULT_RUNTIME_NETWORK="sugang-helper-runtime"
readonly DEFAULT_GRAFANA_URL="http://127.0.0.1:3000"
readonly DEFAULT_GRAFANA_CONTAINER="sugang-helper-grafana"

runtime_network="${RUNTIME_NETWORK_NAME:-${DEFAULT_RUNTIME_NETWORK}}"
probe_image="${DEFAULT_PROBE_IMAGE}"
grafana_url="${GRAFANA_URL:-${DEFAULT_GRAFANA_URL}}"
grafana_container="${GRAFANA_CONTAINER_NAME:-${DEFAULT_GRAFANA_CONTAINER}}"
grafana_token_file="${GRAFANA_SMOKE_TOKEN_FILE:-}"
grafana_user="${GRAFANA_ADMIN_USER:-}"
grafana_password="${GRAFANA_ADMIN_PASSWORD:-}"
grafana_prometheus_uid=""
grafana_loki_uid=""
timeout_seconds="${OBSERVABILITY_SMOKE_TIMEOUT_SECONDS:-45}"
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
  echo "모니터링 점검에 실패했습니다: $*" >&2
  exit 1
}

is_positive_integer() {
  [[ "$1" =~ ^[1-9][0-9]*$ ]]
}

command -v docker >/dev/null || fail "Docker가 필요합니다."
command -v curl >/dev/null || fail "Grafana 주소를 확인하려면 호스트에 curl이 필요합니다."
command -v python3 >/dev/null || fail "쿼리 결과 JSON을 검증하려면 python3가 필요합니다."
is_positive_integer "${timeout_seconds}" || fail "OBSERVABILITY_SMOKE_TIMEOUT_SECONDS는 양의 정수여야 합니다."
is_positive_integer "${http_timeout_seconds}" || fail "OBSERVABILITY_SMOKE_HTTP_TIMEOUT_SECONDS는 양의 정수여야 합니다."

deadline=$((SECONDS + timeout_seconds))
marker_lifetime_seconds=$((timeout_seconds + http_timeout_seconds + 10))

escape_curl_config_value() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  printf '%s' "${value}"
}

grafana_curl_config="${temporary_dir}/grafana-curl.conf"

write_grafana_curl_config() {
  if [ -n "${grafana_token_file}" ]; then
    [[ "${grafana_url}" =~ ^http://127\.0\.0\.1:[0-9]{1,5}$ ]] \
      || fail "Grafana 토큰 인증에는 루프백 Grafana 주소가 필요합니다."
    [ -f "${grafana_token_file}" ] && [ -r "${grafana_token_file}" ] \
      || fail "GRAFANA_SMOKE_TOKEN_FILE은 읽을 수 있는 일반 파일이어야 합니다."
    local token
    token="$(<"${grafana_token_file}")" || fail "Grafana 점검 토큰을 읽지 못했습니다."
    [[ "${token}" =~ ^glsa_[A-Za-z0-9_+=/-]{12,490}$ ]] || fail "Grafana 점검 토큰이 올바르지 않습니다."
    printf 'header = "Authorization: Bearer %s"\n' "${token}"
    return
  fi

  [ -n "${grafana_user}" ] || fail "데이터소스 API 점검에는 GRAFANA_ADMIN_USER가 필요합니다."
  [ -n "${grafana_password}" ] || fail "데이터소스 API 점검에는 GRAFANA_ADMIN_PASSWORD가 필요합니다."
  printf 'user = "%s:%s"\n' "$(escape_curl_config_value "${grafana_user}")" \
    "$(escape_curl_config_value "${grafana_password}")"
}

(umask 077; write_grafana_curl_config >"${grafana_curl_config}")

if ! [[ "${runtime_network}" =~ ^[A-Za-z0-9_.-]+$ ]]; then
  fail "RUNTIME_NETWORK_NAME에 지원하지 않는 문자가 포함됐습니다."
fi
if ! run_with_smoke_deadline docker network inspect "${runtime_network}" >/dev/null 2>&1; then
  fail "실행 네트워크가 없습니다: ${runtime_network}"
fi
if ! run_with_smoke_deadline docker image inspect "${probe_image}" >/dev/null 2>&1; then
  fail "버전이 고정된 모니터링 점검 이미지가 로컬에 없습니다."
fi

if ! run_with_smoke_deadline docker inspect "${grafana_container}" >/dev/null 2>&1; then
  fail "Grafana 컨테이너가 없습니다: ${grafana_container}"
fi

probe_http() {
  run_with_smoke_deadline docker run --rm \
    --network "${runtime_network}" \
    "${probe_image}" \
    wget -q -O - -T "${http_timeout_seconds}" "$@"
}

grafana_curl() {
  curl --disable --config "${grafana_curl_config}" --noproxy 127.0.0.1 \
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
  fail "${description} 점검을 ${timeout_seconds}초 안에 완료하지 못했습니다."
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

resolve_grafana_datasource_uids() {
  local response
  local datasource_uids

  response="$(grafana_curl "${grafana_url}/api/datasources")" || return 1
  datasource_uids="$(python3 - "${response}" <<'PY'
import json
import re
import sys

datasources = json.loads(sys.argv[1])
if not isinstance(datasources, list):
    raise SystemExit(1)

uids = {}
for datasource in datasources:
    if not isinstance(datasource, dict):
        continue
    name = datasource.get("name")
    uid = datasource.get("uid")
    if name in ("Prometheus", "Loki") and isinstance(uid, str):
        if not re.fullmatch(r"[A-Za-z0-9_-]+", uid):
            raise SystemExit(1)
        if name in uids and uids[name] != uid:
            raise SystemExit(1)
        uids[name] = uid

for name in ("Prometheus", "Loki"):
    uid = uids.get(name)
    if not uid:
        raise SystemExit(1)
    print(uid)
PY
  )" || return 1
  grafana_prometheus_uid="$(printf '%s\n' "${datasource_uids}" | sed -n '1p')"
  grafana_loki_uid="$(printf '%s\n' "${datasource_uids}" | sed -n '2p')"
  [ -n "${grafana_prometheus_uid}" ] && [ -n "${grafana_loki_uid}" ]
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
  resolve_grafana_datasource_uids || return 1
  for uid in "${grafana_prometheus_uid}" "${grafana_loki_uid}"; do
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
  [ -n "${grafana_prometheus_uid}" ] || resolve_grafana_datasource_uids || return 1
  grafana_curl \
    "${grafana_url}/api/datasources/proxy/uid/${grafana_prometheus_uid}/api/v1/query?query=up%7Bjob%3D%22sugang-helper-app%22%7D" \
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
  [ -n "${grafana_loki_uid}" ] || resolve_grafana_datasource_uids || return 1
  query_url="$(build_loki_query_url "${marker_text}" "${grafana_url}/api/datasources/proxy/uid/${grafana_loki_uid}/loki/api/v1/query_range")"
  grafana_curl "${query_url}" >"${response_file}" \
    && parse_loki_marker "${response_file}" "${marker_text}"
}

wait_for "Loki HTTP 준비 상태" check_loki_ready
wait_for "Alloy HTTP 준비 상태" check_alloy_ready
wait_for "Prometheus HTTP 준비 상태" check_prometheus_ready
wait_for "실행 네트워크에서 앱 준비 상태" check_app_ready
wait_for "Grafana HTTP 상태" check_grafana_ready
wait_for "Prometheus 앱 수집 대상 활성 상태" check_prometheus_app_target
wait_for "Grafana의 Prometheus 데이터소스 상태" check_grafana_datasources
wait_for "Grafana의 Prometheus 쿼리 프록시" check_grafana_prometheus_query

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
  -c "printf '%s\\n' '${marker_text}'; sleep ${marker_lifetime_seconds}" \
  >/dev/null \
  || fail "Docker JSON 점검 로그 컨테이너를 시작하지 못했습니다."

wait_for "Docker JSON 로그의 Alloy·Loki 수집 경로" check_loki_marker
wait_for "Grafana의 Loki 점검 로그 조회" check_grafana_loki_marker

echo "모니터링 데이터 경로 점검을 통과했습니다. (marker=${marker_text})"
