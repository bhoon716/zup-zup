#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/ci.yml}"
if [ ! -f "${workflow_file}" ]; then
  echo "PR CI workflow not found: ${workflow_file}" >&2
  exit 1
fi

python3 - "${workflow_file}" <<'PY'
import sys
from pathlib import Path

workflow = Path(sys.argv[1]).read_text(encoding="utf-8")

def require(fragment, message):
    if fragment not in workflow:
        raise SystemExit(message)

require("name: PR CI", "PR CI workflow name is missing")
require("pull_request:", "PR CI must run for pull requests")
require("name: CI", "required CI job name must remain CI")
require("actions/setup-node@v4", "PR CI must configure Node.js")
require('node-version: "22"', "PR CI must use Node.js 22")
require("cache-dependency-path: apps/web/package-lock.json", "npm cache must use the Web lockfile")
require("working-directory: apps/web", "Web commands must run from apps/web")
require("run: npm ci --legacy-peer-deps", "Web dependencies must use the lockfile-compatible install command")
require("run: npm run lint", "Web lint must be a required CI step")
require("run: npm run test -- --run", "Web unit tests must be a required CI step")
require("run: npm run build", "Web production build must be a required CI step")

for forbidden in ("continue-on-error:", "npm ci\n", "npx vitest run"):
    if forbidden in workflow:
        raise SystemExit(f"PR Web validation must not use: {forbidden}")

for preserved in (
    "bash infra/scripts/test-runtime-contract.sh",
    "./gradlew clean check --no-daemon",
    "./gradlew migrationTest --no-daemon",
    "docker/build-push-action@v6",
):
    require(preserved, f"existing server/infra CI gate is missing: {preserved}")

print("PR CI Web validation contract passed")
PY
