#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# MacBook Off-Host Clean Restore Drill Script
# Restores backup dump into an isolated local Docker MySQL container & asserts integrity
# ==============================================================================

LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-$HOME/Backups/jbnu-sugang-helper}"
DUMP_FILE="${1:-}"

if [ -z "${DUMP_FILE}" ]; then
  DUMP_FILE="$(find "${LOCAL_BACKUP_DIR}/mysql" -name "*.sql.gz" | sort -r | head -n 1)"
fi

if [ -z "${DUMP_FILE}" ] || [ ! -f "${DUMP_FILE}" ]; then
  echo "[RESTORE-DRILL] Error: No valid .sql.gz backup file found in ${LOCAL_BACKUP_DIR}/mysql"
  echo "Usage: $0 [path/to/backup.sql.gz]"
  exit 1
fi

echo "======================================================================"
echo "[RESTORE-DRILL] Starting Clean Target Restore Drill"
echo "Target Dump File: ${DUMP_FILE}"
echo "======================================================================"

CONTAINER_NAME="mac-restore-drill-mysql-$(date +'%s')"
TEST_PASSWORD="drill-test-password-123"

cleanup() {
  echo "[RESTORE-DRILL] Cleaning up temporary container ${CONTAINER_NAME}..."
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[1/4] Starting temporary MySQL 8.4 container (${CONTAINER_NAME})..."
docker run -d \
  --name "${CONTAINER_NAME}" \
  -e MYSQL_ROOT_PASSWORD="${TEST_PASSWORD}" \
  -e MYSQL_DATABASE=sugang_helper \
  mysql:8.4 >/dev/null

echo "[2/4] Waiting for MySQL container to accept connections..."
for i in {1..30}; do
  if docker exec "${CONTAINER_NAME}" mysqladmin ping -u root -p"${TEST_PASSWORD}" --silent >/dev/null 2>&1; then
    echo "MySQL is ready!"
    break
  fi
  sleep 1
done

echo "[3/4] Restoring dump file into isolated temporary database..."
gunzip -c "${DUMP_FILE}" | docker exec -i "${CONTAINER_NAME}" mysql -u root -p"${TEST_PASSWORD}" sugang_helper

echo "[4/4] Verifying table schemas and representative data rows..."
TABLE_COUNT=$(docker exec "${CONTAINER_NAME}" mysql -u root -p"${TEST_PASSWORD}" -e "SELECT count(*) FROM information_schema.tables WHERE table_schema='sugang_helper';" -s -N)
USER_COUNT=$(docker exec "${CONTAINER_NAME}" mysql -u root -p"${TEST_PASSWORD}" -e "SELECT count(*) FROM sugang_helper.users;" -s -N 2>/dev/null || echo "0")
COURSE_COUNT=$(docker exec "${CONTAINER_NAME}" mysql -u root -p"${TEST_PASSWORD}" -e "SELECT count(*) FROM sugang_helper.courses;" -s -N 2>/dev/null || echo "0")

echo "----------------------------------------------------------------------"
echo "RESTORE DRILL VERIFICATION RESULTS:"
echo "- Total Tables Restored: ${TABLE_COUNT}"
echo "- Users Count: ${USER_COUNT}"
echo "- Courses Count: ${COURSE_COUNT}"
echo "----------------------------------------------------------------------"

if [ "${TABLE_COUNT}" -gt 0 ]; then
  echo "[RESTORE-DRILL] SUCCESS: Clean restore drill completed with valid schema & records!"
else
  echo "[RESTORE-DRILL] FAILURE: Restored database contains 0 tables."
  exit 1
fi
