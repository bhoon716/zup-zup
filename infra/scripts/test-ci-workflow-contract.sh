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

require("name: PR CI", "workflow name must be PR CI")
require("pull_request:", "PR trigger is missing")
if "\n  push:\n" in workflow or "\n  workflow_dispatch:\n" in workflow:
    raise SystemExit("PR CI must only trigger from pull_request")
if "concurrency:" in workflow:
    raise SystemExit("PR CI concurrency must be removed")
require("permissions:\n  contents: read", "workflow must default to contents: read")
require("  verify:", "CI must use one aggregate verification job")
require("    name: CI", "CI aggregate job name is missing")
require("    services:\n      mysql:", "MySQL service container is missing")
require("      redis:\n        image: redis:latest", "Redis service container is missing")
require("Flyway migrate", "Flyway migrate step is missing")
require("Flyway validate", "Flyway validate step is missing")
require("FLYWAY_IMAGE: flyway/flyway@sha256:", "CI Flyway image must be digest-pinned")
require("./gradlew clean check --no-daemon", "backend check is missing")
require("./gradlew migrationTest --no-daemon", "existing-schema migration gate is missing")
require("./gradlew bootJar --no-daemon", "application JAR build is missing")
require("docker/build-push-action@v6", "Buildx push action is missing")
require("platforms: linux/arm64", "ARM64 platform is not pinned")
require("push: false", "PR image build must not push")
require("ghcr.io/", "GHCR image name is missing")
require("github.sha", "commit SHA image tag is missing")
if "packages: write" in workflow or "contents: write" in workflow:
    raise SystemExit("PR CI must not receive write permissions")
if "publish-image" in workflow or "workflow_run" in workflow:
    raise SystemExit("PR CI must not contain a production publish job")
if "e2e" in workflow.lower() or "playwright" in workflow.lower():
    raise SystemExit("E2E must not be a required PR CI gate")

print("PR CI workflow contract passed")
PY
