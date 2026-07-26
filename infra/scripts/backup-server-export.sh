#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# OCI Server Backup Export Script
# Mysqldump + Uploads archiving + SHA-256 sidecar generation
# ==============================================================================

RELEASE_ROOT="${RELEASE_ROOT:-/home/ubuntu/jbnu-sugang-helper}"
BACKUP_DIR="${RELEASE_ROOT}/backups/export"
TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"

mkdir -p "${BACKUP_DIR}"
chmod 0700 "${BACKUP_DIR}"

DB_FILE="${BACKUP_DIR}/sugang_helper_${TIMESTAMP}.sql.gz"
UPLOADS_FILE="${BACKUP_DIR}/uploads_${TIMESTAMP}.tar.gz"

echo "[BACKUP-EXPORT] Starting DB mysqldump export: ${DB_FILE}"
if docker ps --format '{{.Names}}' | grep -q "sugang-helper-mysql"; then
  docker exec sugang-helper-mysql mysqldump \
    --single-transaction \
    --source-data=2 \
    --no-tablespaces \
    --routines \
    --events \
    --triggers \
    -u root -p"${DB_ROOT_PASSWORD:-strongpassword123}" \
    sugang_helper | gzip -9 > "${DB_FILE}"
  chmod 0600 "${DB_FILE}"
  sha256sum "${DB_FILE}" > "${DB_FILE}.sha256"
  echo "[BACKUP-EXPORT] DB export complete. Checksum generated."
else
  echo "[BACKUP-EXPORT] WARNING: sugang-helper-mysql container is not running. Skipping DB export."
fi

# Archive uploads volume if present
UPLOADS_VOL_PATH="/var/lib/docker/volumes/sugang-helper-app-uploads/_data"
if [ -d "${UPLOADS_VOL_PATH}" ]; then
  echo "[BACKUP-EXPORT] Archiving uploads directory..."
  tar -czf "${UPLOADS_FILE}" -C "${UPLOADS_VOL_PATH}" .
  chmod 0600 "${UPLOADS_FILE}"
  sha256sum "${UPLOADS_FILE}" > "${UPLOADS_FILE}.sha256"
  echo "[BACKUP-EXPORT] Uploads archive complete."
fi

echo "[BACKUP-EXPORT] Export finished successfully at ${TIMESTAMP}"
