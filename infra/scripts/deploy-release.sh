#!/usr/bin/env bash
set -euo pipefail

# Ubuntu-owned deploy entrypoint. The workflow copies this file to the fixed
# release root and executes it over SSH; no privileged wrapper is needed.
readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"
readonly STAGING_ROOT="${RELEASE_ROOT}/.staging"
readonly RUNTIME_ENV="${RELEASE_ROOT}/.env"
readonly APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"

sha="${1:-}"
staging_dir="${2:-}"
stage="input"
compose_env=""
runtime_env=""
runtime_env_tmp=""
app_quiesced=false

fail() {
  echo "deploy failed at ${stage}: $*" >&2
  exit 1
}

cleanup() {
  local rc=$?
  [ -z "${compose_env}" ] || rm -f -- "${compose_env}"
  [ -z "${runtime_env_tmp}" ] || rm -f -- "${runtime_env_tmp}"
  if [ "${rc}" -ne 0 ] && [ "${app_quiesced:-false}" = true ] && [ -f "${runtime_env}" ]; then
    docker compose --project-name sugang-helper \
      --env-file "${RUNTIME_ENV}" \
      --env-file "${runtime_env}" \
      -f "${RELEASE_ROOT}/docker-compose.yml" stop app >/dev/null 2>&1 || true
  fi
  [ -z "${runtime_env}" ] || rm -f -- "${runtime_env}"
  if [ -n "${staging_dir}" ] && [[ "${staging_dir}" =~ ^${STAGING_ROOT}/[0-9a-f]{40}$ ]]; then
    rm -rf -- "${staging_dir}" || true
  fi
  if [ "${rc}" -ne 0 ]; then
    echo "deploy failed at ${stage}; app remains stopped after migration failure or startup failure" >&2
  fi
}
trap cleanup EXIT

if [[ ! "${sha}" =~ ^[0-9a-f]{40}$ ]]; then
  fail "release SHA must be exactly 40 lowercase hexadecimal characters"
fi
if [[ ! "${staging_dir}" =~ ^${STAGING_ROOT}/[0-9a-f]{40}$ ]] \
  || [ "${staging_dir##*/}" != "${sha}" ]; then
  fail "staging directory is outside the fixed allowlist"
fi
if [ ! -f "${RUNTIME_ENV}" ]; then
  fail "${RUNTIME_ENV} is missing; bootstrap the Ubuntu runtime first"
fi
if [ ! -f "${staging_dir}/docker-compose.yml" ] \
  || [ ! -f "${staging_dir}/application-prod.yml" ] \
  || [ ! -f "${staging_dir}/apps/server/.env" ] \
  || [ ! -d "${staging_dir}/src/main/resources/db/migration" ] \
  || [ ! -f "${staging_dir}/mysql/init/01-provision-service-accounts.sh" ] \
  || [ ! -f "${staging_dir}/loki/loki-config.yaml" ] \
  || [ ! -f "${staging_dir}/alloy/config.alloy" ] \
  || [ ! -f "${staging_dir}/prometheus/prometheus.yml" ] \
  || [ ! -f "${staging_dir}/grafana/provisioning/datasources/datasource.yml" ]; then
  fail "staging release is incomplete"
fi
if [ -L "${staging_dir}" ] || find -P "${staging_dir}" -type l -print -quit | grep -q .; then
  fail "staging release must not contain symbolic links"
fi

command -v docker >/dev/null || fail "docker is required for Ubuntu deploys"
mkdir -p "${RELEASE_ROOT}"

read_env_value() {
  local key="$1"
  awk -F= -v wanted="${key}" '$1 == wanted { sub(/^[^=]*=/, ""); print; exit }' "${RUNTIME_ENV}"
}

