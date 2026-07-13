#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="${repo_root}/infra/scripts/deploy-app.sh"
compose_policy_script="${repo_root}/infra/scripts/verify-compose-policy.sh"
workdir="$(mktemp -d)"

cleanup() {
  rm -rf "${workdir}"
}
trap cleanup EXIT

mkdir -p \
  "${workdir}/bin" \
  "${workdir}/infra" \
  "${workdir}/releases/old/build/libs" \
  "${workdir}/releases/new/build/libs" \
  "${workdir}/releases/new/src/main/resources/db/migration" \
  "${workdir}/releases/incomplete/build/libs" \
  "${workdir}/releases/forbidden/build/libs" \
  "${workdir}/releases/forbidden/src/main/resources/db/migration"
touch "${workdir}/releases/new/build/libs/app.jar" "${workdir}/releases/new/.env"
touch "${workdir}/releases/old/build/libs/app.jar" "${workdir}/releases/old/.env"
touch "${workdir}/releases/new/src/main/resources/db/migration/V1__initial.sql"
touch "${workdir}/releases/incomplete/build/libs/app.jar" "${workdir}/releases/incomplete/.env"
touch "${workdir}/releases/forbidden/build/libs/app.jar" "${workdir}/releases/forbidden/src/main/resources/db/migration/V1__initial.sql"
printf '%s\n' 'SPRING_FLYWAY_URL=jdbc:mysql://root:must-not-reach-runtime@db:3306/sugang_helper' > "${workdir}/releases/forbidden/.env"

cat > "${workdir}/bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
state_file="${DEPLOY_TEST_STATE_FILE}"
log_file="${DEPLOY_TEST_LOG_FILE}"
printf '%s\n' "$*" >> "${log_file}"
if [ "$1" = "build" ]; then
  exit 0
fi
if [ "$1" = "compose" ] && [[ " $* " == *" exec "* ]] && [[ " $* " == *" db "* ]]; then
  if [ "${DEPLOY_TEST_PROVISION_FAIL:-0}" = "1" ]; then
    exit 1
  fi
  printf '%s\n' "provision-db-users" >> "${log_file}"
  exit 0
fi
if [ "$1" = "compose" ] && [[ " $* " == *" run "* ]] && [[ " $* " == *" migrate"* ]]; then
  if [ "${DEPLOY_TEST_MIGRATION_FAIL:-0}" = "1" ]; then
    exit 1
  fi
  printf '%s\n' "run-migrate" >> "${log_file}"
  exit 0
fi
if [ "$1" = "compose" ] && [ "$2" = "stop" ]; then
  service="${!#}"
  printf '%s\n' "stop-${service}" >> "${log_file}"
  exit 0
fi
if [ "$1" = "compose" ] && [ "$2" = "up" ]; then
  service="${!#}"
  if [ "${DEPLOY_TEST_FAIL_START:-0}" = "1" ] && [ "${service}" = "app" ] && [ "${APP_IMAGE_TAG}" = "new" ]; then
    exit 1
  fi
  if [ "${service}" = "app" ]; then
    printf '%s' "${APP_IMAGE_TAG}" > "${state_file}"
  fi
  if [ "${service}" = "redis" ]; then
    printf '%s' "redis" > "${state_file}.redis"
  fi
  if [ "${service}" = "db" ]; then
    printf '%s' "db" > "${state_file}.db"
  fi
  exit 0
fi
if [ "$1" = "compose" ] && [ "$2" = "ps" ]; then
  service="${!#}"
  if [ "${service}" = "redis" ]; then
    cat "${state_file}.redis" 2>/dev/null || true
  elif [ "${service}" = "db" ]; then
    cat "${state_file}.db" 2>/dev/null || true
  else
    cat "${state_file}"
  fi
  exit 0
