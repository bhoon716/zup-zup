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
if ! grep -F 'compose[@]}" exec -T db bash /docker-entrypoint-initdb.d/01-provision-service-accounts.sh' "${deploy_script}" >/dev/null; then
  echo "deploy must provision DB service accounts on existing volumes" >&2
  exit 1
fi
if ! grep -F 'chown -R root:root' "${deploy_script}" >/dev/null; then
  echo "deploy must lock the staging tree before root consumes it" >&2
  exit 1
fi
if ! grep -F 'RELEASE_ROOT}/secrets/deploy-manifest-public.pem' "${deploy_script}" >/dev/null; then
  echo "deploy must verify the root-owned manifest public key" >&2
  exit 1
fi
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
