#!/usr/bin/env bash
set -euo pipefail

SOURCE_LOG_DIR="${SOURCE_LOG_DIR:-/var/log/jbnu-sugang-helper/server}"
SOURCE_STATE_ROOT="${SOURCE_STATE_ROOT:-/var/lib/jbnu-sugang-helper}"
BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/jbnu-sugang-helper/log-state}"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive="${BACKUP_ROOT}/log-state-${timestamp}.tar.gz"
staging_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${staging_dir}"
}

trap cleanup EXIT

mkdir -p "${BACKUP_ROOT}"
mkdir -p "${staging_dir}/var/log/jbnu-sugang-helper"
mkdir -p "${staging_dir}/var/lib/jbnu-sugang-helper"

cp -a "${SOURCE_LOG_DIR}" "${staging_dir}/var/log/jbnu-sugang-helper/server"
cp -a "${SOURCE_STATE_ROOT}/loki" "${staging_dir}/var/lib/jbnu-sugang-helper/"
cp -a "${SOURCE_STATE_ROOT}/promtail" "${staging_dir}/var/lib/jbnu-sugang-helper/"

tar -czf "${archive}" -C "${staging_dir}" var
printf '%s\n' "${archive}"
