#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/cd.yml}"

if [[ ! -f "${workflow_file}" ]]; then
  echo "workflow file not found: ${workflow_file}" >&2
  exit 1
fi

python3 - "${workflow_file}" <<'PY'
import re
import sys
from pathlib import Path

workflow = Path(sys.argv[1]).read_text()

if not re.search(r'source:\s+"infra/docker-compose\.yml,', workflow):
    raise SystemExit("deploy source does not include infra/docker-compose.yml")

if not re.search(
    r'working-directory:\s+infra\s*\n\s*run:\s+bash scripts/verify-compose-policy\.sh docker-compose\.yml',
    workflow,
):
    raise SystemExit("verify-infra does not validate infra/docker-compose.yml")

for script_name in (
    'infra/scripts/deploy-app.sh',
    'infra/scripts/prepare-app-host-directories.sh',
    'infra/scripts/backup-redis-state.sh',
    'infra/scripts/restore-redis-state.sh',
    'infra/scripts/redis-restart-smoke.sh',
    'infra/scripts/backup-dr-state.sh',
    'infra/scripts/restore-dr-state.sh',
    'infra/mysql/**',
):
    if script_name not in workflow:
        raise SystemExit(f"deploy source does not include {script_name}")

if not re.search(r'cd ~/jbnu-sugang-helper/infra\s*\n\s*RELEASE_SHA=.*?bash scripts/deploy-app\.sh', workflow, re.DOTALL):
    raise SystemExit("deploy does not run the rollback script from the transferred infra directory")

if 'DEPLOYMENT_DISCORD_WEBHOOK_URL' not in workflow:
    raise SystemExit("deploy does not require a rollback notification webhook")

if 'forbidden_runtime_db_prefixes' not in workflow or 'SPRING_FLYWAY_' not in workflow:
    raise SystemExit("deploy does not reject every runtime datasource and Flyway prefix")

if 'test-db-service-accounts.sh' not in workflow or 'test-dr-state-recovery.sh' not in workflow:
    raise SystemExit("verify-infra does not exercise DB least privilege and DR recovery")

if not re.search(r'GOOGLE_REDIRECT_URI', workflow) or not re.search(
    r'GOOGLE_REDIRECT_URI=https://', workflow,
):
    raise SystemExit("deploy does not require an HTTPS GOOGLE_REDIRECT_URI")

if 'prod_profile_count' not in workflow or 'SPRING_PROFILES_ACTIVE must include prod exactly once' not in workflow:
    raise SystemExit("deploy does not require the production profile")

if 'urlsplit' not in workflow or 'APP_CORS_ALLOWED_ORIGINS must contain only explicit HTTPS origins' not in workflow:
    raise SystemExit("deploy does not require explicit HTTPS CORS origins")

if not re.search(
    r'deploy-server:.*?concurrency:\s*\n\s*group:\s*production-server\s*\n\s*cancel-in-progress:\s*false',
    workflow,
    re.DOTALL,
):
    raise SystemExit("production deploys are not serialized")

if 'github.sha' not in workflow or 'RELEASE_SHA' not in workflow or 'RELEASE_DIR' not in workflow:
    raise SystemExit("deploy does not bind the release directory and image to the commit SHA")

if 'apps/server/src/main/resources/db/migration/**' not in workflow:
    raise SystemExit("release transfer does not include the migration SQL mounted by the migrator")

print("deployment compose alignment passed")
PY
