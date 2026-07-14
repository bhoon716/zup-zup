#!/usr/bin/env bash
set -euo pipefail

docker_bin="${DOCKER_BIN:-docker}"
curl_bin="${CURL_BIN:-curl}"
compose_dir="${COMPOSE_DIR:-$HOME/jbnu-sugang-helper/infra}"
operation_lock_dir="${OPERATION_LOCK_DIR:-${compose_dir}/.operation-lock}"
state_dir="${DEPLOY_STATE_DIR:-$HOME/jbnu-sugang-helper/deploy-state}"
state_file="${DEPLOY_STATE_FILE:-${state_dir}/last-known-good.env}"
releases_dir="${RELEASES_DIR:-$HOME/jbnu-sugang-helper/releases}"
release_sha="${RELEASE_SHA:?RELEASE_SHA is required}"
release_dir="${RELEASE_DIR:?RELEASE_DIR is required}"
health_attempts="${APP_HEALTH_MAX_ATTEMPTS:-90}"
health_wait_seconds="${APP_HEALTH_WAIT_SECONDS:-5}"
db_health_attempts="${DB_HEALTH_MAX_ATTEMPTS:-30}"
db_health_wait_seconds="${DB_HEALTH_WAIT_SECONDS:-2}"
redis_health_attempts="${REDIS_HEALTH_MAX_ATTEMPTS:-30}"
redis_health_wait_seconds="${REDIS_HEALTH_WAIT_SECONDS:-2}"
webhook_url="${DEPLOYMENT_DISCORD_WEBHOOK_URL:-}"
app_image_name="${APP_IMAGE_NAME:-sugang-helper-app}"
operation_lock_acquired=false

if [ ! -f "${release_dir}/build/libs/app.jar" ] \
  || [ ! -f "${release_dir}/.env" ] \
  || [ ! -d "${release_dir}/src/main/resources/db/migration" ] \
  || ! compgen -G "${release_dir}/src/main/resources/db/migration/V*__*.sql" > /dev/null; then
  echo "Incomplete release ${release_sha}" >&2
  exit 1
fi

validate_release_runtime_env() {
  local release_env="$1"
  local forbidden_prefix
  local forbidden_variable
  local forbidden_prefixes=(
    SPRING_DATASOURCE_
    SPRING_FLYWAY_
    DB_
    MYSQL_
    FLYWAY_
  )

  for forbidden_prefix in "${forbidden_prefixes[@]}"; do
    forbidden_variable="$(grep -E "^${forbidden_prefix}[A-Za-z0-9_]*=" "${release_env}" | head -n 1 | cut -d= -f1 || true)"
    if [ -n "${forbidden_variable}" ]; then
      echo "Release .env must not contain infrastructure DB credentials: ${forbidden_variable}" >&2
      return 1
    fi
  done
}

if ! validate_release_runtime_env "${release_dir}/.env"; then
  exit 1
fi

acquire_operation_lock() {
  if mkdir "${operation_lock_dir}" 2>/dev/null; then
    operation_lock_acquired=true
    return 0
  fi
  echo "another deployment, backup, or restore operation holds ${operation_lock_dir}" >&2
  return 1
}

release_operation_lock() {
  if [ "${operation_lock_acquired}" = true ]; then
    rmdir "${operation_lock_dir}" >/dev/null 2>&1 || true
    operation_lock_acquired=false
  fi
}

mkdir -p "${state_dir}"

previous_sha=""
previous_release_dir=""
previous_image=""
previous_container_id=""

load_previous_release() {
  if [ -f "${state_file}" ]; then
    # shellcheck disable=SC1090
    source "${state_file}"
    previous_sha="${RELEASE_SHA:-}"
    previous_release_dir="${RELEASE_DIR:-}"
    previous_image="${IMAGE_TAG:-}"
  fi
}

