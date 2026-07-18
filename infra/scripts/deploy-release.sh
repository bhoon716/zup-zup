#!/usr/bin/env bash
set -euo pipefail

# Root-only release orchestrator. GitHub Actions can reach it only through the
# allowlisted /usr/local/sbin/jbnu-deploy wrapper.

readonly RELEASE_ROOT="/opt/jbnu-sugang-helper"
readonly STAGING_ROOT="/opt/jbnu-sugang-helper-staging"
readonly RUNTIME_ENV="${RELEASE_ROOT}/.env.runtime"
readonly RELEASE_STATE="${RELEASE_ROOT}/.env.release"
readonly RELEASE_HISTORY="${RELEASE_ROOT}/.release-history"
readonly MANIFEST_PUBLIC_KEY="${RELEASE_ROOT}/secrets/deploy-manifest-public.pem"
readonly GHCR_USERNAME_FILE="${RELEASE_ROOT}/secrets/ghcr-read-username"
readonly GHCR_TOKEN_FILE="${RELEASE_ROOT}/secrets/ghcr-read-token"
readonly AUDIT_LOG="/var/log/jbnu-sugang-helper/deploy.log"
lock_file="${DEPLOY_LOCK_FILE:-/run/lock/jbnu-deploy.lock}"

sha="${1:-}"
staging_dir="${2:-}"
stage="input"
release_dir=""
release_tmp=""
release_created=false
staging_locked=false
state_updated=false

fail() {
  echo "deploy failed at ${stage}: $*" >&2
  exit 1
}

if [ "$(id -u)" -ne 0 ]; then
  fail "must run as root"
fi
command -v flock >/dev/null || fail "flock is required"
install -d -o root -g root -m 0755 "$(dirname "${lock_file}")"
exec 9>"${lock_file}"
flock -n 9 || fail "another deployment is running"
if [[ ! "${sha}" =~ ^[0-9a-f]{40}$ ]]; then
  fail "release SHA must be exactly 40 lowercase hexadecimal characters"
fi
if [[ ! "${staging_dir}" =~ ^${STAGING_ROOT}/[0-9a-f]{40}$ ]] || [ "${staging_dir##*/}" != "${sha}" ]; then
  fail "staging directory is outside the fixed allowlist"
fi
if [ ! -f "${RUNTIME_ENV}" ]; then
  fail "root-only runtime environment is missing"
fi
if [ ! -f "${MANIFEST_PUBLIC_KEY}" ]; then
  fail "root-owned deploy manifest public key is missing"
fi
if [ ! -f "${staging_dir}/docker-compose.yml" ] \
  || [ ! -f "${staging_dir}/application-prod.yml" ] \
  || [ ! -d "${staging_dir}/src/main/resources/db/migration" ] \
  || [ ! -f "${staging_dir}/mysql/init/01-provision-service-accounts.sh" ] \
  || [ ! -f "${staging_dir}/loki/loki-config.yaml" ] \
  || [ ! -f "${staging_dir}/alloy/config.alloy" ] \
  || [ ! -f "${staging_dir}/grafana/provisioning/datasources/datasource.yml" ]; then
  fail "staging release is incomplete"
fi
if [ -L "${staging_dir}" ] || find -P "${staging_dir}" -type l -print -quit | grep -q .; then
  fail "staging release must not contain symbolic links"
fi
if ! find -P "${staging_dir}" -type f -o -type d | grep -q .; then
  fail "staging release is empty"
fi
if [ ! -f "${staging_dir}/SHA256SUMS" ] || [ ! -f "${staging_dir}/SHA256SUMS.sig" ]; then
  fail "staging manifest and signature are required"
fi

# The deploy account owns the upload directory. Lock it before any root-side
# validation or copy so the checked bytes cannot be replaced during execution.
stage="staging-lock"
chown -R root:root "${staging_dir}"
staging_locked=true
find -P "${staging_dir}" -type d -exec chmod 0750 {} +
find -P "${staging_dir}" -type f -exec chmod 0640 {} +

install -d -o root -g root -m 0750 "${RELEASE_ROOT}" "${RELEASE_ROOT}/releases"
install -d -o root -g root -m 0750 "$(dirname "${AUDIT_LOG}")"

actor="${DEPLOY_ACTOR:-unknown}"
log_event() {
  local result="$1"
  printf '%s actor=%s sha=%s stage=%s result=%s\n' \
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
  if [ "${rc}" -ne 0 ]; then
    [ -z "${release_tmp}" ] || rm -rf -- "${release_tmp}" || true
    if [ "${state_updated}" != true ] && [ "${release_created}" = true ]; then
      rm -rf -- "${release_dir}" || true
    fi
    if [ "${staging_locked}" = true ]; then
      rm -rf -- "${staging_dir}" || true
    fi
    log_event failure || true
  fi
}