app_image_name="$(read_env_value APP_IMAGE_NAME)"
flyway_image="$(read_env_value FLYWAY_IMAGE)"
if [[ "${app_image_name}" != ghcr.io/* ]]; then
  fail "APP_IMAGE_NAME must point to GHCR"
fi
if [[ "${flyway_image}" != *@sha256:* ]]; then
  fail "FLYWAY_IMAGE must be digest-pinned"
fi

stage="staging-compose-validate"
compose_env="${RELEASE_ROOT}/.env.compose.${sha}.$$"
cat >"${compose_env}" <<EOF
APP_IMAGE_NAME=${app_image_name}
IMAGE_TAG=${sha}
APP_BUILD_CONTEXT=${staging_dir}
APP_ENV_FILE=${staging_dir}/apps/server/.env
APP_PROD_CONFIG_PATH=${staging_dir}/application-prod.yml
FIREBASE_CONFIG_PATH=${RELEASE_ROOT}/secrets/firebase-key.json
EOF
chmod 0600 "${compose_env}"
compose_stage=(docker compose --project-name sugang-helper --env-file "${RUNTIME_ENV}" --env-file "${compose_env}" -f "${staging_dir}/docker-compose.yml")
"${compose_stage[@]}" --profile migration --profile observability config >/dev/null \
  || fail "staging Compose config validation failed"

stage="promote-runtime-files"
mkdir -p \
  "${RELEASE_ROOT}/apps/server" \
  "${RELEASE_ROOT}/src/main/resources/db" \
  "${RELEASE_ROOT}/mysql/init" \
  "${RELEASE_ROOT}/loki" \
  "${RELEASE_ROOT}/alloy" \
  "${RELEASE_ROOT}/prometheus" \
  "${RELEASE_ROOT}/grafana"
cp "${staging_dir}/docker-compose.yml" "${RELEASE_ROOT}/docker-compose.yml.tmp.$$"
mv -f "${RELEASE_ROOT}/docker-compose.yml.tmp.$$" "${RELEASE_ROOT}/docker-compose.yml"
cp "${staging_dir}/application-prod.yml" "${RELEASE_ROOT}/application-prod.yml.tmp.$$"
mv -f "${RELEASE_ROOT}/application-prod.yml.tmp.$$" "${RELEASE_ROOT}/application-prod.yml"
cp "${staging_dir}/mysql/init/01-provision-service-accounts.sh" \
  "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh.tmp.$$"
mv -f "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh.tmp.$$" \
  "${RELEASE_ROOT}/mysql/init/01-provision-service-accounts.sh"
cp -a "${staging_dir}/src/main/resources/db/." "${RELEASE_ROOT}/src/main/resources/db/"
cp -a "${staging_dir}/loki/." "${RELEASE_ROOT}/loki/"
cp -a "${staging_dir}/alloy/." "${RELEASE_ROOT}/alloy/"
cp -a "${staging_dir}/prometheus/." "${RELEASE_ROOT}/prometheus/"
cp -a "${staging_dir}/grafana/." "${RELEASE_ROOT}/grafana/"
cp "${staging_dir}/apps/server/.env" "${APP_ENV_FILE}.tmp.$$"
chmod 0600 "${APP_ENV_FILE}.tmp.$$"
mv -f "${APP_ENV_FILE}.tmp.$$" "${APP_ENV_FILE}"

runtime_env="${RELEASE_ROOT}/.compose-runtime.${sha}.$$"
runtime_env_tmp="${runtime_env}.tmp.$$"
cat >"${runtime_env_tmp}" <<EOF
APP_IMAGE_NAME=${app_image_name}
IMAGE_TAG=${sha}
APP_BUILD_CONTEXT=${RELEASE_ROOT}
APP_ENV_FILE=${APP_ENV_FILE}
APP_PROD_CONFIG_PATH=${RELEASE_ROOT}/application-prod.yml
FIREBASE_CONFIG_PATH=${RELEASE_ROOT}/secrets/firebase-key.json
EOF
chmod 0600 "${runtime_env_tmp}"
mv -f "${runtime_env_tmp}" "${runtime_env}"
runtime_env_tmp=""
compose=(docker compose --project-name sugang-helper --env-file "${RUNTIME_ENV}" --env-file "${runtime_env}" -f "${RELEASE_ROOT}/docker-compose.yml")
"${compose[@]}" --profile migration --profile observability config >/dev/null \
  || fail "runtime Compose config validation failed"

stage="infra-start"
"${compose[@]}" --profile observability up -d --wait --wait-timeout 180 db redis loki alloy prometheus grafana \
  || fail "MySQL/Redis/Loki/Alloy/Prometheus/Grafana startup failed"

stage="app-image-pull"
"${compose[@]}" pull app || fail "application image pull failed"

stage="stop-app"
"${compose[@]}" stop app >/dev/null 2>&1 || true
app_quiesced=true

stage="flyway-migrate"
"${compose[@]}" --profile migration run --rm --no-deps migrate migrate \
  || fail "Flyway migrate failed; app remains stopped"

stage="app-start"
"${compose[@]}" up -d --no-deps --wait --wait-timeout 180 app \
  || fail "application container failed to start"

stage="readiness"
curl --fail --silent --show-error --max-time 10 \
  http://127.0.0.1:8081/actuator/health/readiness >/dev/null \
  || fail "application readiness failed"

stage="complete"
echo "deployed ${sha} with Ubuntu SSH-only deploy"
