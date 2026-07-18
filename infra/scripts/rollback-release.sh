#!/usr/bin/env bash
set -euo pipefail

readonly RELEASE_ROOT="/opt/jbnu-sugang-helper"
readonly RUNTIME_ENV="${RELEASE_ROOT}/.env.runtime"
readonly RELEASE_STATE="${RELEASE_ROOT}/.env.release"
readonly RELEASE_HISTORY="${RELEASE_ROOT}/.release-history"
readonly GHCR_USERNAME_FILE="${RELEASE_ROOT}/secrets/ghcr-read-username"
readonly GHCR_TOKEN_FILE="${RELEASE_ROOT}/secrets/ghcr-read-token"
readonly AUDIT_LOG="/var/log/jbnu-sugang-helper/deploy.log"
lock_file="${DEPLOY_LOCK_FILE:-/run/lock/jbnu-deploy.lock}"

sha="${1:-}"
stage="input"
release_env=""
state_updated=false

fail() {
  echo "rollback failed at ${stage}: $*" >&2
  exit 1
}

[ "$(id -u)" -eq 0 ] || fail "must run as root"
command -v flock >/dev/null || fail "flock is required"
install -d -o root -g root -m 0755 "$(dirname "${lock_file}")"
exec 9>"${lock_file}"
flock -n 9 || fail "another deployment is running"
[[ "${sha}" =~ ^[0-9a-f]{40}$ ]] || fail "rollback SHA must be exactly 40 lowercase hexadecimal characters"
[ -f "${RUNTIME_ENV}" ] || fail "root-only runtime environment is missing"
[ -f "${RELEASE_ROOT}/docker-compose.yml" ] || fail "active Compose file is missing"
[ -f "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh" ] \
  || fail "DB init support file is missing from the promoted runtime"

read_env_value() {
  local key="$1"
  awk -F= -v wanted="${key}" '$1 == wanted { sub(/^[^=]*=/, ""); print; exit }' "${RUNTIME_ENV}"
}

