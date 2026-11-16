#!/usr/bin/env bash
set -euo pipefail

readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"
readonly RUNTIME_ENV="${RELEASE_ROOT}/.env"
readonly COMPOSE_FILE="${RELEASE_ROOT}/docker-compose.yml"
diagnostics_env=""

fail() {
  echo "모니터링 진단에 실패했습니다: $*" >&2
  exit 1
}

cleanup() {
  [ -z "${diagnostics_env}" ] || rm -f -- "${diagnostics_env}"
}
trap cleanup EXIT

operation="${1:-}"
[ -n "${operation}" ] || fail "ps, logs, probe, start 중 하나를 지정해야 합니다."
shift
case "${operation}" in
  ps|logs)
    ;;
  probe|start)
    [ "$#" -eq 0 ] || fail "${operation} 명령에는 추가 인수를 지정할 수 없습니다."
    ;;
  *)
    fail "지원하지 않는 명령입니다: ${operation}"
    ;;
esac

[ -f "${RUNTIME_ENV}" ] || fail "실행 환경 설정 파일이 없습니다: ${RUNTIME_ENV}"
[ -f "${COMPOSE_FILE}" ] || fail "실행 환경의 Compose 파일이 없습니다: ${COMPOSE_FILE}"
command -v docker >/dev/null || fail "Docker가 필요합니다."

diagnostics_env="$(mktemp "${RELEASE_ROOT}/.compose-diagnostics.XXXXXX")"
printf '%s\n' \
  "APP_BUILD_CONTEXT=${RELEASE_ROOT}" \
  "APP_ENV_FILE=${RELEASE_ROOT}/apps/server/.env" \
  "APP_PROD_CONFIG_PATH=${RELEASE_ROOT}/application-prod.yml" \
  >"${diagnostics_env}"
chmod 0600 "${diagnostics_env}"
export APP_BUILD_CONTEXT="${RELEASE_ROOT}"
export APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"
export APP_PROD_CONFIG_PATH="${RELEASE_ROOT}/application-prod.yml"

compose=(
  docker compose
  --project-name sugang-helper
  --env-file "${RELEASE_ROOT}/.env"
  --env-file "${diagnostics_env}"
  -f "${RELEASE_ROOT}/docker-compose.yml"
  --profile observability
)

case "${operation}" in
  ps)
    "${compose[@]}" ps --all "$@"
    ;;
  logs)
    "${compose[@]}" logs "$@"
    ;;
  probe)
    "${compose[@]}" run --rm --no-deps observability-probe-tools
    ;;
  start)
    "${compose[@]}" up -d --wait --wait-timeout 180 loki alloy prometheus grafana
    ;;
esac
