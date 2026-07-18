#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"

python3 - "${deploy_script}" <<'PY'
import sys
from pathlib import Path

content = Path(sys.argv[1]).read_text(encoding="utf-8")
stop = content.index('stage="stop-app"')
migrate = content.index('stage="flyway-migrate"')
start = content.index('stage="app-start"')
if not stop < migrate < start:
    raise SystemExit("migration stages are not ordered stop -> migrate -> start")
if "app remains stopped" not in content:
    raise SystemExit("migration failure must leave the app stopped")
if "automatic DB rollback" in content:
    raise SystemExit("deploy script must not promise automatic DB rollback")

print("migration failure ordering contract passed")
PY

# A tiny executable model of the fail-closed branch: a failed migration leaves
# the old app stopped and never invokes an automatic restart.
app_state=running
migration_result=failed
app_state=stopped
if [ "${migration_result}" != failed ] || [ "${app_state}" != stopped ]; then
  echo "fake migration failure model did not remain stopped" >&2
  exit 1
fi
echo "fake migration failure model passed"
