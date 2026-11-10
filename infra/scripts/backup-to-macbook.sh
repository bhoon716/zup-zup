#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${SERVER_HOST:-api.zup-zup.com}"
SERVER_USER="${SERVER_USER:-ubuntu}"
REMOTE_BACKUP_DIR="${REMOTE_BACKUP_DIR:-/home/ubuntu/jbnu-sugang-helper/backups}"
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-$HOME/Backups/jbnu-sugang-helper}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

if ! [[ "${RETENTION_DAYS}" =~ ^[1-9][0-9]{0,3}$ ]]; then
  echo "RETENTION_DAYS는 1부터 9999 사이의 정수여야 합니다." >&2
  exit 1
fi
umask 077

log_important_metadata_missing() {
  printf '{"event":"offhost_backup_rejected","status":"error","reason":"important_metadata_missing","message":"중요 기록 메타데이터가 없어 승격을 중단했습니다."}\n' >&2
}

log_important_archive_conflict() {
  printf '{"event":"offhost_backup_rejected","status":"error","reason":"important_archive_conflict","message":"기존 중요 기록과 내용이 달라 덮어쓰기를 거부했습니다."}\n' >&2
}

log_full_bundle_invalid() {
  printf '{"event":"offhost_backup_rejected","status":"error","reason":"full_bundle_invalid","message":"전체 백업 묶음이 완전하지 않아 승격을 중단했습니다."}\n' >&2
}

validate_full_bundle() {
  local directory="$1"
  local manifest="${directory}/backup.manifest"
  local dump uploads_mode uploads
  [ -f "$manifest" ] && [ -f "${manifest}.sha256" ] || return 1
  grep -qx 'schema=full-backup-v1' "$manifest" && grep -qx 'database=sugang_helper' "$manifest" || return 1
  dump="$(sed -n 's/^dump_file=//p' "$manifest")"
  [[ "$dump" =~ ^sugang_helper_[0-9]{8}T[0-9]{6}Z\.sql\.gz$ ]] || return 1
  [ -f "${directory}/${dump}" ] && [ -f "${directory}/${dump}.sha256" ] || return 1
  uploads_mode="$(sed -n 's/^uploads=//p' "$manifest")"
  if [ "$uploads_mode" = excluded ]; then
    ! grep -q '^uploads_file=' "$manifest"
    return
  fi
  [ "$uploads_mode" = included ] || return 1
  uploads="$(sed -n 's/^uploads_file=//p' "$manifest")"
  [[ "$uploads" =~ ^uploads_[0-9]{8}T[0-9]{6}Z\.tar\.gz$ ]] || return 1
  [ -f "${directory}/${uploads}" ] && [ -f "${directory}/${uploads}.sha256" ]
}

log_full_bundle_rotation_completed() {
  printf '{"event":"offhost_bundle_rotation_completed","status":"ok","deletedBundles":%s,"message":"보존 기간이 지난 전체 백업 묶음을 정리했습니다."}\n' "$1"
}

prune_full_bundles() {
  local bundle
  local deleted=0
  for bundle in "${LOCAL_BACKUP_DIR}/mysql/export"/full_backup_*; do
    [ -d "$bundle" ] || continue
    validate_full_bundle "$bundle" || continue
    # Keep a bundle intact until every artifact has passed the existing local retention.
    if [ -n "$(find "$bundle" -type f ! -mtime +"${RETENTION_DAYS}" -print -quit)" ]; then
      continue
    fi
    rm -rf -- "$bundle"
    deleted=$((deleted + 1))
  done
  log_full_bundle_rotation_completed "$deleted"
}

TIMESTAMP="$(date +'%Y-%m-%d %H:%M:%S')"
echo "======================================================================"
echo "[MACBOOK-OFFHOST-BACKUP] ${TIMESTAMP}"
echo "${SERVER_USER}@${SERVER_HOST}:${REMOTE_BACKUP_DIR}에서 백업을 동기화합니다."
echo "로컬 백업 경로: ${LOCAL_BACKUP_DIR}"
echo "======================================================================"

