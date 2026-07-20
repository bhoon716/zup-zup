#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="${repo_root}/infra/scripts/prepare-app-host-directories.sh"

for required in 'mountpoint -q' 'findmnt -n -T' 'DB_DATA_DIR must be an actual mounted'; do
  if ! grep -F -- "${required}" "${script}" >/dev/null; then
    echo "DB host preparation must fail closed on an unmounted data path: ${required}" >&2
    exit 1
  fi
done

if ! grep -F -- '/var/lib/jbnu-sugang-helper/prometheus' "${script}" >/dev/null; then
  echo "host preparation must create the Prometheus data directory" >&2
  exit 1
fi

echo "DB mount contract passed"
