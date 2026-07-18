#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/rollback.yml}"
if [ ! -f "${workflow_file}" ]; then
  echo "rollback workflow not found: ${workflow_file}" >&2
  exit 1
fi

python3 - "${workflow_file}" <<'PY'
import sys
from pathlib import Path

workflow_path = Path(sys.argv[1])
workflow = workflow_path.read_text(encoding="utf-8")
repo_root = workflow_path.resolve().parents[2]
rollback_script = (repo_root / "infra/scripts/rollback-release.sh").read_text(encoding="utf-8") if (repo_root / "infra/scripts/rollback-release.sh").exists() else ""
contract = workflow + "\n" + rollback_script

def require(fragment, message):
    if fragment not in contract:
        raise SystemExit(message)

require("name: Application rollback", "rollback workflow name is missing")
require("workflow_dispatch:", "rollback must be manually dispatched")
require("image_tag:", "rollback SHA input is missing")
require("environment: production", "rollback must use production Environment")
require("concurrency:\n  group: production-deploy\n  cancel-in-progress: false", "rollback must share serialized production concurrency")
require("^", "strict rollback SHA validation marker is missing")
require("[0-9a-f]{40}", "strict rollback SHA validation is missing")
require("jbnu-rollback", "allowlisted rollback wrapper is missing")
require("IMAGE_TAG", "rollback must update IMAGE_TAG only after readiness")
require("actuator/health/readiness", "rollback readiness gate is missing")
require("validate", "rollback compatibility validation is missing")
require("automatic DB rollback", "rollback policy must explicitly reject DB rollback")
require("recent 3", "rollback image retention policy is missing")

if "docker compose down -v" in contract or "flyway clean" in contract:
    raise SystemExit("rollback must not delete volumes or run Flyway clean")

print("rollback workflow contract passed")
PY