capture_running_release() {
  previous_container_id="$(cd "${compose_dir}" && "${docker_bin}" compose ps -q app 2>/dev/null || true)"
  if [ -n "${previous_container_id}" ]; then
    "${docker_bin}" inspect "${previous_container_id}" > "${state_dir}/previous-container.json" 2>/dev/null || true
    local running_image
    running_image="$("${docker_bin}" inspect --format '{{.Config.Image}}' "${previous_container_id}" 2>/dev/null || true)"
    if [ -n "${running_image}" ]; then
      previous_image="${running_image}"
      if [ -z "${previous_sha}" ]; then
        local running_sha
        running_sha="${running_image##*:}"
        local running_release_dir="${releases_dir}/${running_sha}"
        if [ -f "${running_release_dir}/build/libs/app.jar" ] && [ -f "${running_release_dir}/.env" ]; then
          previous_sha="${running_sha}"
          previous_release_dir="${running_release_dir}"
        fi
      fi
    fi
  fi
}

write_state() {
  local destination="$1"
  local sha="$2"
  local directory="$3"
  local image="$4"
  local temporary
  temporary="$(mktemp "${state_dir}/state.XXXXXX")"
  {
    printf 'RELEASE_SHA=%q\n' "${sha}"
    printf 'RELEASE_DIR=%q\n' "${directory}"
    printf 'IMAGE_TAG=%q\n' "${image}"
  } > "${temporary}"
  mv "${temporary}" "${destination}"
}

