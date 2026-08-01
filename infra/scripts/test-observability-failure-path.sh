#!/usr/bin/env bash
set -euo pipefail

# This file also acts as the docker/curl stub used by the test. The test creates
# temporary symlinks named docker and curl, so the production smoke script is
# exercised without touching a real observability stack.

script_name="$(basename "$0")"

fake_prometheus_response() {
  local value="$1"
  printf '{"status":"success","data":{"result":[{"metric":{"job":"sugang-helper-app"},"value":[0,"%s"]}]} }\n' "${value}"
}

fake_loki_response() {
  local marker="${1:-}"
  if [ -z "${marker}" ]; then
    printf '{"status":"success","data":{"result":[]}}\n'
    return
  fi
  printf '{"status":"success","data":{"result":[{"stream":{"job":"observability-smoke"},"values":[["0","%s"]]}]}}\n' "${marker}"
}

extract_marker() {
  python3 - "$1" <<'PY'
import re
import sys
from urllib.parse import unquote

match = re.search(r"observability-smoke-[0-9a-f]+", unquote(sys.argv[1]))
print(match.group(0) if match else "")
PY
}

fake_docker() {
  local mode="${OBSERVABILITY_FAILURE_MODE:-}"
  local subcommand="${1:-}"
  shift || true

  case "${subcommand}" in
    network)
      [ "${1:-}" = inspect ] || return 1
      return 0
      ;;
    image)
      [ "${1:-}" = inspect ] || return 1
      return 0
      ;;
    inspect)
      return 0
      ;;
    run)
      local detached=false
      local argument
      for argument in "$@"; do
        if [ "${argument}" = "-d" ]; then
          detached=true
          break
        fi
      done
      if [ "${detached}" = true ]; then
        printf 'fake-marker-container\n'
        return 0
      fi

      local url="${*: -1}"
      case "${url}" in
        *'/api/v1/query?query=up%7Bjob%3D%22sugang-helper-app%22%7D'*)
          if [ "${mode}" = prometheus-scrape ]; then
            fake_prometheus_response 0
          else
            fake_prometheus_response 1
          fi
          ;;
        *'/loki/api/v1/query_range?query='*)
          if [ "${mode}" = loki-push ]; then
            fake_loki_response
          else
            fake_loki_response "$(extract_marker "${url}")"
          fi
          ;;
        *)
          printf 'ok\n'
          ;;
      esac
      return 0
      ;;
    rm)
      return 0
      ;;
    *)
      printf 'unexpected fake docker command: %s\n' "${subcommand}" >&2
      return 1
      ;;
  esac
}

fake_curl() {
  local mode="${OBSERVABILITY_FAILURE_MODE:-}"
  local url="${*: -1}"

  case "${url}" in
    */api/health)
      printf '{"database":"ok"}\n'
      ;;
    */api/datasources/uid/*/health)
      if [ "${mode}" = datasource ]; then
        printf '{"status":"Error"}\n'
      else
        printf '{"status":"OK"}\n'
      fi
      ;;
    */api/datasources/proxy/uid/prometheus/api/v1/query*)
      fake_prometheus_response 1
      ;;
    */api/datasources/proxy/uid/loki/loki/api/v1/query_range*)
      if [ "${mode}" = loki-datasource ]; then
        fake_loki_response
      else
        fake_loki_response "$(extract_marker "${url}")"
      fi
      ;;
    *)
      printf 'unexpected fake curl URL: %s\n' "${url}" >&2
      return 1
      ;;
  esac
}

if [ "${script_name}" = docker ]; then
  fake_docker "$@"
  exit $?
fi
if [ "${script_name}" = curl ]; then
  fake_curl "$@"
  exit $?
fi

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
smoke_script="${repo_root}/infra/scripts/test-observability-smoke.sh"
temporary_dir="$(mktemp -d)"
stub_bin="${temporary_dir}/bin"
mkdir -p "${stub_bin}"
trap 'rm -rf -- "${temporary_dir}"' EXIT

ln -s "${repo_root}/infra/scripts/test-observability-failure-path.sh" "${stub_bin}/docker"
ln -s "${repo_root}/infra/scripts/test-observability-failure-path.sh" "${stub_bin}/curl"

run_failure_case() {
  local mode="$1"
  local expected="$2"
  local output
  local status

  set +e
  output="$({
    PATH="${stub_bin}:${PATH}" \
    OBSERVABILITY_FAILURE_MODE="${mode}" \
    OBSERVABILITY_SMOKE_TIMEOUT_SECONDS=3 \
    OBSERVABILITY_SMOKE_HTTP_TIMEOUT_SECONDS=1 \
    RUNTIME_NETWORK_NAME=sugang-helper-runtime \
    GRAFANA_CONTAINER_NAME=sugang-helper-grafana \
    GRAFANA_URL=http://127.0.0.1:3000 \
    GRAFANA_ADMIN_USER=admin \
    GRAFANA_ADMIN_PASSWORD=test-password \
      bash "${smoke_script}"
  } 2>&1)"
  status=$?
  set -e

  if [ "${status}" -eq 0 ]; then
    printf '%s failure injection unexpectedly passed\n' "${mode}" >&2
    printf '%s\n' "${output}" >&2
    exit 1
  fi
  if [[ "${output}" != *"${expected}"* ]]; then
    printf '%s failure injection produced an unexpected error\n' "${mode}" >&2
    printf 'expected: %s\nactual:\n%s\n' "${expected}" "${output}" >&2
    exit 1
  fi
  printf '%s failure injection failed closed\n' "${mode}"
}

run_failure_case prometheus-scrape "Prometheus app target up did not succeed within"
run_failure_case datasource "Grafana Prometheus datasource health did not succeed within"
run_failure_case loki-push "Docker JSON to Alloy to Loki marker round-trip did not succeed within"
run_failure_case loki-datasource "Grafana Loki datasource marker query did not succeed within"

printf 'observability failure-path contract passed\n'
