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

def require(fragment, message):
    if fragment not in workflow:
        raise SystemExit(message)

def require_deploy(fragment, message):
    if fragment not in deploy_script:
        raise SystemExit(message)

require("name: Production CD", "workflow name must be Production CD")
require("push:\n    branches: [main]", "CD must trigger from main push")
require("workflow_dispatch:", "manual dispatch trigger is missing")
require("image_tag:", "manual SHA input is missing")
for forbidden in ("pull_request:", "workflow_run:", "concurrency:", "publish-image:", "needs: publish-image", "always()"):
    if forbidden in workflow:
        raise SystemExit(f"CD must not use {forbidden}")
require("if: github.event_name == 'push'", "image publish steps must be push-only")
require("docker/build-push-action@v6", "CD must build the production image")
require("push: true", "CD must push the production image")
require("packages: write", "single CD job must publish to GHCR")
require("actions/checkout@v5", "CD checkout is missing")
for secret in ("SERVER_HOST", "SERVER_USER", "SSH_PRIVATE_KEY", "SERVER_DOTENV"):
    require(f"${{{{ secrets.{secret} }}}}", f"repository Actions secret is missing: {secret}")
for forbidden in (
    "OCI_HOST", "OCI_KNOWN_HOSTS", "OCI_DEPLOY_USER", "DEPLOY_MANIFEST_PRIVATE_KEY",
    "SHA256SUMS", "SHA256SUMS.sig", "GHCR_READ_TOKEN", "GHCR_READ_USERNAME",
    "jbnu-deploy", "jbnu-rollback", "rollback-release", "sudo", "environment: production",
):
    if forbidden in workflow or forbidden in deploy_script:
        raise SystemExit(f"SSH-only CD must not use {forbidden}")
require('target="${SERVER_USER}@${SERVER_HOST}"', "CD must use the configured server account")
require("ssh-keyscan", "runtime SSH host key setup is missing")
require("SSH_PRIVATE_KEY", "SSH private key is missing")
require("SERVER_DOTENV", "server dotenv secret is missing")
require('stage_dir}/apps/server/.env', "SERVER_DOTENV must be staged as apps/server/.env")
require("scp", "staging SCP transfer is missing")
require("deploy-release.sh", "deploy script must be transferred")
require("deploy.sh", "remote deploy entrypoint is missing")
require("GITHUB_TOKEN", "the short-lived Actions token must authenticate the remote pull")
require("docker login ghcr.io", "remote GHCR login is missing")
require("--password-stdin", "remote GHCR login must not expose the token in arguments")
require("docker logout ghcr.io", "remote GHCR credentials must be removed after deploy")
require('remote_root="/home/ubuntu/jbnu-sugang-helper"', "CD must use the ubuntu home release root")
require('remote_stage="${remote_root}/.staging/${IMAGE_TAG}"', "CD must use an ephemeral staging root under the release root")
require_deploy('readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"', "fixed Ubuntu release root is missing")
require_deploy('readonly STAGING_ROOT="${RELEASE_ROOT}/.staging"', "ephemeral staging root is missing")
require_deploy('APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"', "deploy must use apps/server/.env")
require_deploy('RUNTIME_ENV="${RELEASE_ROOT}/.env"', "deploy must use the root .env runtime contract")
require_deploy('rm -f -- "${runtime_env}"', "temporary Compose environment must be removed after deploy")
require_deploy("pull app", "deploy must pull the selected app image")
require_deploy("--profile migration run --rm --no-deps migrate migrate", "deploy must run one-shot Flyway migration")
require_deploy("--no-deps --wait", "deploy must wait for app health")
require_deploy("127.0.0.1:8081/actuator/health/readiness", "internal readiness gate is missing")
require_deploy("loki/loki-config.yaml", "Loki configuration must be deployed")
require_deploy("alloy/config.alloy", "Alloy configuration must be deployed")
require_deploy("prometheus/prometheus.yml", "Prometheus configuration must be deployed")
require_deploy("grafana/provisioning/datasources/datasource.yml", "Grafana configuration must be deployed")
for forbidden_path in (
    "/opt/jbnu-sugang-helper",
    "/opt/jbnu-sugang-helper-staging",
    "/home/ubuntu/jbnu-sugang-helper-staging",
    ".env.runtime",
    ".env.release",
    "flock",
):
    if forbidden_path in workflow or forbidden_path in deploy_script:
        raise SystemExit(f"legacy deploy path must not remain: {forbidden_path}")
print("Ubuntu SSH-only CD workflow contract passed")
PY
