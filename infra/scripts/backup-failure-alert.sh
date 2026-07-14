#!/usr/bin/env bash
set -euo pipefail

unit_name="${1:-unknown-backup-unit}"
webhook_url="${BACKUP_FAILURE_WEBHOOK_URL:-}"
message="jbnu-sugang-helper backup unit failed: ${unit_name}"

if [ -z "${webhook_url}" ]; then
  logger -t jbnu-sugang-helper-backup "${message}; BACKUP_FAILURE_WEBHOOK_URL is not configured"
  exit 1
fi

payload="$(python3 -c 'import json,sys; print(json.dumps({"content": sys.argv[1]}))' "${message}")"
"${CURL_BIN:-curl}" -fsS \
  -H 'Content-Type: application/json' \
  --data "${payload}" \
  "${webhook_url}" >/dev/null
