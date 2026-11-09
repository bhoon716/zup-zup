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
cp -Rp "${BACKUP_FIXTURE_DIR}/." "${destination}/"
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

assert_audit_archives_survive_retention() {
  local fixture_dir="${temporary_dir}/audit-fixture"
  local local_dir="${temporary_dir}/audit-local"
  local output_file="${temporary_dir}/audit-output.log"
  mkdir -p "${fixture_dir}/audit/snapshot" "${local_dir}/audit/old"
  printf 'audit facts\n' > "${fixture_dir}/audit/snapshot/audit.sql.gz"
  printf 'rows=1\n' > "${fixture_dir}/audit/snapshot/audit.sql.gz.manifest"
  (cd "${fixture_dir}/audit/snapshot" && shasum -a 256 audit.sql.gz > audit.sql.gz.sha256 && shasum -a 256 audit.sql.gz.manifest > audit.sql.gz.manifest.sha256)
  printf 'old audit facts\n' > "${local_dir}/audit/old/audit.sql.gz"
  touch -t 202001010000 "${local_dir}/audit/old/audit.sql.gz"
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  test -f "${local_dir}/audit/old/audit.sql.gz"
  test -f "${local_dir}/audit/snapshot/audit.sql.gz"
  test -f "${local_dir}/audit/snapshot/audit.sql.gz.manifest"
  printf 'tampered\n' >> "${fixture_dir}/audit/snapshot/audit.sql.gz.manifest"
  if run_backup "${fixture_dir}" "${local_dir}" "${output_file}"; then
    echo 'tampered audit metadata must fail closed' >&2
    return 1
  fi
}
assert_audit_archives_survive_retention

assert_events_survive_retention_and_require_metadata() {
  local fixture_dir="${temporary_dir}/events-fixture"
  local local_dir="${temporary_dir}/events-local"
  local output_file="${temporary_dir}/events-output.log"
  mkdir -p "${fixture_dir}/events/snapshot" "${local_dir}/events/old"
  printf '{"event":"run_failed","level":"ERROR"}\n' | gzip > "${fixture_dir}/events/snapshot/events.jsonl.gz"
  printf '{"schemaVersion":1,"completeHistory":false}\n' > "${fixture_dir}/events/snapshot/events.jsonl.gz.manifest"
  (cd "${fixture_dir}/events/snapshot" && shasum -a 256 events.jsonl.gz > events.jsonl.gz.sha256 && shasum -a 256 events.jsonl.gz.manifest > events.jsonl.gz.manifest.sha256)
  printf 'old important facts\n' > "${local_dir}/events/old/events.jsonl.gz"
  touch -t 202001010000 "${local_dir}/events/old/events.jsonl.gz"
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  test -f "${local_dir}/events/old/events.jsonl.gz"
  test -f "${local_dir}/events/snapshot/events.jsonl.gz"
  test ! -e "${local_dir}/mysql/events"
  printf '{"schemaVersion":2}\n' > "${fixture_dir}/events/snapshot/events.jsonl.gz.manifest"
  (cd "${fixture_dir}/events/snapshot" && shasum -a 256 events.jsonl.gz.manifest > events.jsonl.gz.manifest.sha256)
  if run_backup "${fixture_dir}" "${local_dir}" "${output_file}"; then
    echo 'existing important facts must not be overwritten with changed metadata' >&2
    return 1
  fi
  rm "${fixture_dir}/events/snapshot/events.jsonl.gz.manifest" "${fixture_dir}/events/snapshot/events.jsonl.gz.manifest.sha256"
  if run_backup "${fixture_dir}" "${local_dir}" "${output_file}"; then
    echo 'important facts without metadata must fail closed' >&2
    return 1
  fi
}
assert_events_survive_retention_and_require_metadata

assert_full_backup_bundles_are_complete_and_immutable() {
  local fixture_dir="${temporary_dir}/bundle-fixture"
  local local_dir="${temporary_dir}/bundle-local"
  local output_file="${temporary_dir}/bundle-output.log"
  local bundle="export/full_backup_20261108T120000Z_fixture"
  local dump="sugang_helper_20261108T120000Z.sql.gz"
  mkdir -p "${fixture_dir}/${bundle}"
  printf 'fixture SQL\n' | gzip > "${fixture_dir}/${bundle}/${dump}"
  printf 'schema=full-backup-v1\ndatabase=sugang_helper\ndump_file=%s\nuploads=excluded\n' "$dump" > "${fixture_dir}/${bundle}/backup.manifest"
  (cd "${fixture_dir}/${bundle}" && shasum -a 256 "$dump" > "$dump.sha256" && shasum -a 256 backup.manifest > backup.manifest.sha256)
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  test -f "${local_dir}/mysql/${bundle}/${dump}"
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  printf 'created_at_utc=changed\n' >> "${fixture_dir}/${bundle}/backup.manifest"
  (cd "${fixture_dir}/${bundle}" && shasum -a 256 backup.manifest > backup.manifest.sha256)
  if run_backup "${fixture_dir}" "${local_dir}" "${output_file}"; then
    echo 'existing complete bundle must not be overwritten' >&2
    return 1
  fi
  ! grep -q changed "${local_dir}/mysql/${bundle}/backup.manifest"
  rm "${fixture_dir}/${bundle}/backup.manifest" "${fixture_dir}/${bundle}/backup.manifest.sha256"
  if run_backup "${fixture_dir}" "${local_dir}" "${output_file}"; then
    echo 'bundle without a manifest must fail closed' >&2
    return 1
  fi
}
assert_full_backup_bundles_are_complete_and_immutable

assert_bundle_retention_keeps_bundles_complete_across_repeated_sync() {
  local fixture_dir="${temporary_dir}/old-bundle-fixture"
  local local_dir="${temporary_dir}/old-bundle-local"
  local output_file="${temporary_dir}/old-bundle-output.log"
  local bundle="export/full_backup_20200101T000000Z_fixture"
  local dump="sugang_helper_20200101T000000Z.sql.gz"
  mkdir -p "${fixture_dir}/${bundle}"
  printf 'fixture SQL\n' | gzip > "${fixture_dir}/${bundle}/${dump}"
  printf 'schema=full-backup-v1\ndatabase=sugang_helper\ndump_file=%s\nuploads=excluded\n' "$dump" > "${fixture_dir}/${bundle}/backup.manifest"
  (cd "${fixture_dir}/${bundle}" && shasum -a 256 "$dump" > "$dump.sha256" && shasum -a 256 backup.manifest > backup.manifest.sha256)
  touch -t 202001010000 "${fixture_dir}/${bundle}/"*
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  test ! -e "${local_dir}/mysql/${bundle}"
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  # A bundle crossing the age boundary must stay complete until every file expires.
  touch "${fixture_dir}/${bundle}/backup.manifest.sha256"
  run_backup "${fixture_dir}" "${local_dir}" "${output_file}"
  test -f "${local_dir}/mysql/${bundle}/${dump}"
  test -f "${local_dir}/mysql/${bundle}/backup.manifest.sha256"
}
assert_bundle_retention_keeps_bundles_complete_across_repeated_sync

assert_checksum_failure_is_fail_closed
assert_verified_backup_is_promoted_before_retention

echo "MacBook off-host backup fail-closed contract passed"