mkdir -p "${LOCAL_BACKUP_DIR}/mysql"
mkdir -p "${LOCAL_BACKUP_DIR}/uploads"
mkdir -p "${LOCAL_BACKUP_DIR}/logs"
chmod 0700 "${LOCAL_BACKUP_DIR}"

LOG_FILE="${LOCAL_BACKUP_DIR}/logs/sync_$(date +'%Y%m%d').log"
STAGING_DIR="$(mktemp -d "${LOCAL_BACKUP_DIR}/.incoming.XXXXXX")"

cleanup() {
  local status=$?
  rm -rf "${STAGING_DIR}"
  exit "${status}"
}
trap cleanup EXIT

echo "[1/4] rsync로 MySQL 덤프와 백업 압축본을 임시 경로에 복사합니다." | tee -a "${LOG_FILE}"
rsync -avz --exclude=".pending.*" --exclude=".partial-events-*" --include="*.manifest" --include="*.sql.gz" --include="*.jsonl.gz" --include="*.tar.gz" --include="*.sha256" --include="*/" --exclude="*" \
  -e "ssh -o StrictHostKeyChecking=accept-new" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_BACKUP_DIR}/" \
  "${STAGING_DIR}/" | tee -a "${LOG_FILE}"

echo "[2/4] 맥북에서 SHA-256 체크섬을 검증합니다." | tee -a "${LOG_FILE}"
CHECKSUM_ERRORS=0
CHECKSUM_FILES=0
while IFS= read -r -d '' sha_file; do
  CHECKSUM_FILES=$((CHECKSUM_FILES + 1))
  artifact="${sha_file%.sha256}"
  relative_path="${sha_file#"${STAGING_DIR}/"}"
  echo "체크섬 확인 중: ${relative_path}" | tee -a "${LOG_FILE}"
  expected_hash="$(awk 'NR == 1 { print $1 }' "${sha_file}")"
  if [ ! -f "${artifact}" ]; then
    echo "ERROR: ${relative_path}에 해당하는 백업 파일이 없습니다." | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  elif ! [[ "${expected_hash}" =~ ^[[:xdigit:]]{64}$ ]]; then
    echo "ERROR: 체크섬 목록이 올바르지 않습니다. 경로: ${relative_path}" | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  else
    actual_hash="$(shasum -a 256 "${artifact}" | awk '{ print $1 }')"
    if [ "${actual_hash}" = "${expected_hash}" ]; then
      echo "OK: ${relative_path}" | tee -a "${LOG_FILE}"
    else
      echo "ERROR: 체크섬이 일치하지 않습니다. 경로: ${relative_path}" | tee -a "${LOG_FILE}"
      CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
    fi
  fi
done < <(find "${STAGING_DIR}" -type f -name "*.sha256" -print0)

while IFS= read -r -d '' artifact; do
  if [ ! -f "${artifact}.sha256" ]; then
    relative_path="${artifact#"${STAGING_DIR}/"}"
    echo "ERROR: ${relative_path}에 해당하는 체크섬 목록이 없습니다." | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  fi
done < <(find "${STAGING_DIR}" -type f \( -name "*.sql.gz" -o -name "*.jsonl.gz" -o -name "*.tar.gz" -o -name "*.manifest" \) -print0)

# Important snapshots are only useful when their coverage/schema metadata travels with them.
while IFS= read -r -d '' artifact; do
  if [ ! -f "${artifact}.manifest" ] || [ ! -f "${artifact}.manifest.sha256" ]; then
    log_important_metadata_missing
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  fi
done < <(find "${STAGING_DIR}" -type f \( \( -path '*/audit/*' -name '*.sql.gz' \) -o \( -path '*/events/*' -name '*.jsonl.gz' \) \) -print0)

while IFS= read -r -d '' bundle; do
  if ! validate_full_bundle "$bundle"; then
    log_full_bundle_invalid
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  fi
done < <(find "${STAGING_DIR}/export" -type d -name 'full_backup_*' -print0 2>/dev/null)

