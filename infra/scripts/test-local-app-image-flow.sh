#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_env="${repo_root}/infra/.env.example"
compose_file="${repo_root}/infra/docker-compose.yml"
temporary_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

base_config="${temporary_dir}/base.json"

docker compose \
  --env-file "${compose_env}" \
  -f "${compose_file}" \
  config --format json >"${base_config}"

python - "${base_config}" <<'PY'
import json
import sys
from pathlib import Path

with open(sys.argv[1], encoding="utf-8") as handle:
    base_app = json.load(handle)["services"]["app"]

if base_app.get("image") != "sugang-helper-app:latest":
    raise SystemExit(f"Compose must default to sugang-helper-app:latest: {base_app.get('image')!r}")

build = base_app.get("build", {})
if build.get("dockerfile") != "Dockerfile":
    raise SystemExit(f"Compose must build the server Dockerfile: {build!r}")
if Path(build.get("context", "")).parent.name != "apps":
    raise SystemExit(f"Compose build context must point to the apps directory: {build!r}")
PY

if ! grep -F -- 'APP_IMAGE_NAME="${app_image_name}"' "${repo_root}/infra/scripts/deploy-app.sh" >/dev/null; then
  echo "deployment must pass the built app image name to Compose" >&2
  exit 1
fi

echo "local and production app image flows are separated"
