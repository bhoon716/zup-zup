#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_file="${repo_root}/infra/docker-compose.yml"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"

python3 - "${compose_file}" <<'PY'
import sys
from pathlib import Path

compose = Path(sys.argv[1]).read_text(encoding="utf-8")
if not compose.startswith("name: sugang-helper\n"):
    raise SystemExit("production Compose must declare the stable sugang-helper project name")
PY

for required in \
  'readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"' \
  'readonly STAGING_ROOT="${RELEASE_ROOT}/.staging"' \
  'APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"' \
  'RUNTIME_ENV="${RELEASE_ROOT}/.env"' \
  'docker compose --project-name sugang-helper' \
  'pull app' \
  'docker network connect sugang-helper-runtime sugang-helper-npm' \
  '--profile migration run --rm --no-deps migrate migrate' \
  '--no-deps --wait' \
  '--profile observability run --rm --no-deps observability-probe-tools' \
  '127.0.0.1:8081/actuator/health/readiness' \
  'rm -rf -- "${staging_dir}"'; do
  if ! grep -F -- "${required}" "${deploy_script}" >/dev/null; then
    echo "deploy contract is missing: ${required}" >&2
    exit 1
  fi
done

if ! grep -F 'sync_directory()' "${deploy_script}" >/dev/null; then
  echo "deploy must define exact directory synchronization" >&2
  exit 1
fi
for required_sync in \
  'sync_directory "${staging_dir}/src/main/resources/db" "${RELEASE_ROOT}/src/main/resources/db"' \
  'sync_directory "${staging_dir}/loki" "${RELEASE_ROOT}/loki"' \
  'sync_directory "${staging_dir}/alloy" "${RELEASE_ROOT}/alloy"' \
  'sync_directory "${staging_dir}/prometheus" "${RELEASE_ROOT}/prometheus"' \
  'sync_directory "${staging_dir}/grafana" "${RELEASE_ROOT}/grafana"'; do
  if ! grep -F -- "${required_sync}" "${deploy_script}" >/dev/null; then
    echo "deploy must synchronize without stale files: ${required_sync}" >&2
    exit 1
  fi
done
for forbidden_overlay in \
  'cp -a "${staging_dir}/src/main/resources/db/."' \
  'cp -a "${staging_dir}/loki/."' \
  'cp -a "${staging_dir}/alloy/."' \
  'cp -a "${staging_dir}/prometheus/."' \
  'cp -a "${staging_dir}/grafana/."'; do
  if grep -F -- "${forbidden_overlay}" "${deploy_script}" >/dev/null; then
    echo "deploy must not overlay stale-prone directory: ${forbidden_overlay}" >&2
    exit 1
  fi
done

fixture_root="$(mktemp -d)"
trap 'rm -rf -- "${fixture_root}"' EXIT
mkdir -p "${fixture_root}/source" "${fixture_root}/destination"
printf 'current\n' >"${fixture_root}/source/current.conf"
printf 'stale\n' >"${fixture_root}/destination/removed.conf"
sync_function="$(sed -n '/^sync_directory() {/,/^}/p' "${deploy_script}")"
eval "${sync_function}"
sync_directory "${fixture_root}/source" "${fixture_root}/destination"
if [ -e "${fixture_root}/destination/removed.conf" ] \
  || [ "$(cat "${fixture_root}/destination/current.conf")" != "current" ]; then
  echo "directory synchronization fixture retained stale files" >&2
  exit 1
fi
printf 'keep-on-failure\n' >"${fixture_root}/destination/keep.conf"
if sync_directory "${fixture_root}/missing" "${fixture_root}/destination" 2>/dev/null; then
  echo "directory synchronization must fail for a missing source" >&2
  exit 1
fi
if [ "$(cat "${fixture_root}/destination/keep.conf")" != "keep-on-failure" ]; then
  echo "directory synchronization removed the existing tree before a failed copy" >&2
  exit 1
fi

for forbidden in \
  'readonly RELEASE_ROOT="/opt/jbnu-sugang-helper"' \
  'readonly STAGING_ROOT="/opt/jbnu-sugang-helper-staging"' \
  'readonly STAGING_ROOT="/home/ubuntu/jbnu-sugang-helper-staging"' \
  '.env.runtime' \
  '.env.release' \
  'flock'; do
  if grep -F -- "${forbidden}" "${deploy_script}" >/dev/null; then
    echo "legacy deploy path must not remain: ${forbidden}" >&2
    exit 1
  fi
done

for required_path in \
  'loki/loki-config.yaml' \
  'alloy/config.alloy' \
  'prometheus/prometheus.yml' \
  'grafana/provisioning/datasources/datasource.yml' \
  'scripts/test-observability-smoke.sh' \
  'scripts/compose-observability-diagnostics.sh' \
  'src/main/resources/db/migration'; do
  if ! grep -F -- "${required_path}" "${deploy_script}" >/dev/null; then
    echo "deploy must promote: ${required_path}" >&2
    exit 1
  fi
done

for forbidden in \
  'SHA256SUMS' \
  'sha256sum' \
  'RELEASE_HISTORY' \
  'releases/' \
  '/usr/local/sbin' \
  '/usr/local/libexec' \
  'GHCR_USERNAME_FILE' \
  'GHCR_TOKEN_FILE' \
  'sudo' \
  'chown -R root:root'; do
  if grep -F -- "${forbidden}" "${deploy_script}" >/dev/null; then
    echo "lightweight deploy must not use: ${forbidden}" >&2
    exit 1
  fi
done

if ! grep -F 'app remains stopped' "${deploy_script}" >/dev/null; then
  echo "migration failure must leave the app stopped" >&2
  exit 1
fi
if ! grep -F 'FLYWAY_IMAGE' "${deploy_script}" >/dev/null \
  || ! grep -F '@sha256:' "${deploy_script}" >/dev/null; then
  echo "deploy must require a digest-pinned Flyway image" >&2
  exit 1
fi

echo "lightweight deploy contract passed"
