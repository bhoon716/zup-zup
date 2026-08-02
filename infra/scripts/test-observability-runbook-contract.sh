#!/usr/bin/env bash
set -euo pipefail

if [ "${OBSERVABILITY_RUNBOOK_FAKE_DOCKER:-0}" = 1 ]; then
  capture_dir="${OBSERVABILITY_RUNBOOK_CAPTURE_DIR:?capture directory is required}"
  call_number_file="${capture_dir}/call-number"
  call_number=0
  if [ -f "${call_number_file}" ]; then
    call_number="$(cat "${call_number_file}")"
  fi
  call_number=$((call_number + 1))
  printf '%s\n' "${call_number}" >"${call_number_file}"
  printf '<%s>\n' "$@" >"${capture_dir}/call-${call_number}"

  environment_file_number=0
  previous_argument=""
  for argument in "$@"; do
    if [ "${previous_argument}" = --env-file ]; then
      environment_file_number=$((environment_file_number + 1))
      cp -- "${argument}" "${capture_dir}/env-${call_number}-${environment_file_number}"
      if [ "${environment_file_number}" -eq 2 ]; then
        # GNU/Linux uses -c; BSD/macOS uses -f. GNU stat -f can exit 0 with filesystem output.
        mode="$(stat -c '%a' "${argument}" 2>/dev/null || stat -f '%Lp' "${argument}")"
        printf '%s\n' "${mode}" >"${capture_dir}/diagnostics-env-mode"
      fi
    fi
    previous_argument="${argument}"
  done
  printf '%s\n' "${APP_BUILD_CONTEXT-}" >"${capture_dir}/shell-app-build-context-${call_number}"
  printf '%s\n' "${APP_ENV_FILE-}" >"${capture_dir}/shell-app-env-file-${call_number}"
  printf '%s\n' "${APP_PROD_CONFIG_PATH-}" >"${capture_dir}/shell-app-prod-config-${call_number}"
  printf '%s\n' "${FIREBASE_CONFIG_PATH-}" >"${capture_dir}/shell-firebase-config-${call_number}"
  exit 0
fi

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
wrapper="${repo_root}/infra/scripts/compose-observability-diagnostics.sh"
temporary_dir="$(mktemp -d)"
release_root="${temporary_dir}/release"
fake_bin="${temporary_dir}/bin"
capture_dir="${temporary_dir}/capture"

cleanup() {
  rm -rf -- "${temporary_dir}"
}
trap cleanup EXIT

if [ ! -x "${wrapper}" ]; then
  echo "persistent observability diagnostics wrapper is missing" >&2
  exit 1
fi

mkdir -p "${release_root}/scripts" "${fake_bin}" "${capture_dir}"
cp -- "${wrapper}" "${release_root}/scripts/compose-observability-diagnostics.sh"
chmod 0755 "${release_root}/scripts/compose-observability-diagnostics.sh"
python3 - "${release_root}/scripts/compose-observability-diagnostics.sh" "${release_root}" <<'PY'
from pathlib import Path
import sys

wrapper_path = Path(sys.argv[1])
release_root = sys.argv[2]
contents = wrapper_path.read_text(encoding="utf-8")
wrapper_path.write_text(
    contents.replace("/home/ubuntu/jbnu-sugang-helper", release_root),
    encoding="utf-8",
)
PY
printf '%s\n' \
  'APP_IMAGE_NAME=fixture/app' \
  'IMAGE_TAG=fixture' \
  'APP_ENV_FILE=missing-from-persistent-env' \
  >"${release_root}/.env"
printf '%s\n' 'name: sugang-helper' >"${release_root}/docker-compose.yml"
ln -s -- "${repo_root}/infra/scripts/test-observability-runbook-contract.sh" "${fake_bin}/docker"

run_wrapper() {
  PATH="${fake_bin}:${PATH}" \
  OBSERVABILITY_RUNBOOK_FAKE_DOCKER=1 \
  OBSERVABILITY_RUNBOOK_CAPTURE_DIR="${capture_dir}" \
    "${release_root}/scripts/compose-observability-diagnostics.sh" "$@"
}

run_wrapper ps
run_wrapper logs --tail=100 app alloy loki prometheus grafana
run_wrapper probe
run_wrapper start
APP_BUILD_CONTEXT=/tmp/wrong-build-context \
APP_ENV_FILE=/tmp/wrong-app-env \
APP_PROD_CONFIG_PATH=/tmp/wrong-app-config \
FIREBASE_CONFIG_PATH=/tmp/wrong-firebase \
PATH="${fake_bin}:${PATH}" \
OBSERVABILITY_RUNBOOK_FAKE_DOCKER=1 \
OBSERVABILITY_RUNBOOK_CAPTURE_DIR="${capture_dir}" \
  "${release_root}/scripts/compose-observability-diagnostics.sh" ps

for call_number in 1 2 3 4 5; do
  call_file="${capture_dir}/call-${call_number}"
  grep -Fqx '<compose>' "${call_file}"
  grep -Fqx '<--profile>' "${call_file}"
  grep -Fqx '<observability>' "${call_file}"
done
grep -Fqx '<ps>' "${capture_dir}/call-1"
grep -Fqx '<--all>' "${capture_dir}/call-1"
grep -Fqx '<logs>' "${capture_dir}/call-2"
grep -Fqx '<--tail=100>' "${capture_dir}/call-2"
grep -Fqx '<run>' "${capture_dir}/call-3"
grep -Fqx '<--rm>' "${capture_dir}/call-3"
grep -Fqx '<--no-deps>' "${capture_dir}/call-3"
grep -Fqx '<observability-probe-tools>' "${capture_dir}/call-3"
grep -Fqx '<up>' "${capture_dir}/call-4"
grep -Fqx '<-d>' "${capture_dir}/call-4"
grep -Fqx '<--wait>' "${capture_dir}/call-4"
grep -Fqx '<--wait-timeout>' "${capture_dir}/call-4"
grep -Fqx '<-f>' "${capture_dir}/call-1"
grep -Fqx '<--project-name>' "${capture_dir}/call-1"
grep -Fqx '<ps>' "${capture_dir}/call-5"
grep -Fqx '<--all>' "${capture_dir}/call-5"

diagnostics_env="${capture_dir}/env-1-2"
grep -Fqx "APP_BUILD_CONTEXT=${release_root}" "${diagnostics_env}"
grep -Fqx "APP_ENV_FILE=${release_root}/apps/server/.env" "${diagnostics_env}"
grep -Fqx "APP_PROD_CONFIG_PATH=${release_root}/application-prod.yml" "${diagnostics_env}"
grep -Fqx "FIREBASE_CONFIG_PATH=${release_root}/secrets/firebase-key.json" "${diagnostics_env}"
[ "$(cat "${capture_dir}/diagnostics-env-mode")" = 600 ]
[ "$(find "${release_root}" -maxdepth 1 -name '.compose-diagnostics.*' -print -quit)" = "" ]
grep -Fqx "${release_root}" "${capture_dir}/shell-app-build-context-5"
grep -Fqx "${release_root}/apps/server/.env" "${capture_dir}/shell-app-env-file-5"
grep -Fqx "${release_root}/application-prod.yml" "${capture_dir}/shell-app-prod-config-5"
grep -Fqx "${release_root}/secrets/firebase-key.json" "${capture_dir}/shell-firebase-config-5"

printf 'observability runbook wrapper contract passed\n'
