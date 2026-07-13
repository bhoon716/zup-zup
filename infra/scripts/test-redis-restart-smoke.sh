#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
smoke_script="${repo_root}/infra/scripts/redis-restart-smoke.sh"
workdir="$(mktemp -d)"

cleanup() {
  rm -rf "${workdir}"
}
trap cleanup EXIT

mkdir -p "${workdir}/bin" "${workdir}/compose"
touch "${workdir}/compose/docker-compose.yml"
printf 'running' > "${workdir}/redis-state"

cat > "${workdir}/bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

state_file="${REDIS_SMOKE_TEST_STATE_FILE}"

if [ "$1" = "inspect" ]; then
  printf 'healthy'
  exit 0
fi

if [ "$1" = "logs" ]; then
  exit 0
fi

if [ "$1" != "compose" ]; then
  exit 1
fi

action="$2"
service=""
for argument in "$@"; do
  case "${argument}" in
    redis|app)
      service="${argument}"
      ;;
  esac
done

case "${action}" in
  ps)
    printf '%s-id' "${service}"
    ;;
  stop)
    if [ "${service}" = "redis" ]; then
      printf 'stopped' > "${state_file}"
    fi
    ;;
  start)
    if [ "${service}" = "redis" ]; then
      printf 'started' > "${state_file}"
    fi
    ;;
  exec)
    if [ "${service}" = "app" ]; then
      exit 0
    fi
    printf 'OK'
    ;;
esac
EOF
chmod 755 "${workdir}/bin/docker"

set +e
DOCKER_BIN="${workdir}/bin/docker" \
COMPOSE_DIR="${workdir}/compose" \
REDIS_SMOKE_TEST_STATE_FILE="${workdir}/redis-state" \
REDIS_SMOKE_HEALTH_MAX_ATTEMPTS=1 \
REDIS_SMOKE_HEALTH_WAIT_SECONDS=0 \
REDIS_SMOKE_READINESS_TIMEOUT_SECONDS=1 \
bash "${smoke_script}"
status=$?
set -e

if [ "${status}" -eq 0 ]; then
  echo "restart smoke unexpectedly succeeded while app readiness stayed UP" >&2
  exit 1
fi

if [ "$(cat "${workdir}/redis-state")" != "started" ]; then
  echo "failed restart smoke left Redis stopped" >&2
  exit 1
fi

echo "redis restart smoke cleanup verification passed"
