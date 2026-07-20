#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_file="${repo_root}/infra/docker-compose.yml"
output_file="$(mktemp)"

cleanup() {
  rm -f "${output_file}"
}
trap cleanup EXIT

set +e
env -i \
  HOME="${HOME:-}" \
  PATH="${PATH}" \
  docker compose \
    --env-file /dev/null \
    -f "${compose_file}" \
    config >"${output_file}" 2>&1
status=$?
set -e

if [ "${status}" -eq 0 ]; then
  echo "Compose config unexpectedly accepted a missing environment" >&2
  cat "${output_file}" >&2
  exit 1
fi

if grep -q 'Defaulting to a blank string' "${output_file}"; then
  echo "Compose still silently defaults missing variables to blank strings" >&2
  cat "${output_file}" >&2
  exit 1
fi

if ! grep -Eq 'must be set|environment variable.*required' "${output_file}"; then
  echo "Compose failed, but did not report a required environment variable" >&2
  cat "${output_file}" >&2
  exit 1
fi

echo "missing Compose environment is rejected before startup"
