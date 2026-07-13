#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_file="${repo_root}/infra/docker-compose.yml"
compose_env="${COMPOSE_ENV_FILE:-${repo_root}/infra/.env.example}"
app_env="${APP_ENV_FILE:-${repo_root}/apps/server/.env.example}"

COMPOSE_ENV_FILE="${compose_env}" APP_ENV_FILE="${app_env}" \
  bash "${repo_root}/infra/scripts/verify-compose-policy.sh" "${compose_file}"

config="$(docker compose --env-file "${compose_env}" -f "${compose_file}" config --format json)"
python -c 'import json, sys; services=json.loads(sys.stdin.read())["services"]; assert "alertmanager" in services; assert "prometheus" in services; assert any(v.get("target")=="/prometheus" for v in services["prometheus"]["volumes"]); print("observability config smoke passed")' <<< "${config}"
