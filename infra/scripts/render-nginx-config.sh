#!/usr/bin/env bash
set -euo pipefail

api_host="${API_HOST:-}"
output_file="${1:-}"
template_file="$(cd "$(dirname "$0")/.." && pwd)/nginx/sites-available/jbnu-sugang-helper.conf.example"

if [[ -z "${api_host}" || -z "${output_file}" ]]; then
  echo "usage: API_HOST=<fqdn> $0 <output-file>" >&2
  exit 1
fi

if [[ ! "${api_host}" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]]; then
  echo "API_HOST must be a DNS hostname without a scheme or path" >&2
  exit 1
fi

mkdir -p "$(dirname "${output_file}")"
sed "s/<API_HOST>/${api_host}/g" "${template_file}" >"${output_file}.tmp"
if grep -q '<API_HOST>' "${output_file}.tmp"; then
  rm -f "${output_file}.tmp"
  echo "nginx template still contains a placeholder" >&2
  exit 1
fi
mv -f "${output_file}.tmp" "${output_file}"
chmod 0644 "${output_file}"
echo "rendered Nginx site for ${api_host}: ${output_file}"