trap cleanup_on_exit EXIT

stage="manifest"
openssl dgst -sha256 -verify "${MANIFEST_PUBLIC_KEY}" \
  -signature "${staging_dir}/SHA256SUMS.sig" "${staging_dir}/SHA256SUMS" \
  >/dev/null || fail "staging manifest signature is invalid"
(cd "${staging_dir}" && sha256sum --check SHA256SUMS) || fail "staging checksum mismatch"

read_env_value() {
  local key="$1"
  awk -F= -v wanted="${key}" '$1 == wanted { sub(/^[^=]*=/, ""); print; exit }' "${RUNTIME_ENV}"
}

app_image_name="$(read_env_value APP_IMAGE_NAME)"
[ -n "${app_image_name}" ] || fail "APP_IMAGE_NAME is missing from runtime env"
[[ "${app_image_name}" == ghcr.io/* ]] || fail "APP_IMAGE_NAME must point to private GHCR"
flyway_image="$(read_env_value FLYWAY_IMAGE)"
[[ "${flyway_image}" == *@sha256:* ]] || fail "FLYWAY_IMAGE must be digest-pinned"
min_free_percent="$(read_env_value MIN_FREE_PERCENT)"
min_free_percent="${min_free_percent:-20}"
if [[ ! "${min_free_percent}" =~ ^[1-9][0-9]?$|^100$ ]]; then
  fail "MIN_FREE_PERCENT is invalid"
fi
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
if [ -e "${release_dir}" ]; then
  fail "release directory already exists: ${release_dir}"
fi
release_tmp="${release_dir}.tmp.$$"
stage="release-staging"
install -d -o root -g root -m 0750 \
  "${release_tmp}/src/main/resources" \
  "${release_tmp}/mysql/init" \
  "${release_tmp}/loki" \
  "${release_tmp}/alloy" \
  "${release_tmp}/grafana"
cp -a "${staging_dir}/src/main/resources/db" "${release_tmp}/src/main/resources/"
cp -a "${staging_dir}/loki/." "${release_tmp}/loki/"
cp -a "${staging_dir}/alloy/." "${release_tmp}/alloy/"
cp -a "${staging_dir}/grafana/." "${release_tmp}/grafana/"
cp -a "${staging_dir}/application-prod.yml" "${release_tmp}/application-prod.yml"
cp -a "${staging_dir}/mysql/init/01-provision-service-accounts.sh" "${release_tmp}/mysql/init/"
cp -a "${staging_dir}/docker-compose.yml" "${release_tmp}/docker-compose.yml"
find "${release_tmp}/loki" "${release_tmp}/alloy" "${release_tmp}/grafana" \
  -type d -exec chmod 0755 {} +
find "${release_tmp}/loki" "${release_tmp}/alloy" "${release_tmp}/grafana" \
  -type f -exec chmod 0644 {} +
chmod 0644 "${release_tmp}/application-prod.yml"
chmod 0644 "${release_tmp}/mysql/init/01-provision-service-accounts.sh"
chmod 0644 "${release_tmp}/docker-compose.yml"
mv "${release_tmp}" "${release_dir}"
release_created=true

release_env="${release_dir}/.env.compose"
stage="release-contract"
cat >"${release_env}" <<EOF
APP_IMAGE_NAME=${app_image_name}
IMAGE_TAG=${sha}
APP_BUILD_CONTEXT=${release_dir}
APP_ENV_FILE=${RELEASE_ROOT}/.env.app
APP_PROD_CONFIG_PATH=${release_dir}/application-prod.yml
FIREBASE_CONFIG_PATH=${RELEASE_ROOT}/secrets/firebase-key.json
EOF
chmod 0600 "${release_env}"

compose=(docker compose --project-name sugang-helper --env-file "${RUNTIME_ENV}" --env-file "${release_env}" -f "${release_dir}/docker-compose.yml")
stage="compose-validate"
"${compose[@]}" --profile migration --profile observability config >/dev/null \
  || fail "Compose config validation failed"

stage="disk-preflight"
available_percent="$(df -P "${RELEASE_ROOT}" | awk 'NR == 2 {gsub(/%/, "", $5); print 100 - $5}')"
if [ -z "${available_percent}" ] || [ "${available_percent}" -lt "${min_free_percent}" ]; then
  fail "disk free space is below MIN_FREE_PERCENT"
fi

stage="infra-preflight"
"${compose[@]}" pull db redis || fail "MySQL/Redis image preflight failed"
"${compose[@]}" --profile migration pull migrate || fail "Flyway image preflight failed"
"${compose[@]}" --profile observability pull loki alloy grafana \
  || fail "Loki/Alloy/Grafana image preflight failed"
"${compose[@]}" --profile observability up -d db redis loki alloy grafana \
  || fail "MySQL/Redis/Loki/Alloy/Grafana startup failed"

wait_healthy() {
  local service="$1"
  local container_id
  local compose_for_service=("${compose[@]}")
  case "${service}" in
    loki|alloy|grafana) compose_for_service+=(--profile observability) ;;
  esac
  container_id="$("${compose_for_service[@]}" ps -q "${service}")"
  [ -n "${container_id}" ] || fail "${service} container was not created"
  for _ in $(seq 1 60); do
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    [ "${health}" = healthy ] && return 0
    case "${health}" in exited|dead|unhealthy) fail "${service} is ${health}";; esac
    sleep 2
  done
  fail "${service} did not become healthy"
}

wait_healthy db
wait_healthy redis
wait_healthy loki
wait_healthy alloy
wait_healthy grafana

stage="db-service-accounts"
"${compose[@]}" exec -T db bash /docker-entrypoint-initdb.d/01-provision-service-accounts.sh \
  || fail "DB service-account provisioning failed"

stage="app-image-preflight"
[ -f "${GHCR_USERNAME_FILE}" ] || fail "root-only GHCR username file is missing"
[ ! -L "${GHCR_USERNAME_FILE}" ] || fail "GHCR username file must not be a symlink"
ghcr_username="$(cat "${GHCR_USERNAME_FILE}" 2>/dev/null || true)"
[ -n "${ghcr_username}" ] || fail "root-only GHCR username is missing"
[ -f "${GHCR_TOKEN_FILE}" ] || fail "root-only GHCR token file is missing"
[ ! -L "${GHCR_TOKEN_FILE}" ] || fail "GHCR token file must not be a symlink"
docker login ghcr.io --username "${ghcr_username}" --password-stdin \
  <"${GHCR_TOKEN_FILE}" >/dev/null || fail "GHCR authentication failed"
for attempt in $(seq 1 6); do
  if "${compose[@]}" pull app; then
    break
  fi
  if [ "${attempt}" -eq 6 ]; then
    fail "application image pull failed after ${attempt} attempts"
  fi
  sleep 5
done
docker image inspect "${app_image_name}:${sha}" >/dev/null || fail "application SHA image is missing"

stage="promote-runtime-files"
install -d -o root -g root -m 0750 "${RELEASE_ROOT}/mysql" "${RELEASE_ROOT}/mysql/init"
install -d -o root -g root -m 0750 \
  "${RELEASE_ROOT}/loki" \
  "${RELEASE_ROOT}/alloy" \
  "${RELEASE_ROOT}/grafana"
install -o root -g root -m 0644 "${staging_dir}/docker-compose.yml" \
  "${RELEASE_ROOT}/docker-compose.yml.tmp.$$"
mv "${RELEASE_ROOT}/docker-compose.yml.tmp.$$" "${RELEASE_ROOT}/docker-compose.yml"
cp -a "${release_dir}/loki/." "${RELEASE_ROOT}/loki/"
cp -a "${release_dir}/alloy/." "${RELEASE_ROOT}/alloy/"
cp -a "${release_dir}/grafana/." "${RELEASE_ROOT}/grafana/"
install -o root -g root -m 0644 "${staging_dir}/mysql/init/01-provision-service-accounts.sh" \
  "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh.tmp.$$"
mv "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh.tmp.$$" \
  "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh"

stage="stop-app"
"${compose[@]}" stop app >/dev/null 2>&1 || true

stage="flyway-migrate"
"${compose[@]}" --profile migration run --rm --no-deps migrate migrate || fail "Flyway migrate failed; app remains stopped"

stage="app-start"
"${compose[@]}" up -d --no-deps app || fail "application container failed to start"
wait_healthy app

stage="readiness"
curl --fail --silent --show-error --max-time 10 \
  http://127.0.0.1:8081/actuator/health/readiness >/dev/null \
  || fail "application readiness failed"

stage="public-readiness"
curl --fail --silent --show-error --max-time 10 \
  "https://${api_host}/health/ready" >/dev/null \
  || fail "public HTTPS readiness failed"

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
rm -rf -- "${staging_dir}" || true

stage="complete"
log_event success
echo "deployed ${sha}; keep recent 3 app images"
