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

# Step 1: Sync MySQL dumps and export files
echo "[1/3] Syncing MySQL dumps and export archives via rsync..." | tee -a "${LOG_FILE}"
rsync -avz --include="*.sql.gz" --include="*.tar.gz" --include="*.sha256" --include="*/" --exclude="*" \
  -e "ssh -o StrictHostKeyChecking=accept-new" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_BACKUP_DIR}/" \
  "${LOCAL_BACKUP_DIR}/mysql/" | tee -a "${LOG_FILE}"

# Step 2: Verify Checksums on MacBook
echo "[2/3] Verifying SHA-256 checksums on MacBook..." | tee -a "${LOG_FILE}"
cd "${LOCAL_BACKUP_DIR}/mysql"
CHECKSUM_ERRORS=0
for sha_file in $(find . -maxdepth 2 -name "*.sha256"); do
  echo "Checking ${sha_file}..." | tee -a "${LOG_FILE}"
  if shasum -a 256 -c "${sha_file}" 2>&1 | tee -a "${LOG_FILE}"; then
    echo "OK: ${sha_file}" | tee -a "${LOG_FILE}"
  else
    echo "ERROR: Checksum mismatch for ${sha_file}" | tee -a "${LOG_FILE}"
    CHECKSUM_ERRORS=$((CHECKSUM_ERRORS + 1))
  fi
done

if [ "${CHECKSUM_ERRORS}" -gt 0 ]; then
  echo "[WARNING] ${CHECKSUM_ERRORS} checksum verification failure(s) detected!" | tee -a "${LOG_FILE}"
else
  echo "[SUCCESS] All checksums verified successfully!" | tee -a "${LOG_FILE}"
fi

# Step 3: Retention Cleanup (Delete backups older than RETENTION_DAYS)
echo "[3/3] Cleaning up MacBook backups older than ${RETENTION_DAYS} days..." | tee -a "${LOG_FILE}"
find "${LOCAL_BACKUP_DIR}/mysql" -type f -mtime +"${RETENTION_DAYS}" -delete
find "${LOCAL_BACKUP_DIR}/logs" -type f -mtime +"${RETENTION_DAYS}" -delete

echo "[MACBOOK-OFFHOST-BACKUP] Backup sync completed cleanly at $(date +'%Y-%m-%d %H:%M:%S')" | tee -a "${LOG_FILE}"
