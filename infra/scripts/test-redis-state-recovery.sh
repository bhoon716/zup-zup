#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
backup_script="${repo_root}/infra/scripts/backup-redis-state.sh"
restore_script="${repo_root}/infra/scripts/restore-redis-state.sh"
docker_bin="${DOCKER_BIN:-docker}"
workdir="$(mktemp -d)"
compose_dir="${workdir}/compose"
compose_file="${compose_dir}/docker-compose.yml"
state_root="${workdir}/state"
redis_state_dir="${state_root}/redis-data"
backup_root="${workdir}/backups"
project_name="issue098-redis-recovery-${RANDOM}-$$"
redis_password="redis-recovery-smoke-password"
key="issue098:recovery:sentinel"
value="preserved-after-aof-restore"

export COMPOSE_PROJECT_NAME="${project_name}"

compose() {
  "${docker_bin}" compose -p "${project_name}" -f "${compose_file}" "$@"
}

cleanup() {
  status=$?
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  rm -rf "${workdir}"
  exit "${status}"
}

wait_for_redis_health() {
  local container_id
  container_id="$(compose ps -q redis)"
  if [ -z "${container_id}" ]; then
    echo "Redis container was not created" >&2
    return 1
  fi

  for _ in $(seq 1 20); do
    status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${status}" = "healthy" ]; then
      return 0
    fi
    sleep 1
  done

  echo "Redis did not become healthy" >&2
  return 1
}

redis_cli() {
  compose exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --raw "$@"' sh "$@"
}

trap cleanup EXIT

mkdir -p "${compose_dir}" "${redis_state_dir}"
chmod 0777 "${redis_state_dir}"
cat > "${compose_file}" <<EOF
services:
  redis:
    image: redis:6-alpine
    environment:
      REDIS_PASSWORD: ${redis_password}
    command: redis-server --requirepass ${redis_password} --appendonly yes --appendfsync everysec
    volumes:
      - ${redis_state_dir}:/data
    healthcheck:
      test: ["CMD-SHELL", "REDISCLI_AUTH=\"\$\$REDIS_PASSWORD\" redis-cli ping | grep -qx PONG"]
      interval: 1s
      timeout: 1s
      retries: 20
      start_period: 1s
EOF

compose up -d redis
wait_for_redis_health
redis_cli SET "${key}" "${value}" EX 120 >/dev/null

ALLOW_NON_ROOT=1 \
COMPOSE_DIR="${compose_dir}" \
REDIS_STATE_DIR="${redis_state_dir}" \
BACKUP_ROOT="${backup_root}" \
REDIS_HEALTH_MAX_ATTEMPTS=20 \
REDIS_HEALTH_WAIT_SECONDS=1 \
"${backup_script}"

archive="$(find "${backup_root}" -maxdepth 1 -name 'redis-state-*.tar.gz' -type f | head -1)"
if [ -z "${archive}" ] || [ ! -f "${archive}.sha256" ]; then
  echo "Redis backup archive or checksum was not created" >&2
  exit 1
fi

compose stop redis >/dev/null
mv "${redis_state_dir}" "${state_root}/redis-data-lost"
mkdir "${redis_state_dir}"
chmod 0777 "${redis_state_dir}"
compose up -d redis >/dev/null
wait_for_redis_health

if [ -n "$(redis_cli GET "${key}")" ]; then
  echo "Redis state loss simulation did not remove the sentinel" >&2
  exit 1
fi

ALLOW_NON_ROOT=1 \
COMPOSE_DIR="${compose_dir}" \
REDIS_STATE_DIR="${redis_state_dir}" \
REDIS_HEALTH_MAX_ATTEMPTS=20 \
REDIS_HEALTH_WAIT_SECONDS=1 \
CONFIRM_REDIS_RESTORE=RESTORE \
"${restore_script}" "${archive}"

if [ "$(redis_cli GET "${key}")" != "${value}" ]; then
  echo "Redis recovery did not restore the sentinel" >&2
  exit 1
fi

if [ "$(stat -f '%Lp' "${redis_state_dir}")" != "700" ]; then
  echo "Redis recovery did not restore owner-only state permissions" >&2
  exit 1
fi

echo "Redis state recovery verification passed"
