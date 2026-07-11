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

if not re.search(
    r'cd ~/jbnu-sugang-helper/infra\s*\n\s*(?:APP_RELEASE_DIR=.*? )?'
    r'(?:APP_IMAGE_TAG=.*? )?docker compose up -d --no-deps app',
    workflow,
):
    raise SystemExit("deploy does not run Compose from the transferred infra directory")

if not re.search(
    r'docker compose up -d --no-deps app\s*\n\s*container_id=\$\(docker compose ps -q app\).*?'
    r'docker inspect.*State\.Health.*Status.*\$\{container_id\}',
    workflow,
    re.DOTALL,
):
    raise SystemExit("deploy does not wait for the app container health status")

if not re.search(r'GOOGLE_REDIRECT_URI', workflow) or not re.search(
    r'GOOGLE_REDIRECT_URI=https://', workflow,
):
    raise SystemExit("deploy does not require an HTTPS GOOGLE_REDIRECT_URI")

if not re.search(
    r'deploy-server:.*?concurrency:\s*\n\s*group:\s*production-server\s*\n\s*cancel-in-progress:\s*false',
    workflow,
    re.DOTALL,
):
    raise SystemExit("production deploys are not serialized")

if 'github.sha' not in workflow or 'APP_IMAGE_TAG' not in workflow or 'APP_RELEASE_DIR' not in workflow:
    raise SystemExit("deploy does not bind the release directory and image to the commit SHA")

print("deployment compose alignment passed")
PY
