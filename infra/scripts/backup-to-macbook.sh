#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# MacBook Off-Host Backup Sync Script
# Runs on Local MacBook to pull DB & Uploads backups from OCI Server
# ==============================================================================

SERVER_HOST="${SERVER_HOST:-api.zup-zup.com}"
SERVER_USER="${SERVER_USER:-ubuntu}"
REMOTE_BACKUP_DIR="${REMOTE_BACKUP_DIR:-/home/ubuntu/jbnu-sugang-helper/backups}"
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-$HOME/Backups/jbnu-sugang-helper}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

TIMESTAMP="$(date +'%Y-%m-%d %H:%M:%S')"
echo "======================================================================"
echo "[MACBOOK-OFFHOST-BACKUP] ${TIMESTAMP}"
echo "Syncing backups from ${SERVER_USER}@${SERVER_HOST}:${REMOTE_BACKUP_DIR}"
echo "Local Target Directory: ${LOCAL_BACKUP_DIR}"
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

# Step 1: Sync into an isolated staging directory
echo "[1/4] Syncing MySQL dumps and export archives into staging via rsync..." | tee -a "${LOG_FILE}"
rsync -avz --include="*.sql.gz" --include="*.tar.gz" --include="*.sha256" --include="*/" --exclude="*" \
  -e "ssh -o StrictHostKeyChecking=accept-new" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_BACKUP_DIR}/" \
  "${STAGING_DIR}/" | tee -a "${LOG_FILE}"

# Step 2: Verify Checksums on MacBook
echo "[2/4] Verifying SHA-256 checksums on MacBook..." | tee -a "${LOG_FILE}"
CHECKSUM_ERRORS=0
CHECKSUM_FILES=0
while IFS= read -r -d '' sha_file; do
  CHECKSUM_FILES=$((CHECKSUM_FILES + 1))
  artifact="${sha_file%.sha256}"
  relative_path="${sha_file#"${STAGING_DIR}/"}"
  echo "Checking ${relative_path}..." | tee -a "${LOG_FILE}"
  expected_hash="$(awk 'NR == 1 { print $1 }' "${sha_file}")"
  if [ ! -f "${artifact}" ]; then
    echo "ERROR: Missing backup artifact for ${relative_path}" | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  elif ! [[ "${expected_hash}" =~ ^[[:xdigit:]]{64}$ ]]; then
    echo "ERROR: Invalid checksum manifest ${relative_path}" | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  else
    actual_hash="$(shasum -a 256 "${artifact}" | awk '{ print $1 }')"
    if [ "${actual_hash}" = "${expected_hash}" ]; then
      echo "OK: ${relative_path}" | tee -a "${LOG_FILE}"
    else
      echo "ERROR: Checksum mismatch for ${relative_path}" | tee -a "${LOG_FILE}"
      CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
    fi
  fi
done < <(find "${STAGING_DIR}" -type f -name "*.sha256" -print0)

while IFS= read -r -d '' artifact; do
  if [ ! -f "${artifact}.sha256" ]; then
    relative_path="${artifact#"${STAGING_DIR}/"}"
    echo "ERROR: Missing checksum manifest for ${relative_path}" | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  fi
done < <(find "${STAGING_DIR}" -type f \( -name "*.sql.gz" -o -name "*.tar.gz" \) -print0)

if [ "${CHECKSUM_FILES}" -eq 0 ]; then
  echo "[ERROR] No checksum manifests were downloaded; refusing to promote or prune backups." | tee -a "${LOG_FILE}"
  exit 1
fi

if [ "${CHECKSUM_ERRORS}" -gt 0 ]; then
  echo "[ERROR] ${CHECKSUM_ERRORS} checksum verification failure(s); refusing to promote or prune backups." \
    | tee -a "${LOG_FILE}"
  exit 1
fi
echo "[SUCCESS] All checksums verified successfully!" | tee -a "${LOG_FILE}"

# Step 3: Promote only files from the fully verified staging set.
echo "[3/4] Promoting verified backups..." | tee -a "${LOG_FILE}"
promote_file() {
  local source_file="$1"
  local relative_path="${source_file#"${STAGING_DIR}/"}"
  local target_file="${LOCAL_BACKUP_DIR}/mysql/${relative_path}"
  local target_dir
  local temporary_target="${target_file}.incoming.$$"

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

# Step 4: Retention Cleanup (Delete backups older than RETENTION_DAYS)
echo "[4/4] Cleaning up MacBook backups older than ${RETENTION_DAYS} days..." | tee -a "${LOG_FILE}"
find "${LOCAL_BACKUP_DIR}/mysql" -type f -mtime +"${RETENTION_DAYS}" -delete
find "${LOCAL_BACKUP_DIR}/logs" -type f -mtime +"${RETENTION_DAYS}" -delete

echo "[MACBOOK-OFFHOST-BACKUP] Backup sync completed cleanly at $(date +'%Y-%m-%d %H:%M:%S')" | tee -a "${LOG_FILE}"