fi
if [ "$1" = "inspect" ]; then
  target="${!#}"
  if [[ "$*" == *'.Config.Image'* ]]; then
    printf 'sugang-helper-app:%s' "$(cat "${state_file}")"
    exit 0
  fi
  if [ "${target}" = "redis" ] && [ "${DEPLOY_TEST_REDIS_UNHEALTHY:-0}" = "1" ]; then
    printf 'unhealthy'
  elif [ "${target}" = "db" ] && [ "${DEPLOY_TEST_DB_UNHEALTHY:-0}" = "1" ]; then
    printf 'unhealthy'
  elif [ "${target}" != "redis" ] && [ "$(cat "${state_file}")" = "new" ] \
    && [ "${DEPLOY_TEST_APP_UNHEALTHY:-0}" = "1" ]; then
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

malicious_runtime_env="${workdir}/malicious-runtime.env"
cp "${repo_root}/apps/server/.env.example" "${malicious_runtime_env}"
printf '%s\n' 'SPRING_FLYWAY_URL=jdbc:mysql://root:must-not-reach-runtime@db:3306/sugang_helper' >> "${malicious_runtime_env}"
set +e
COMPOSE_ENV_FILE="${repo_root}/infra/.env.example" \
APP_ENV_FILE="${malicious_runtime_env}" \
bash "${compose_policy_script}" "${repo_root}/infra/docker-compose.yml" > "${workdir}/malicious-runtime-env.out" 2>&1
status=$?
set -e

if [ "${status}" -eq 0 ] \
  || ! grep -q 'app must not receive SPRING_FLYWAY_URL' "${workdir}/malicious-runtime-env.out"; then
  echo "Compose policy accepted a Flyway JDBC URL from the runtime environment file" >&2
  exit 1
fi

: > "${workdir}/docker.log"
: > "${workdir}/notification.log"
set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=incomplete \
RELEASE_DIR="${workdir}/releases/incomplete" \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ] || [ -s "${workdir}/docker.log" ]; then
  echo "release without migration SQL reached Docker deployment commands" >&2
  exit 1
fi

: > "${workdir}/docker.log"
set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=forbidden \
RELEASE_DIR="${workdir}/releases/forbidden" \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ] || [ -s "${workdir}/docker.log" ]; then
  echo "release .env with a Flyway JDBC URL reached Docker deployment commands" >&2
  exit 1
fi

mkdir "${workdir}/infra/.operation-lock"
: > "${workdir}/docker.log"
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
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}" > "${workdir}/lock.out" 2>&1
status=$?
set -e
rmdir "${workdir}/infra/.operation-lock"

if [ "${status}" -eq 0 ] \
  || grep -F -- 'build -t' "${workdir}/docker.log" >/dev/null \
  || grep -F -- 'run-migrate' "${workdir}/docker.log" >/dev/null \
  || ! grep -q 'operation holds' "${workdir}/lock.out"; then
  echo "operation lock did not block deployment before build and migration" >&2
  exit 1
fi

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
DEPLOY_TEST_APP_UNHEALTHY=1 \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ]; then
  echo "unhealthy release unexpectedly succeeded" >&2
  exit 1
fi
if [ "$(cat "${workdir}/current")" != "new" ]; then
  echo "post-migration health failure unexpectedly rolled back to the old image" >&2
  exit 1
fi
if ! grep -F -- 'stop-app' "${workdir}/docker.log" >/dev/null \
  || ! grep -F -- 'run-migrate' "${workdir}/docker.log" >/dev/null; then
  echo "app was not quiesced before schema migration" >&2
  exit 1
fi
if ! grep -qi 'manual' "${workdir}/notification.log"; then
  echo "post-migration health failure did not require manual recovery" >&2
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
  echo "container-start failure did not keep the previous image state" >&2
  exit 1
fi
if ! grep -qi 'manual' "${workdir}/notification.log"; then
  echo "post-migration container-start failure did not require manual recovery" >&2
  exit 1
fi

printf 'old' > "${workdir}/current"
: > "${workdir}/docker.log"
: > "${workdir}/notification.log"
set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DEPLOY_TEST_REDIS_UNHEALTHY=1 \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=new \
RELEASE_DIR="${workdir}/releases/new" \
APP_HEALTH_MAX_ATTEMPTS=1 \
APP_HEALTH_WAIT_SECONDS=0 \
REDIS_HEALTH_MAX_ATTEMPTS=1 \
REDIS_HEALTH_WAIT_SECONDS=0 \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ] || [ "$(cat "${workdir}/current")" != "old" ]; then
  echo "unhealthy Redis did not block the app deployment" >&2
  exit 1
