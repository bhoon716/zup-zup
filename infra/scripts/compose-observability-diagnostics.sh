#!/usr/bin/env bash
set -euo pipefail

# Persistent post-deploy observability operations. The release root is fixed
# because this wrapper is deployed only to the production release root.
readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"
readonly RUNTIME_ENV="${RELEASE_ROOT}/.env"
readonly COMPOSE_FILE="${RELEASE_ROOT}/docker-compose.yml"
diagnostics_env=""

fail() {
  echo "observability diagnostics failed: $*" >&2
  exit 1
}

cleanup() {
  [ -z "${diagnostics_env}" ] || rm -f -- "${diagnostics_env}"
}
trap cleanup EXIT

operation="${1:-}"
[ -n "${operation}" ] || fail "operation is required: ps, logs, probe, or start"
shift
case "${operation}" in
  ps|logs)
    ;;
  probe|start)
    [ "$#" -eq 0 ] || fail "${operation} does not accept extra arguments"
    ;;
  *)
    fail "unsupported operation: ${operation}"
    ;;
esac

[ -f "${RUNTIME_ENV}" ] || fail "persistent runtime environment is missing: ${RUNTIME_ENV}"
[ -f "${COMPOSE_FILE}" ] || fail "runtime Compose file is missing: ${COMPOSE_FILE}"
command -v docker >/dev/null || fail "docker is required"

diagnostics_env="$(mktemp "${RELEASE_ROOT}/.compose-diagnostics.XXXXXX")"
printf '%s\n' \
  "APP_BUILD_CONTEXT=${RELEASE_ROOT}" \
  "APP_ENV_FILE=${RELEASE_ROOT}/apps/server/.env" \
  "APP_PROD_CONFIG_PATH=${RELEASE_ROOT}/application-prod.yml" \
  "FIREBASE_CONFIG_PATH=${RELEASE_ROOT}/secrets/firebase-key.json" \
  >"${diagnostics_env}"
chmod 0600 "${diagnostics_env}"
export APP_BUILD_CONTEXT="${RELEASE_ROOT}"
export APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"
export APP_PROD_CONFIG_PATH="${RELEASE_ROOT}/application-prod.yml"
export FIREBASE_CONFIG_PATH="${RELEASE_ROOT}/secrets/firebase-key.json"

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
