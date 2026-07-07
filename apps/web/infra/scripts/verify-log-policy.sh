#!/usr/bin/env bash
set -euo pipefail

compose_file="${1:-docker-compose.yml}"

if [ ! -f "${compose_file}" ]; then
  echo "compose file not found: ${compose_file}" >&2
  exit 1
fi

config_output="$(docker compose -f "${compose_file}" config)"

grep -F -- "--binlog-expire-logs-seconds=172800" <<<"${config_output}" >/dev/null

grep -F -- "driver: json-file" <<<"${config_output}" >/dev/null
grep -F -- "max-size: 10m" <<<"${config_output}" >/dev/null
grep -F -- "max-file: \"5\"" <<<"${config_output}" >/dev/null

echo "log policy verification passed"
