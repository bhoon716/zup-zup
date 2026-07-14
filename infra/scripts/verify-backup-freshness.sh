#!/usr/bin/env bash
set -euo pipefail

dr_backup_root="${DR_BACKUP_ROOT:-/var/backups/jbnu-sugang-helper/dr-state}"
redis_backup_root="${REDIS_BACKUP_ROOT:-/var/backups/jbnu-sugang-helper/redis-state}"
max_age_hours="${BACKUP_MAX_AGE_HOURS:-26}"

if [[ ! "${max_age_hours}" =~ ^[0-9]+$ ]] || [ "${max_age_hours}" -eq 0 ]; then
  echo "BACKUP_MAX_AGE_HOURS must be a positive integer" >&2
  exit 1
fi

file_mtime() {
  stat -c '%Y' "$1" 2>/dev/null || stat -f '%m' "$1"
}

latest_archive() {
  local root="$1"
  local pattern="$2"
  local latest=""
  local latest_mtime=0
  local candidate candidate_mtime

  if [ ! -d "${root}" ]; then
    return 1
  fi

  while IFS= read -r -d '' candidate; do
    candidate_mtime="$(file_mtime "${candidate}")"
    if [ "${candidate_mtime}" -gt "${latest_mtime}" ]; then
      latest="${candidate}"
      latest_mtime="${candidate_mtime}"
    fi
  done < <(find "${root}" -maxdepth 1 -type f -name "${pattern}" -print0)

  [ -n "${latest}" ]
  printf '%s\n' "${latest}"
}

validate_fresh_archive() {
  local label="$1"
  local archive="$2"
  local now age
  now="$(date +%s)"
  age=$((now - $(file_mtime "${archive}")))
  if [ "${age}" -lt 0 ] || [ "${age}" -gt $((max_age_hours * 3600)) ]; then
    echo "${label} backup is older than ${max_age_hours} hours: ${archive}" >&2
    return 1
  fi
  printf '%s\n' "${archive}"
}

dr_archive="$(latest_archive "${dr_backup_root}" 'dr-state-*.tar.gz.enc')" || {
  echo "no DR backup archive found under ${dr_backup_root}" >&2
  exit 1
}
redis_archive="$(latest_archive "${redis_backup_root}" 'redis-state-*.tar.gz')" || {
  echo "no Redis backup archive found under ${redis_backup_root}" >&2
  exit 1
}

for sidecar in "${dr_archive}.sha256" "${dr_archive}.hmac" "${redis_archive}.sha256"; do
  if [ ! -s "${sidecar}" ]; then
    echo "backup sidecar is missing or empty: ${sidecar}" >&2
    exit 1
  fi
done

validate_fresh_archive DR "${dr_archive}" >/dev/null
validate_fresh_archive Redis "${redis_archive}" >/dev/null
printf 'backup freshness verification passed\n'
