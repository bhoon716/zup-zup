#!/usr/bin/env bash
set -euo pipefail

api_host="${API_HOST:-}"
if [[ -z "${api_host}" ]]; then
  echo "API_HOST=<fqdn> is required" >&2
  exit 1
fi

response="$(curl --fail --silent --show-error --max-time 10 "https://${api_host}/health")"
if grep -Eiq 'password|secret|jdbc|redis|mysql|exception|stacktrace' <<<"${response}"; then
  echo "readiness response appears to expose internal details" >&2
  exit 1
fi

echo "external health endpoint returned HTTP 200 without sensitive detail"
