#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"

for required in \
  'readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"' \
  'readonly STAGING_ROOT="${RELEASE_ROOT}/.staging"' \
  'APP_ENV_FILE="${RELEASE_ROOT}/apps/server/.env"' \
  'RUNTIME_ENV="${RELEASE_ROOT}/.env"' \
  'rm -rf -- "${staging_dir}"'; do
  if ! grep -F -- "${required}" "${deploy_script}" >/dev/null; then
    echo "release layout is missing: ${required}" >&2
    exit 1
  fi
done

fixture_root="$(mktemp -d)"
cleanup() {
  rm -rf -- "${fixture_root}"
}
trap cleanup EXIT

mkdir -p "${fixture_root}/source" "${fixture_root}/destination"
printf 'current\n' >"${fixture_root}/source/current.conf"
printf 'stale\n' >"${fixture_root}/destination/stale.conf"

sync_function="$(sed -n '/^sync_directory() {/,/^}/p' "${deploy_script}")"
[ -n "${sync_function}" ] || {
  echo "deploy script must define sync_directory" >&2
  exit 1
}
eval "${sync_function}"

sync_directory "${fixture_root}/source" "${fixture_root}/destination"
if [ -e "${fixture_root}/destination/stale.conf" ] \
  || [ "$(cat "${fixture_root}/destination/current.conf")" != "current" ]; then
  echo "release synchronization retained stale files" >&2
  exit 1
fi

printf 'preserve\n' >"${fixture_root}/destination/preserve.conf"
if sync_directory "${fixture_root}/missing" "${fixture_root}/destination" 2>/dev/null; then
  echo "release synchronization accepted a missing source" >&2
  exit 1
fi
if [ "$(cat "${fixture_root}/destination/preserve.conf")" != "preserve" ]; then
  echo "failed synchronization damaged the current release" >&2
  exit 1
fi

echo "release synchronization contract passed"
