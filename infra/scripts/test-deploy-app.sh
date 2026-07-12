#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="${repo_root}/infra/scripts/deploy-app.sh"
workdir="$(mktemp -d)"

cleanup() {
  rm -rf "${workdir}"
}
trap cleanup EXIT

mkdir -p "${workdir}/bin" "${workdir}/infra" "${workdir}/releases/old/build/libs" "${workdir}/releases/new/build/libs"
touch "${workdir}/releases/new/build/libs/app.jar" "${workdir}/releases/new/.env"
touch "${workdir}/releases/old/build/libs/app.jar" "${workdir}/releases/old/.env"

cat > "${workdir}/bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
state_file="${DEPLOY_TEST_STATE_FILE}"
log_file="${DEPLOY_TEST_LOG_FILE}"
printf '%s\n' "$*" >> "${log_file}"
if [ "$1" = "build" ]; then
  exit 0
fi
if [ "$1" = "compose" ] && [ "$2" = "up" ]; then
  if [ "${DEPLOY_TEST_FAIL_START:-0}" = "1" ] && [ "${APP_IMAGE_TAG}" = "new" ]; then
    exit 1
  fi
  printf '%s' "${APP_IMAGE_TAG}" > "${state_file}"
  exit 0
fi
if [ "$1" = "compose" ] && [ "$2" = "ps" ]; then
  cat "${state_file}"
  exit 0
fi
if [ "$1" = "inspect" ]; then
  if [ "$(cat "${state_file}")" = "new" ]; then
    printf 'unhealthy'
  else
    printf 'healthy'
  fi
  exit 0
fi
if [ "$1" = "logs" ]; then
  exit 0
fi
exit 0
EOF
chmod 755 "${workdir}/bin/docker"

cat > "${workdir}/bin/curl" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${DEPLOY_TEST_NOTIFICATION_FILE}"
EOF
chmod 755 "${workdir}/bin/curl"

cat > "${workdir}/state.env" <<EOF
RELEASE_SHA=old
RELEASE_DIR=${workdir}/releases/old
IMAGE_TAG=old
EOF
printf 'old' > "${workdir}/current"

set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=new \
RELEASE_DIR="${workdir}/releases/new" \
APP_HEALTH_MAX_ATTEMPTS=1 \
APP_HEALTH_WAIT_SECONDS=0 \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ]; then
  echo "unhealthy release unexpectedly succeeded" >&2
  exit 1
fi
if [ "$(cat "${workdir}/current")" != "old" ]; then
  echo "rollback did not restore the old release" >&2
  exit 1
fi
if ! grep -q 'old' "${workdir}/notification.log"; then
  echo "rollback notification did not include the previous release" >&2
  exit 1
fi

printf 'old' > "${workdir}/current"
set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DEPLOY_TEST_FAIL_START=1 \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=new \
RELEASE_DIR="${workdir}/releases/new" \
APP_HEALTH_MAX_ATTEMPTS=1 \
APP_HEALTH_WAIT_SECONDS=0 \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ] || [ "$(cat "${workdir}/current")" != "old" ]; then
  echo "container-start failure did not preserve the old release" >&2
  exit 1
fi

echo "deploy rollback verification passed"
