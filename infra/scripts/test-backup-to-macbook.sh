#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
backup_script="${repo_root}/infra/scripts/backup-to-macbook.sh"
temporary_dir="$(mktemp -d)"
stub_bin="${temporary_dir}/bin"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

mkdir -p "${stub_bin}"
cat >"${stub_bin}/rsync" <<'RSYNC'
#!/usr/bin/env bash
set -euo pipefail
destination="${@: -1}"
mkdir -p "${destination}"
cp -R "${BACKUP_FIXTURE_DIR}/." "${destination}/"
RSYNC
chmod +x "${stub_bin}/rsync"

run_backup() {
  local fixture_dir="$1"
  local local_dir="$2"
  local output_file="$3"

  BACKUP_FIXTURE_DIR="${fixture_dir}" \
    LOCAL_BACKUP_DIR="${local_dir}" \
    RETENTION_DAYS=30 \
    PATH="${stub_bin}:${PATH}" \
    bash "${backup_script}" >"${output_file}" 2>&1
}

assert_checksum_failure_is_fail_closed() {
  local fixture_dir="${temporary_dir}/mismatch-fixture"
  local local_dir="${temporary_dir}/mismatch-local"
  local output_file="${temporary_dir}/mismatch-output.log"
  mkdir -p "${fixture_dir}/export" "${local_dir}/mysql"

  printf 'corrupted backup\n' >"${fixture_dir}/export/latest.sql.gz"
  printf '%064d  /remote/export/latest.sql.gz\n' 0 \
    >"${fixture_dir}/export/latest.sql.gz.sha256"
  printf 'known good backup\n' >"${local_dir}/mysql/known-good.sql.gz"
  touch -t 202001010000 "${local_dir}/mysql/known-good.sql.gz"

  if run_backup "${fixture_dir}" "${local_dir}" "${output_file}"; then
    echo "checksum mismatch must return a non-zero status" >&2
    return 1
  fi
  if [ ! -f "${local_dir}/mysql/known-good.sql.gz" ]; then
    echo "checksum failure must not run retention cleanup" >&2
    return 1
  fi
  if [ -f "${local_dir}/mysql/export/latest.sql.gz" ]; then
    echo "unverified backup must not be promoted" >&2
    return 1
  fi
  if grep -Fq 'Cleaning up MacBook backups' "${output_file}"; then
    echo "checksum failure must stop before retention" >&2
    return 1
  fi
}

assert_verified_backup_is_promoted_before_retention() {
  local fixture_dir="${temporary_dir}/verified-fixture"
  local local_dir="${temporary_dir}/verified-local"
  local output_file="${temporary_dir}/verified-output.log"
  local checksum
  mkdir -p "${fixture_dir}/export" "${local_dir}/mysql"

  printf 'verified backup\n' >"${fixture_dir}/export/latest.sql.gz"
  checksum="$(shasum -a 256 "${fixture_dir}/export/latest.sql.gz" | awk '{print $1}')"
  printf '%s  /remote/export/latest.sql.gz\n' "${checksum}" \
    >"${fixture_dir}/export/latest.sql.gz.sha256"
  printf 'expired backup\n' >"${local_dir}/mysql/expired.sql.gz"
  touch -t 202001010000 "${local_dir}/mysql/expired.sql.gz"

  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"

  if [ ! -f "${local_dir}/mysql/export/latest.sql.gz" ] \
      || [ ! -f "${local_dir}/mysql/export/latest.sql.gz.sha256" ]; then
    echo "verified backup and checksum must be promoted together" >&2
    return 1
  fi
  if [ -f "${local_dir}/mysql/expired.sql.gz" ]; then
    echo "retention must run after successful verification" >&2
    return 1
  fi
}

assert_checksum_failure_is_fail_closed
assert_verified_backup_is_promoted_before_retention

echo "MacBook off-host backup fail-closed contract passed"