app_image_name="$(read_env_value APP_IMAGE_NAME)"
[ -n "${app_image_name}" ] || fail "APP_IMAGE_NAME is missing from runtime env"
[[ "${app_image_name}" == ghcr.io/* ]] || fail "APP_IMAGE_NAME must point to private GHCR"
api_host="$(read_env_value API_HOST)"
if [[ ! "${api_host}" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]]; then
  fail "API_HOST is missing or invalid"
fi
db_data_dir="$(read_env_value DB_DATA_DIR)"
[ -n "${db_data_dir}" ] || fail "DB_DATA_DIR is missing from runtime env"
mountpoint -q "${db_data_dir}" || fail "DB_DATA_DIR is not mounted"
db_source="$(findmnt -n -T "${db_data_dir}" -o SOURCE 2>/dev/null || true)"
[[ "${db_source}" =~ ^(/dev/|UUID=|LABEL=) ]] \
  || fail "DB_DATA_DIR is not backed by a block-volume device"
release_dir="${RELEASE_ROOT}/releases/${sha}"
[ -d "${release_dir}" ] || fail "rollback release is not retained: ${sha}"
[ -f "${release_dir}/application-prod.yml" ] || fail "rollback application config is missing"
[ -d "${release_dir}/src/main/resources/db/migration" ] || fail "rollback migration files are missing"

install -d -o root -g root -m 0750 "$(dirname "${AUDIT_LOG}")"
actor="${ROLLBACK_ACTOR:-unknown}"
log_event() {
  local result="$1"
  printf '%s actor=%s sha=%s stage=rollback-%s result=%s\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${actor}" "${sha}" "${stage}" "${result}" >>"${AUDIT_LOG}"
}

record_release_history() {
  local history_tmp="${RELEASE_HISTORY}.tmp.$$"
  {
    printf '%s\n' "${sha}"
    if [ -f "${RELEASE_HISTORY}" ]; then
      cat "${RELEASE_HISTORY}"
    fi
  } | awk '/^[0-9a-f]{40}$/ && !seen[$0]++ { if (count++ < 20) print }' >"${history_tmp}"
  chown root:root "${history_tmp}"
  chmod 0600 "${history_tmp}"
  mv -f "${history_tmp}" "${RELEASE_HISTORY}"
}

cleanup_on_exit() {
  local rc=$?
  [ -z "${release_env}" ] || rm -f -- "${release_env}" || true
  if [ "${rc}" -ne 0 ]; then
    log_event failure || true
  fi
}
trap cleanup_on_exit EXIT

release_env="${RELEASE_ROOT}/.env.rollback.$$"
cat >"${release_env}" <<EOF
APP_IMAGE_NAME=${app_image_name}
IMAGE_TAG=${sha}
APP_BUILD_CONTEXT=${release_dir}
APP_ENV_FILE=${RELEASE_ROOT}/.env.app
APP_PROD_CONFIG_PATH=${release_dir}/application-prod.yml
FIREBASE_CONFIG_PATH=${RELEASE_ROOT}/secrets/firebase-key.json
EOF
chmod 0600 "${release_env}"

compose=(docker compose --project-name sugang-helper --env-file "${RUNTIME_ENV}" --env-file "${release_env}" -f "${RELEASE_ROOT}/docker-compose.yml")
stage="compose-validate"
"${compose[@]}" config >/dev/null || fail "Compose config validation failed"

stage="image-preflight"
[ -f "${GHCR_USERNAME_FILE}" ] || fail "root-only GHCR username file is missing"
[ ! -L "${GHCR_USERNAME_FILE}" ] || fail "GHCR username file must not be a symlink"
ghcr_username="$(cat "${GHCR_USERNAME_FILE}" 2>/dev/null || true)"
[ -n "${ghcr_username}" ] || fail "root-only GHCR username is missing"
[ -f "${GHCR_TOKEN_FILE}" ] || fail "root-only GHCR token file is missing"
[ ! -L "${GHCR_TOKEN_FILE}" ] || fail "GHCR token file must not be a symlink"
docker login ghcr.io --username "${ghcr_username}" --password-stdin \
  <"${GHCR_TOKEN_FILE}" >/dev/null || fail "GHCR authentication failed"
"${compose[@]}" pull app || fail "rollback image pull failed"
docker image inspect "${app_image_name}:${sha}" >/dev/null || fail "rollback SHA image is missing"
command -v nginx >/dev/null || fail "host nginx is not installed"
nginx -t >/dev/null || fail "host nginx configuration is invalid"

# Validate compatibility without applying a down migration. A failed validate
# leaves the current app running because it occurs before the stop step.
stage="flyway-compatibility"
"${compose[@]}" --profile migration run --rm --no-deps migrate validate \
  || fail "rollback migration history is not compatible; current app remains running"

stage="stop-app"
"${compose[@]}" stop app >/dev/null 2>&1 || true

stage="start-app"
"${compose[@]}" up -d --no-deps app || fail "rollback app failed to start; manual recovery required"
container_id="$("${compose[@]}" ps -q app)"
[ -n "${container_id}" ] || fail "rollback app container was not created"
health=""
for _ in $(seq 1 60); do
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
  [ "${health}" = healthy ] && break
  case "${health}" in exited|dead|unhealthy) fail "rollback app is ${health}; manual recovery required";; esac
  sleep 2
done
[ "${health}" = healthy ] || fail "rollback app did not become healthy; manual recovery required"

stage="readiness"
curl --fail --silent --show-error --max-time 10 \
  http://127.0.0.1:8081/actuator/health/readiness >/dev/null \
  || fail "rollback readiness failed; manual recovery required"

stage="public-readiness"
curl --fail --silent --show-error --max-time 10 \
  "https://${api_host}/health/ready" >/dev/null \
  || fail "rollback public HTTPS readiness failed; manual recovery required"

stage="state-update"
state_tmp="${RELEASE_STATE}.tmp.$$"
{
  printf 'APP_IMAGE_NAME=%q\n' "${app_image_name}"
  printf 'IMAGE_TAG=%q\n' "${sha}"
  printf 'RELEASE_DIR=%q\n' "${release_dir}"
} >"${state_tmp}"
chown root:root "${state_tmp}"
chmod 0600 "${state_tmp}"
mv "${state_tmp}" "${RELEASE_STATE}"
state_updated=true
record_release_history

stage="cleanup"
mapfile -t keep_shas < <(head -n 3 "${RELEASE_HISTORY}" 2>/dev/null || true)
is_kept_release() {
  local candidate="$1"
  local kept
  for kept in "${keep_shas[@]}"; do
    [ "${kept}" = "${candidate}" ] && return 0
  done
  return 1
}
for release_candidate in "${RELEASE_ROOT}"/releases/*; do
  [ -d "${release_candidate}" ] || continue
  release_candidate_sha="${release_candidate##*/}"
  if [[ "${release_candidate_sha}" =~ ^[0-9a-f]{40}$ ]] \
    && ! is_kept_release "${release_candidate_sha}"; then
    rm -rf -- "${release_candidate}"
  fi
done
while IFS= read -r image; do
  [ -n "${image}" ] || continue
  tag="${image##*:}"
  if [[ "${tag}" =~ ^[0-9a-f]{40}$ ]] && ! is_kept_release "${tag}"; then
    docker image rm "${image}" >/dev/null 2>&1 || true
  fi
done < <(docker image ls "${app_image_name}" --format '{{.Repository}}:{{.Tag}}')

stage="complete"
log_event success
echo "rollback completed to ${sha}; keep recent 3 app images; no automatic DB rollback"
