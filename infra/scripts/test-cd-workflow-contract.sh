#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/cd.yml}"
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"

python3 - "${workflow_file}" "${deploy_script}" <<'PY'
import sys
from pathlib import Path

workflow = Path(sys.argv[1]).read_text(encoding="utf-8")
deploy = Path(sys.argv[2]).read_text(encoding="utf-8")

def require(text, fragment, message):
    if fragment not in text:
        raise SystemExit(message)

for fragment in (
    "name: Production CD",
    "push:\n    branches: [main]",
    'paths:\n      - "apps/server/**"\n      - "infra/**"',
    "workflow_dispatch:",
    "group: production",
    "cancel-in-progress: false",
    "timeout-minutes:",
    "packages: write",
    "SSH_HOST_PUBLIC_KEY: ${{ vars.SSH_HOST_PUBLIC_KEY }}",
    "StrictHostKeyChecking=yes",
    "UserKnownHostsFile=",
    "ssh-keygen -l -E sha256",
    "docker login ghcr.io",
    "--password-stdin",
    "docker logout ghcr.io",
):
    require(workflow, fragment, f"CD workflow is missing: {fragment}")

for secret in ("SERVER_HOST", "SERVER_USER", "SSH_PRIVATE_KEY", "SERVER_DOTENV"):
    require(workflow, f"${{{{ secrets.{secret} }}}}", f"CD secret is missing: {secret}")

for forbidden in ("ssh-keyscan", "sudo", "GHCR_READ_TOKEN"):
    if forbidden in workflow or forbidden in deploy:
        raise SystemExit(f"CD must not use: {forbidden}")

for fragment in (
    'readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"',
    '--profile migration run --rm --no-deps migrate migrate',
    '127.0.0.1:8081/actuator/health/readiness',
    'scripts/test-observability-smoke.sh',
    'observability smoke warning; deployment remains active',
):
    require(deploy, fragment, f"deploy script is missing: {fragment}")

if '|| fail "observability data-plane smoke failed"' in deploy:
    raise SystemExit("observability smoke must not stop a ready application")

print("minimal production CD contract passed")
PY
