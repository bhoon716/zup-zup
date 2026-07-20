#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="${repo_root}/infra/scripts/bootstrap-nginx.sh"

for required in \
  'certbot certonly' \
  '--webroot' \
  'nginx -t' \
  'systemctl reload nginx' \
  '00-sugang-helper-rate-limit.conf' \
  'jbnu-sugang-helper-proxy.conf'; do
  if ! grep -F -- "${required}" "${script}" >/dev/null; then
    echo "Nginx bootstrap contract is missing: ${required}" >&2
    exit 1
  fi
done

if grep -F -- 'ssl_certificate /etc/letsencrypt' "${script}" >/dev/null; then
  echo "ACME bootstrap must not require TLS certificates before certbot runs" >&2
  exit 1
fi

echo "Nginx bootstrap contract passed"
