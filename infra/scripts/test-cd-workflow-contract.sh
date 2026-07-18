#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/cd.yml}"
if [ ! -f "${workflow_file}" ]; then
  echo "CD workflow not found: ${workflow_file}" >&2
  exit 1
fi

python3 - "${workflow_file}" <<'PY'
import sys
from pathlib import Path

workflow_path = Path(sys.argv[1])
workflow = workflow_path.read_text(encoding="utf-8")
repo_root = workflow_path.resolve().parents[2]
deploy_script = (repo_root / "infra/scripts/deploy-release.sh").read_text(encoding="utf-8")
contract = workflow + "\n" + deploy_script

def require(fragment, message):
    if fragment not in contract:
        raise SystemExit(message)

require("name: Production CD", "workflow name must be Production CD")
require("workflow_run:", "completed CI workflow trigger is missing")
require("workflows: [\"PR CI\"]", "CD must consume PR CI completion")
require("types: [completed]", "completed CI workflow trigger is missing")
require("head_branch == 'main'", "CD must deploy only successful main CI runs")
require("workflow_dispatch:", "manual dispatch trigger is missing")
require("concurrency:\n  group: production-deploy\n  cancel-in-progress: false", "production deploys must be serialized without cancellation")
require("environment: production", "production Environment is missing")
require("known_hosts", "pinned known_hosts setup is missing")
require("SSH_PRIVATE_KEY", "production SSH key is missing")
require("scp", "staging SCP transfer is missing")
require("sha256sum", "staging checksum validation is missing")
require("DEPLOY_MANIFEST_PRIVATE_KEY", "signed staging manifest secret is missing")
require("SHA256SUMS.sig", "signed staging manifest is missing")
require("openssl dgst -sha256 -sign", "staging manifest signing is missing")
require("config >/dev/null", "Compose config validation is missing")
require("test-release-layout-contract.sh", "release layout contract is missing")
require("test-ghcr-auth-contract.sh", "GHCR auth contract is missing")
require("test-nginx-bootstrap-contract.sh", "Nginx bootstrap contract is missing")
require("00-sugang-helper-rate-limit.conf", "Nginx rate-limit config staging is missing")
require("jbnu-sugang-helper-proxy.conf", "Nginx proxy snippet staging is missing")
require("jbnu-deploy", "allowlisted deploy wrapper is missing")
require("df -P", "disk preflight is missing")
require("pull db redis", "MySQL/Redis image preflight is missing")
require("pull migrate", "Flyway image preflight is missing")
require("IMAGE_TAG", "IMAGE_TAG contract is missing")
require("validate", "Flyway validate is missing")
require("migrate", "Flyway migrate is missing")
require("127.0.0.1:8081/actuator/health/readiness", "readiness gate is missing")
require("pull app", "application image pull is missing")
require("after ${attempt} attempts", "application image pull retry is missing")
require(".env.release", "release state file is missing")
require("keep recent 3", "recent-three image cleanup contract is missing")

if "cancel-in-progress: true" in workflow:
    raise SystemExit("production deploy must not cancel an in-flight deployment")
if "packages: write" in workflow or "contents: write" in workflow:
    raise SystemExit("CD workflow must not receive package/content write permission")

print("production CD workflow contract passed")
PY
