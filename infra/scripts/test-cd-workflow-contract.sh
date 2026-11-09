#!/usr/bin/env bash
set -euo pipefail

workflow_file="${1:-.github/workflows/cd.yml}"
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"

python3 - "${workflow_file}" "${deploy_script}" "${repo_root}" <<'PY'
import sys
from pathlib import Path

workflow = Path(sys.argv[1]).read_text(encoding="utf-8")
deploy = Path(sys.argv[2]).read_text(encoding="utf-8")
repo_root = Path(sys.argv[3])

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

for script in ("backup-server-export.sh", "backup-admin-audit.sh"):
    require(workflow, f'cp infra/scripts/{script} "${{stage_dir}}/scripts/{script}"',
            f"CD must package both archive scripts: {script}")
    require(deploy, script, f"deploy must preserve backup script pair: {script}")

for script in ("run-host-job.py", "check-storage-health.py", "backup-important-events.py"):
    require(workflow, script, f"CD must package host job dependency: {script}")
    require(deploy, script, f"deploy must prepare host job dependency: {script}")
require(workflow, 'cp -a infra/systemd "${stage_dir}/systemd"',
        "CD must package host job templates")
require(deploy, 'sync_directory "${staging_dir}/systemd" "${RELEASE_ROOT}/systemd"',
        "deploy must prepare host job templates without activating them")
for fragment in ('docker-compose.alerting.yml', 'ALERTING_ENABLED', 'alertmanager startup failed'):
    require(deploy.lower(), fragment.lower(), f"deploy must retain opt-in receiver wiring: {fragment}")
require(workflow, 'cp -a infra/alertmanager "${stage_dir}/alertmanager"',
        "CD must package receiver configuration without its credential")
for forbidden in ("systemctl enable", "systemctl start", "daemon-reload"):
    if forbidden in deploy:
        raise SystemExit(f"deployment must not activate new host jobs: {forbidden}")

if '|| fail "observability data-plane smoke failed"' in deploy:
    raise SystemExit("observability smoke must not stop a ready application")

print("minimal production CD contract passed")
PY