fi
if ! grep -F -- 'stop-app' "${workdir}/docker.log" >/dev/null; then
  echo "Redis failure did not occur after app quiescence" >&2
  exit 1
fi
if grep -F -- '--no-deps app' "${workdir}/docker.log" >/dev/null; then
  echo "app deployment ran after the Redis preflight failed" >&2
  exit 1
fi
if ! grep -qi 'Redis health' "${workdir}/notification.log"; then
  echo "Redis preflight failure did not notify the deployment channel" >&2
  exit 1
fi

printf 'old' > "${workdir}/current"
: > "${workdir}/docker.log"
: > "${workdir}/notification.log"
set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DEPLOY_TEST_DB_UNHEALTHY=1 \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=new \
RELEASE_DIR="${workdir}/releases/new" \
APP_HEALTH_MAX_ATTEMPTS=1 \
APP_HEALTH_WAIT_SECONDS=0 \
DB_HEALTH_MAX_ATTEMPTS=1 \
DB_HEALTH_WAIT_SECONDS=0 \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ] || [ "$(cat "${workdir}/current")" != "old" ]; then
  echo "unhealthy DB did not block the app deployment" >&2
  exit 1
fi
if grep -F -- '--no-deps app' "${workdir}/docker.log" >/dev/null \
  || grep -F -- 'run-migrate' "${workdir}/docker.log" >/dev/null; then
  echo "DB failure reached migration or app deployment" >&2
  exit 1
fi
if ! grep -qi 'DB health' "${workdir}/notification.log"; then
  echo "DB preflight failure did not notify the deployment channel" >&2
  exit 1
fi

printf 'old' > "${workdir}/current"
: > "${workdir}/docker.log"
: > "${workdir}/notification.log"
set +e
DEPLOY_STATE_DIR="${workdir}" \
DEPLOY_STATE_FILE="${workdir}/state.env" \
DEPLOY_TEST_STATE_FILE="${workdir}/current" \
DEPLOY_TEST_LOG_FILE="${workdir}/docker.log" \
DEPLOY_TEST_NOTIFICATION_FILE="${workdir}/notification.log" \
DEPLOY_TEST_MIGRATION_FAIL=1 \
DOCKER_BIN="${workdir}/bin/docker" \
CURL_BIN="${workdir}/bin/curl" \
COMPOSE_DIR="${workdir}/infra" \
RELEASE_SHA=new \
RELEASE_DIR="${workdir}/releases/new" \
APP_HEALTH_MAX_ATTEMPTS=1 \
APP_HEALTH_WAIT_SECONDS=0 \
DB_HEALTH_MAX_ATTEMPTS=1 \
DB_HEALTH_WAIT_SECONDS=0 \
DEPLOYMENT_DISCORD_WEBHOOK_URL=https://example.test/webhook \
bash "${script}"
status=$?
set -e

if [ "${status}" -eq 0 ] || [ "$(cat "${workdir}/current")" != "old" ]; then
  echo "migration failure did not preserve the old release" >&2
  exit 1
fi
if ! grep -F -- 'stop-app' "${workdir}/docker.log" >/dev/null; then
  echo "migration failure did not quiesce the previous app" >&2
  exit 1
fi
if grep -F -- '--no-deps app' "${workdir}/docker.log" >/dev/null; then
  echo "app deployment ran after the migration failed" >&2
  exit 1
fi
if ! grep -F -- 'provision-db-users' "${workdir}/docker.log" >/dev/null; then
  echo "migration ran without provisioning DB users first" >&2
  exit 1
fi
if ! grep -qi 'migration' "${workdir}/notification.log"; then
  echo "migration failure did not notify the deployment channel" >&2
  exit 1
fi

echo "deploy migration-safety verification passed"
