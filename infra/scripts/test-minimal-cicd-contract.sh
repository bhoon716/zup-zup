#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
ci_file="${repo_root}/.github/workflows/ci.yml"
cd_file="${repo_root}/.github/workflows/cd.yml"
deploy_file="${repo_root}/infra/scripts/deploy-release.sh"
rollback_workflow="${repo_root}/.github/workflows/rollback.yml"
settings_file="${repo_root}/docs/operations/github-settings.md"
observability_contract="${repo_root}/infra/scripts/test-observability-contract.sh"

python3 - "${ci_file}" "${cd_file}" "${deploy_file}" "${settings_file}" "${observability_contract}" <<'PY'
import sys
from pathlib import Path

ci = Path(sys.argv[1]).read_text(encoding="utf-8")
cd = Path(sys.argv[2]).read_text(encoding="utf-8")
deploy = Path(sys.argv[3]).read_text(encoding="utf-8")
settings = Path(sys.argv[4]).read_text(encoding="utf-8")
observability = Path(sys.argv[5]).read_text(encoding="utf-8")

def require(value, message):
    if not value:
        raise SystemExit(message)

require("pull_request:" in ci, "CI must trigger from pull_request")
require("\n  push:\n" not in ci, "CI must not rerun on push main")
require("\n  workflow_dispatch:\n" not in ci, "CI must not expose a separate manual trigger")
require("concurrency:" not in ci, "PR CI concurrency must be removed")
require("pull_request:" not in cd, "CD must not own PR checks")
require("push:" in cd, "CD must trigger from push")
require("branches: [main]" in cd, "CD push trigger must be limited to main")
require("workflow_dispatch:" in cd, "CD must support manual SHA deployment")
require("workflow_run:" not in cd, "CD must not depend on workflow_run")
require("concurrency:" not in cd, "GitHub CD concurrency must be removed")
require("docker/build-push-action@v6" in cd, "CD must build the production image")
require("push: true" in cd, "CD must push the production image")
require("jbnu-deploy" in cd, "CD must use the fixed OCI deploy wrapper")
require("render-nginx-config.sh" not in cd, "app CD must not render Nginx configuration")
require("nginx/conf.d" not in cd and "nginx/snippets" not in cd, "app CD must not stage Nginx support files")
require("flock" in deploy, "deploy script must serialize on the OCI host")
require("flyway-migrate" in deploy, "deploy script must retain one-shot Flyway migration")
require("flyway-validate" not in deploy, "production deploy must not duplicate Flyway validation")
require("loki" in deploy and "alloy" in deploy and "grafana" in deploy, "production deploy must start the log stack")
require("loki.source.docker" in observability, "Alloy Docker log collection contract is missing")
require("apply_staged_nginx" not in deploy, "app deploy must not apply Nginx configuration")
require("main 직접 push 차단" in settings, "branch settings must keep only the minimal direct-push guard")
require("리뷰 승인 요구 없음" in settings, "branch settings must not require solo-developer review")
PY

if [ -e "${rollback_workflow}" ]; then
  echo "dedicated rollback workflow must be removed" >&2
  exit 1
fi

echo "minimal CI/CD contract passed"
