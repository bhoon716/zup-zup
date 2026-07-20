#!/usr/bin/env bash
set -euo pipefail

if ! command -v nginx >/dev/null 2>&1; then
  echo "nginx is not installed on this host" >&2
  exit 1
fi

nginx -t

if [[ -n "${API_HOST:-}" ]]; then
  curl --fail --silent --show-error --max-time 10 "https://${API_HOST}/health/ready" >/dev/null
fi

echo "host Nginx syntax and optional readiness check passed"
