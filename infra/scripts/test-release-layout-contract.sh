#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_file="${repo_root}/infra/docker-compose.yml"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"
rollback_script="${repo_root}/infra/scripts/rollback-release.sh"

python3 - "${compose_file}" <<'PY'
import sys
from pathlib import Path

compose = Path(sys.argv[1]).read_text(encoding="utf-8")
if not compose.startswith("name: sugang-helper\n"):
    raise SystemExit("production Compose must declare the stable sugang-helper project name")
PY

if ! grep -F ' -f "${RELEASE_ROOT}/docker-compose.yml"' "${rollback_script}" >/dev/null; then
  echo "rollback must use the fixed RELEASE_ROOT Compose file" >&2
  exit 1
fi
if ! grep -F -- '--project-name sugang-helper' "${deploy_script}" >/dev/null \
  || ! grep -F -- '--project-name sugang-helper' "${rollback_script}" >/dev/null; then
  echo "deploy and rollback must use the stable sugang-helper Compose project" >&2
  exit 1
fi
if ! grep -F 'mysql/init/01-provision-service-accounts.sh' "${deploy_script}" >/dev/null; then
  echo "deploy must promote the DB init support file" >&2
  exit 1
fi
for required_path in 'loki/loki-config.yaml' 'alloy/config.alloy' 'grafana/provisioning/datasources/datasource.yml'; do
  if ! grep -F "${required_path}" "${deploy_script}" >/dev/null; then
    echo "deploy must promote observability file: ${required_path}" >&2
    exit 1
  fi
done
if ! grep -F 'find "${release_tmp}/loki"' "${deploy_script}" >/dev/null \
  || ! grep -F 'cp -a "${release_dir}/grafana/.' "${deploy_script}" >/dev/null; then
  echo "observability release files must be readable and promoted from the retained release" >&2
  exit 1
fi
if ! grep -F 'compose[@]}" exec -T db bash /docker-entrypoint-initdb.d/01-provision-service-accounts.sh' "${deploy_script}" >/dev/null; then
  echo "deploy must provision DB service accounts on existing volumes" >&2
  exit 1
fi
if ! grep -F 'chown -R root:root' "${deploy_script}" >/dev/null; then
  echo "deploy must lock the staging tree before root consumes it" >&2
  exit 1
fi
if ! grep -F 'staging_dir}/apps/server/.env' "${deploy_script}" >/dev/null; then
  echo "deploy must promote apps/server/.env" >&2
  exit 1
fi
if ! grep -F 'APP_ENV_FILE=${RELEASE_ROOT}/apps/server/.env' "${deploy_script}" >/dev/null \
  || ! grep -F 'APP_ENV_FILE=${RELEASE_ROOT}/apps/server/.env' "${rollback_script}" >/dev/null; then
  echo "Compose must read the deployed apps/server/.env" >&2
  exit 1
fi
for forbidden in 'deploy-manifest-public.pem' 'SHA256SUMS.sig' 'openssl dgst -sha256 -verify' '.env.app'; do
  if grep -F -- "${forbidden}" "${deploy_script}" >/dev/null; then
    echo "SSH-only deploy must not use ${forbidden}" >&2
    exit 1
  fi
done
if ! grep -F 'https://${api_host}/health/ready' "${deploy_script}" >/dev/null; then
  echo "deploy must verify public HTTPS readiness" >&2
  exit 1
fi
if ! grep -F 'https://${api_host}/health/ready' "${rollback_script}" >/dev/null; then
  echo "rollback must verify public HTTPS readiness" >&2
  exit 1
fi
if ! grep -F 'RELEASE_HISTORY' "${deploy_script}" >/dev/null \
  || ! grep -F 'head -n 3 "${RELEASE_HISTORY}"' "${deploy_script}" >/dev/null; then
  echo "deploy retention must follow deployment order from release history" >&2
  exit 1
fi
if grep -F 'sort -r' "${deploy_script}" >/dev/null || grep -F 'sort -r' "${rollback_script}" >/dev/null; then
  echo "release retention must not use lexical SHA ordering" >&2
  exit 1
fi
if ! grep -F 'rm -rf -- "${staging_dir}"' "${deploy_script}" >/dev/null; then
  echo "deploy must clean staging after a successful or failed handoff" >&2
  exit 1
fi
if ! grep -F 'mountpoint -q "${db_data_dir}"' "${deploy_script}" >/dev/null \
  || ! grep -F 'findmnt -n -T "${db_data_dir}"' "${deploy_script}" >/dev/null; then
  echo "deploy must fail closed when the OCI DB volume is not mounted" >&2
  exit 1
fi
if ! grep -F 'FLYWAY_IMAGE' "${deploy_script}" >/dev/null \
  || ! grep -F '@sha256:' "${deploy_script}" >/dev/null; then
  echo "deploy must require a digest-pinned Flyway image" >&2
  exit 1
fi

echo "release layout contract passed"
