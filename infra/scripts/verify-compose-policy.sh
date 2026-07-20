#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
expected_file="${repo_root}/infra/docker-compose.yml"
compose_file="${1:-${expected_file}}"

if [ "$(cd "$(dirname "${compose_file}")" && pwd)/$(basename "${compose_file}")" != "${expected_file}" ]; then
  echo "only the minimal infra/docker-compose.yml contract is supported" >&2
  exit 1
fi

"${script_dir}/test-runtime-contract.sh"