wait_for_healthy() {
  local container_id
  container_id="$(cd "${compose_dir}" && "${docker_bin}" compose ps -q app)"
  if [ -z "${container_id}" ]; then
    echo "App container was not created" >&2
    return 1
  fi

  for attempt in $(seq 1 "${health_attempts}"); do
    local health_status
    health_status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${health_status}" = "healthy" ]; then
      return 0
    fi
    if [ "${health_status}" = "exited" ] || [ "${health_status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep "${health_wait_seconds}"
  done

  echo "App container did not become healthy" >&2
  "${docker_bin}" logs "${container_id}" >&2 || true
  return 1
}

wait_for_redis_healthy() {
  local container_id
  container_id="$(cd "${compose_dir}" && "${docker_bin}" compose ps -q redis)"
  if [ -z "${container_id}" ]; then
    echo "Redis container was not created" >&2
    return 1
  fi

  for attempt in $(seq 1 "${redis_health_attempts}"); do
    local health_status
    health_status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${health_status}" = "healthy" ]; then
      return 0
    fi
    if [ "${health_status}" = "exited" ] || [ "${health_status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep "${redis_health_wait_seconds}"
  done

  echo "Redis container did not become healthy" >&2
  "${docker_bin}" logs "${container_id}" >&2 || true
  return 1
}

wait_for_db_healthy() {
  local container_id
  container_id="$(cd "${compose_dir}" && "${docker_bin}" compose ps -q db)"
  if [ -z "${container_id}" ]; then
    echo "DB container was not created" >&2
    return 1
  fi

  for attempt in $(seq 1 "${db_health_attempts}"); do
    local health_status
    health_status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${health_status}" = "healthy" ]; then
      return 0
    fi
    if [ "${health_status}" = "exited" ] || [ "${health_status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep "${db_health_wait_seconds}"
  done

  echo "DB container did not become healthy" >&2
  "${docker_bin}" logs "${container_id}" >&2 || true
  return 1
}

ensure_db_ready() {
  (
    cd "${compose_dir}"
    "${docker_bin}" compose up -d db
  )
  wait_for_db_healthy
}

provision_db_service_accounts() {
  (
    cd "${compose_dir}"
    "${docker_bin}" compose exec -T db /docker-entrypoint-initdb.d/01-provision-service-accounts.sh
  )
}

migrate_release_schema() {
  local directory="$1"
  (
    cd "${compose_dir}"
    APP_RELEASE_DIR="${directory}" "${docker_bin}" compose --profile migration run --rm --no-deps migrate
  )
}

quiesce_runtime_app() {
  local container_id
  container_id="$(cd "${compose_dir}" && "${docker_bin}" compose ps -q app 2>/dev/null || true)"
  if [ -n "${container_id}" ]; then
    (
      cd "${compose_dir}"
      "${docker_bin}" compose stop app
    )
  fi
}

ensure_redis_ready() {
  (
    cd "${compose_dir}"
    "${docker_bin}" compose up -d redis
  )
  wait_for_redis_healthy
}

deploy_release() {
  local sha="$1"
  local directory="$2"
  cd "${compose_dir}"
  APP_RELEASE_DIR="${directory}" \
  APP_IMAGE_NAME="${app_image_name}" \
  APP_IMAGE_TAG="${sha}" \
    "${docker_bin}" compose up -d --no-deps app
}

notify_deployment_failure() {
  local reason="$1"
  local outcome="$2"
  if [ -z "${webhook_url}" ]; then
    echo "DEPLOYMENT_DISCORD_WEBHOOK_URL is not configured; deployment failure notification was not sent" >&2
    return 1
  fi
  "${curl_bin}" --fail --silent --show-error \
    -H 'Content-Type: application/json' \
    --data "{\"content\":\"[JBNU Sugang Helper] deployment ${release_sha} failed (${reason}); ${outcome}.\"}" \
    "${webhook_url}"
}

notify_rollback() {
  local reason="$1"
  notify_deployment_failure "${reason}" "rolled back to ${previous_sha}"
}

notify_redis_preflight_failure() {
  notify_deployment_failure "Redis health check failed before app deployment" "kept ${previous_sha} running"
}

notify_db_preflight_failure() {
  notify_deployment_failure "DB health check failed before app deployment" "kept ${previous_sha} running"
}

notify_db_provisioning_failure() {
  notify_deployment_failure "DB service-account provisioning failed before app deployment" "kept ${previous_sha} running"
}

notify_migration_failure() {
  notify_deployment_failure "schema migration failed after the app was quiesced" "application remains stopped; manual recovery is required"
}

notify_post_migration_failure() {
  local reason="$1"
  notify_deployment_failure "${reason} after schema migration" "application remains stopped; manual recovery is required"
}

rollback() {
  local reason="$1"
  if [ -z "${previous_sha}" ] || [ -z "${previous_release_dir}" ] || [ ! -d "${previous_release_dir}" ]; then
    echo "No recorded healthy release is available for rollback" >&2
    return 1
  fi

  echo "Rolling back failed release ${release_sha} to ${previous_sha}" >&2
  deploy_release "${previous_sha}" "${previous_release_dir}"
  wait_for_healthy
  notify_rollback "${reason}" || true
}

load_previous_release
capture_running_release
if ! acquire_operation_lock; then
  notify_deployment_failure "operation lock is held" "kept ${previous_sha} running" || true
  exit 1
fi
trap release_operation_lock EXIT
write_state "${state_dir}/deployment-start.env" "${previous_sha}" "${previous_release_dir}" "${previous_image}"

if ! (cd "${release_dir}" && "${docker_bin}" build -t "${app_image_name}:${release_sha}" .); then
  rollback "image build failed" || true
  exit 1
fi

if ! ensure_db_ready; then
  notify_db_preflight_failure || true
  exit 1
fi

if ! provision_db_service_accounts; then
  notify_db_provisioning_failure || true
  exit 1
fi

if ! quiesce_runtime_app; then
  notify_deployment_failure "could not quiesce the current app before schema migration" "kept ${previous_sha} running" || true
  exit 1
fi

if ! migrate_release_schema "${release_dir}"; then
  notify_migration_failure || true
  exit 1
fi

if ! ensure_redis_ready; then
  notify_post_migration_failure "Redis health check failed" || true
  exit 1
fi

if ! deploy_release "${release_sha}" "${release_dir}"; then
  notify_post_migration_failure "new app container start failed" || true
  exit 1
fi

if ! wait_for_healthy; then
  notify_post_migration_failure "new app health check failed" || true
  exit 1
fi

write_state "${state_file}" "${release_sha}" "${release_dir}" "${app_image_name}:${release_sha}"
echo "Release ${release_sha} is healthy"
