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
require("push:\n    branches: [main]", "CD must trigger from main push")
require("workflow_dispatch:", "manual dispatch trigger is missing")
require("image_tag:", "manual SHA input is missing")
if "workflow_run:" in workflow:
    raise SystemExit("CD must not depend on workflow_run")
if "concurrency:" in workflow:
    raise SystemExit("GitHub CD concurrency must be removed")
require("publish-image:", "production image publish job is missing")
require("docker/build-push-action@v6", "CD must build the production image")
require("push: true", "CD must push the production image")
require("needs: publish-image", "deploy must wait for the image publish job")
require("always()", "manual SHA deploy must run when publish job is skipped")
if "environment: production" in workflow:
    raise SystemExit("CD must use repository-level Actions secrets, not a production Environment")
for secret in ("SERVER_HOST", "SERVER_USER", "SSH_PRIVATE_KEY", "SERVER_DOTENV"):
    require(f"${{{{ secrets.{secret} }}}}", f"repository Actions secret is missing: {secret}")
require('target="${SERVER_USER}@${SERVER_HOST}"', "CD must use the configured server account")
require("install -d -o ${SERVER_USER} -g ${SERVER_USER}", "staging directory must belong to the configured server account")
require("ssh-keyscan", "runtime SSH host key setup is missing")
require("SSH_PRIVATE_KEY", "production SSH key is missing")
require("SERVER_DOTENV", "server dotenv secret is missing")
require('stage_dir}/apps/server/.env', "SERVER_DOTENV must be staged as apps/server/.env")
require("scp", "staging SCP transfer is missing")
require("sha256sum", "staging checksum validation is missing")
for forbidden in ("OCI_HOST", "OCI_KNOWN_HOSTS", "OCI_DEPLOY_USER", "DEPLOY_MANIFEST_PRIVATE_KEY", "SHA256SUMS.sig", "openssl dgst -sha256 -sign", "deploy-manifest-public.pem", ".env.app"):
    if forbidden in workflow or forbidden in deploy_script:
        raise SystemExit(f"SSH-only CD must not use {forbidden}")
require("config >/dev/null", "Compose config validation is missing")
require("test-release-layout-contract.sh", "release layout contract is missing")
require("test-observability-contract.sh", "observability contract is missing")
require("test-ghcr-auth-contract.sh", "GHCR auth contract is missing")
require("jbnu-deploy", "allowlisted deploy wrapper is missing")
require("df -P", "disk preflight is missing")
require("pull db redis", "MySQL/Redis image preflight is missing")
require("pull migrate", "Flyway image preflight is missing")
require("pull loki alloy grafana", "Loki/Alloy/Grafana image preflight is missing")
require("--profile observability", "observability profile must be deployed explicitly")
require("cp -a infra/alloy", "Alloy config must be staged")
require("cp -a infra/loki", "Loki config must be staged")
require("cp -a infra/grafana", "Grafana config must be staged")
require("IMAGE_TAG", "IMAGE_TAG contract is missing")
require("flyway-migrate", "Flyway migrate stage is missing")
if "flyway-validate" in deploy_script:
    raise SystemExit("production deploy must not duplicate Flyway validation")
require("127.0.0.1:8081/actuator/health/readiness", "readiness gate is missing")
require("pull app", "application image pull is missing")
require("after ${attempt} attempts", "application image pull retry is missing")
require(".env.release", "release state file is missing")
require("keep recent 3", "recent-three image cleanup contract is missing")
require("flock", "server-side deploy lock is missing")
for forbidden in ("render-nginx-config.sh", "nginx/conf.d", "nginx/snippets", "apply_staged_nginx", "systemctl reload nginx"):
    if forbidden in workflow or forbidden in deploy_script:
        raise SystemExit(f"app CD must not manage host Nginx: {forbidden}")
if "packages: write" not in workflow:
    raise SystemExit("GHCR publish job must receive packages: write")
if "contents: write" in workflow:
    raise SystemExit("CD workflow must not receive contents: write")

print("production CD workflow contract passed")
PY
