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
  'readonly STAGING_ROOT="/home/ubuntu/jbnu-sugang-helper-staging"' \
  'APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"' \
  'docker compose --project-name sugang-helper' \
  'flock' \
  'pull app' \
  '--profile migration run --rm --no-deps migrate migrate' \
  '--no-deps --wait' \
  '127.0.0.1:8081/actuator/health/readiness' \
  '.env.release' \
  'rm -rf -- "${staging_dir}"'; do
  if ! grep -F -- "${required}" "${deploy_script}" >/dev/null; then
    echo "deploy contract is missing: ${required}" >&2
    exit 1
  fi
done

for forbidden in \
  'readonly RELEASE_ROOT="/opt/jbnu-sugang-helper"' \
  'readonly STAGING_ROOT="/opt/jbnu-sugang-helper-staging"'; do
  if grep -F -- "${forbidden}" "${deploy_script}" >/dev/null; then
    echo "legacy deploy path must not remain: ${forbidden}" >&2
    exit 1
  fi
done

for required_path in \
  'loki/loki-config.yaml' \
  'alloy/config.alloy' \
  'grafana/provisioning/datasources/datasource.yml' \
  'mysql/init/01-provision-service-accounts.sh' \
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
  'chown -R root:root' \
  'https://${api_host}/health/ready'; do
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
