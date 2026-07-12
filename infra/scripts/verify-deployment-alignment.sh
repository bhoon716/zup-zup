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

if 'infra/scripts/deploy-app.sh' not in workflow:
    raise SystemExit("deploy source does not include the rollback deployment script")

if not re.search(r'cd ~/jbnu-sugang-helper/infra\s*\n\s*RELEASE_SHA=.*?bash scripts/deploy-app\.sh', workflow, re.DOTALL):
    raise SystemExit("deploy does not run the rollback script from the transferred infra directory")

if 'DEPLOYMENT_DISCORD_WEBHOOK_URL' not in workflow:
    raise SystemExit("deploy does not require a rollback notification webhook")

if not re.search(r'GOOGLE_REDIRECT_URI', workflow) or not re.search(
    r'GOOGLE_REDIRECT_URI=https://', workflow,
):
    raise SystemExit("deploy does not require an HTTPS GOOGLE_REDIRECT_URI")

if 'SPRING_PROFILES_ACTIVE must include prod exactly once' not in workflow:
    raise SystemExit("deploy does not require the production profile")

if 'APP_CORS_ALLOWED_ORIGINS must contain only explicit HTTPS origins' not in workflow:
    raise SystemExit("deploy does not require explicit HTTPS CORS origins")

if not re.search(
    r'deploy-server:.*?concurrency:\s*\n\s*group:\s*production-server\s*\n\s*cancel-in-progress:\s*false',
    workflow,
    re.DOTALL,
):
    raise SystemExit("production deploys are not serialized")

if 'github.sha' not in workflow or 'RELEASE_SHA' not in workflow or 'RELEASE_DIR' not in workflow:
    raise SystemExit("deploy does not bind the release directory and image to the commit SHA")

print("deployment compose alignment passed")
PY
