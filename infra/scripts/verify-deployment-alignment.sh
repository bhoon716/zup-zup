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
    r'cd ~/jbnu-sugang-helper/infra\s*\n\s*docker compose up -d --no-deps app',
    workflow,
):
    raise SystemExit("deploy does not run Compose from the transferred infra directory")

print("deployment compose alignment passed")
PY