if [ "${CHECKSUM_FILES}" -eq 0 ]; then
  echo "[ERROR] 내려받은 체크섬 목록이 없어 백업 저장과 보관 기간 정리를 중단합니다." | tee -a "${LOG_FILE}"
  exit 1
fi

if [ "${CHECKSUM_ERRORS}" -gt 0 ]; then
  echo "[ERROR] 체크섬 검증 ${CHECKSUM_ERRORS}건이 실패해 백업 저장과 보관 기간 정리를 중단합니다." \
    | tee -a "${LOG_FILE}"
  exit 1
fi
echo "[SUCCESS] 모든 체크섬 검증을 완료했습니다." | tee -a "${LOG_FILE}"

echo "[3/4] 검증한 백업을 보관 경로로 옮깁니다." | tee -a "${LOG_FILE}"

promote_full_bundle() {
  local source_dir="$1"
  local relative_path="${source_dir#"${STAGING_DIR}/"}"
  local target_dir="${LOCAL_BACKUP_DIR}/mysql/${relative_path}"
  local temporary_dir
  if [ -e "$target_dir" ]; then
    if ! diff -qr "$source_dir" "$target_dir" >/dev/null 2>&1; then
      log_important_archive_conflict
      return 1
    fi
    rm -rf -- "$source_dir"
    return
  fi
  mkdir -p "$(dirname "$target_dir")"
  temporary_dir="$(mktemp -d "$(dirname "$target_dir")/.bundle-incoming.XXXXXX")"
  if ! cp -Rp "${source_dir}/." "${temporary_dir}/"; then
    rm -rf -- "$temporary_dir"
    return 1
  fi
  # rename publishes all verified artifacts at once and never merges into an existing directory.
  if ! python3 - "$temporary_dir" "$target_dir" <<'PY'
import os, sys
os.rename(sys.argv[1], sys.argv[2])
PY
  then
    rm -rf -- "$temporary_dir"
    return 1
  fi
  rm -rf -- "$source_dir"
}

while IFS= read -r -d '' bundle; do
  promote_full_bundle "$bundle"
done < <(find "${STAGING_DIR}/export" -type d -name 'full_backup_*' -print0 2>/dev/null)

promote_file() {
  local source_file="$1"
  local relative_path="${source_file#"${STAGING_DIR}/"}"
  local target_file="${LOCAL_BACKUP_DIR}/mysql/${relative_path}"
  if [[ "${relative_path}" == audit/* || "${relative_path}" == events/* ]]; then
    target_file="${LOCAL_BACKUP_DIR}/${relative_path}"
  fi
  local target_dir
  local temporary_target="${target_file}.incoming.$$"

  if [[ "${relative_path}" == audit/* || "${relative_path}" == events/* ]] && [ -f "${target_file}" ]; then
    if ! cmp -s "${source_file}" "${target_file}"; then
      log_important_archive_conflict
      return 1
    fi
    return
  fi
  target_dir="$(dirname "${target_file}")"
  mkdir -p "${target_dir}"
  cp -p "${source_file}" "${temporary_target}"
  mv -f "${temporary_target}" "${target_file}"
}

while IFS= read -r -d '' source_file; do
  promote_file "${source_file}"
done < <(find "${STAGING_DIR}" -type f ! -name "*.sha256" -print0)
while IFS= read -r -d '' source_file; do
  promote_file "${source_file}"
done < <(find "${STAGING_DIR}" -type f -name "*.sha256" -print0)

echo "[4/4] 맥북에서 ${RETENTION_DAYS}일이 지난 백업을 정리합니다." | tee -a "${LOG_FILE}"
find "${LOCAL_BACKUP_DIR}/mysql" -type f ! -path '*/full_backup_*/*' -mtime +"${RETENTION_DAYS}" -delete
prune_full_bundles
find "${LOCAL_BACKUP_DIR}/logs" -type f -mtime +"${RETENTION_DAYS}" -delete

echo "[MACBOOK-OFFHOST-BACKUP] 백업 동기화 완료 시각: $(date +'%Y-%m-%d %H:%M:%S')" | tee -a "${LOG_FILE}"
