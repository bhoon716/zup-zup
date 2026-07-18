#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/ci.yml}"
if [ ! -f "${workflow_file}" ]; then
  echo "CI workflow not found: ${workflow_file}" >&2
  exit 1
fi

python3 - "${workflow_file}" <<'PY'
import sys
from pathlib import Path

workflow = Path(sys.argv[1]).read_text(encoding="utf-8")

def require(fragment, message):
    if fragment not in workflow:
        raise SystemExit(message)

def job_block(name):
    lines = workflow.splitlines()
    marker = f"  {name}:"
    try:
        start = lines.index(marker)
    except ValueError:
        raise SystemExit(f"job {name} is missing")
    block = []
    for line in lines[start:]:
        if block and line.startswith("  ") and not line.startswith("    "):
            break
        block.append(line)
    return "\n".join(block)

require("name: PR CI", "workflow name must be PR CI")
require("pull_request:", "PR trigger is missing")
require("permissions:\n  contents: read", "workflow must default to contents: read")
require("    services:\n      mysql:", "MySQL service container is missing")
require("      redis:\n        image: redis:latest", "Redis service container is missing")
require("name: Flyway validate", "Flyway validate step is missing")
require("name: Flyway migrate", "Flyway migrate step is missing")
require("FLYWAY_IMAGE: flyway/flyway@sha256:", "CI Flyway image must be digest-pinned")
require("./gradlew clean check --no-daemon", "backend check is missing")
require("./gradlew migrationTest --no-daemon", "existing-schema migration gate is missing")
require("docker/build-push-action@v6", "Buildx push action is missing")
require("platforms: linux/arm64", "ARM64 platform is not pinned")
require("ghcr.io/", "GHCR image name is missing")
require("github.sha", "commit SHA image tag is missing")
require("PREVIOUS_IMAGE_TAG", "previous image smoke input is missing")
require('docker pull --platform linux/arm64 "${IMAGE_NAME}:${PREVIOUS_IMAGE_TAG}"', "previous image pull is missing")
require('"${IMAGE_NAME}:${PREVIOUS_IMAGE_TAG}" -version', "previous image smoke test is missing")

if "packages: write-all" in workflow or "contents: write" in workflow:
    raise SystemExit("workflow grants broader write permissions than required")

publish = job_block("publish-image")
if "packages: write" not in publish:
    raise SystemExit("only publish-image should receive packages: write")

for job_name in ("server-integration", "build-image"):
    if "packages: write" in job_block(job_name):
        raise SystemExit(f"job {job_name} must not receive packages: write")

if "e2e" in workflow.lower() or "playwright" in workflow.lower():
    raise SystemExit("E2E must not be a required PR CI gate")

print("PR CI workflow contract passed")
PY
