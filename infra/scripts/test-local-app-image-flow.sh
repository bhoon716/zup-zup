#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_env="${repo_root}/infra/.env.example"
compose_file="${repo_root}/infra/docker-compose.yml"
local_override="${repo_root}/infra/docker-compose.local.yml"
temporary_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

base_config="${temporary_dir}/base.json"
local_config="${temporary_dir}/local.json"

docker compose \
  --env-file "${compose_env}" \
  -f "${compose_file}" \
  config --format json >"${base_config}"

docker compose \
  --env-file "${compose_env}" \
  -f "${compose_file}" \
  -f "${local_override}" \
  config --format json >"${local_config}"

python - "${base_config}" "${local_config}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    base_app = json.load(handle)["services"]["app"]
with open(sys.argv[2], encoding="utf-8") as handle:
    local_app = json.load(handle)["services"]["app"]

if base_app.get("image") != "sugang-helper-app:latest":
    raise SystemExit(f"production compose must default to sugang-helper-app:latest: {base_app.get('image')!r}")

if local_app.get("image") != "sugang-helper-app:local":
    raise SystemExit(f"local compose must use sugang-helper-app:local: {local_app.get('image')!r}")

build = local_app.get("build", {})
if build.get("dockerfile") != "Dockerfile":
    raise SystemExit(f"local compose must build the server Dockerfile: {build!r}")
if local_app.get("pull_policy") != "build":
    raise SystemExit(f"local compose must build without pulling the app image: {local_app!r}")
PY

if ! grep -F -- 'APP_IMAGE_NAME="${app_image_name}"' "${repo_root}/infra/scripts/deploy-app.sh" >/dev/null; then
  echo "deployment must pass the built app image name to Compose" >&2
  exit 1
fi

echo "local and production app image flows are separated"
