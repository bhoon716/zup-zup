#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
backup_script="${repo_root}/infra/scripts/backup-dr-state.sh"
restore_script="${repo_root}/infra/scripts/restore-dr-state.sh"
provision_script="${repo_root}/infra/mysql/init/01-provision-service-accounts.sh"
docker_bin="${DOCKER_BIN:-docker}"
workdir="$(mktemp -d)"
compose_dir="${workdir}/compose"
compose_file="${compose_dir}/docker-compose.yml"
project_name="issue100-dr-recovery-${RANDOM}-$$"
state_root="${workdir}/state"
uploads_dir="${state_root}/uploads"
grafana_dir="${state_root}/grafana"
npm_data_dir="${state_root}/nginx-proxy-manager/data"
npm_letsencrypt_dir="${state_root}/nginx-proxy-manager/letsencrypt"
backup_root="${workdir}/backups"
encryption_password_file="${workdir}/backup-passphrase"
authentication_key_file="${workdir}/backup-authentication-key"

export COMPOSE_PROJECT_NAME="${project_name}"
export MYSQL_ROOT_PASSWORD="root-password-for-issue100-dr-test"
export MYSQL_DATABASE="sugang_helper"
export DB_RUNTIME_USER="sugang_runtime"
export DB_RUNTIME_PASSWORD="runtime-password-for-dr-test"
export DB_MIGRATOR_USER="sugang_migrator"
export DB_MIGRATOR_PASSWORD="migrator-password-for-dr-test"
export DB_BACKUP_USER="sugang_backup"
export DB_BACKUP_PASSWORD="backup-password-for-dr-test"

compose() {
  "${docker_bin}" compose -p "${project_name}" -f "${compose_file}" "$@"
}

cleanup() {
  status=$?
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  rm -rf "${workdir}"
  exit "${status}"
}

wait_for_db_health() {
  local container_id
  container_id="$(compose ps -q db)"
  if [ -z "${container_id}" ]; then
    echo "MySQL container was not created" >&2
    return 1
  fi

  for _ in $(seq 1 60); do
    local status
    status="$("${docker_bin}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    if [ "${status}" = "healthy" ] \
      && mysql_as MYSQL_ROOT_PASSWORD root "SELECT 1" >/dev/null 2>&1; then
      return 0
    fi
    if [ "${status}" = "exited" ] || [ "${status}" = "dead" ]; then
      "${docker_bin}" logs "${container_id}" >&2 || true
      return 1
    fi
    sleep 1
  done

  echo "MySQL did not become healthy" >&2
  return 1
}

mysql_as() {
  local password_variable="$1"
  local username="$2"
  local sql="$3"
  compose exec -T db sh -ceu '
    export MYSQL_PWD="$(printenv "$1")"
    mysql --protocol=socket --user="$2" "$MYSQL_DATABASE" -e "$3"
  ' sh "${password_variable}" "${username}" "${sql}"
}

write_archive_integrity_files() {
  local target_archive="$1"
  printf '%s  %s\n' "$(sha256sum "${target_archive}" | awk '{print $1}')" "$(basename "${target_archive}")" > "${target_archive}.sha256"
  printf '%s  %s\n' "$(openssl dgst -sha256 -mac HMAC -macopt "key:file:${authentication_key_file}" -r "${target_archive}" | awk 'NR == 1 {print $1}')" "$(basename "${target_archive}")" > "${target_archive}.hmac"
}

trap cleanup EXIT

for script in "${provision_script}" "${backup_script}" "${restore_script}"; do
  if [ ! -x "${script}" ]; then
    echo "required DR script is missing or not executable: ${script}" >&2
    exit 1
  fi
done

mkdir -p "${compose_dir}" "${uploads_dir}" "${grafana_dir}" "${npm_data_dir}" "${npm_letsencrypt_dir}"
printf '%s' 'issue100-dr-encryption-passphrase' > "${encryption_password_file}"
printf '%s' 'issue100-dr-authentication-key' > "${authentication_key_file}"
chmod 600 "${encryption_password_file}"
chmod 600 "${authentication_key_file}"

cat > "${compose_file}" <<EOF
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      DB_RUNTIME_USER: ${DB_RUNTIME_USER}
      DB_RUNTIME_PASSWORD: ${DB_RUNTIME_PASSWORD}
      DB_MIGRATOR_USER: ${DB_MIGRATOR_USER}
      DB_MIGRATOR_PASSWORD: ${DB_MIGRATOR_PASSWORD}
      DB_BACKUP_USER: ${DB_BACKUP_USER}
      DB_BACKUP_PASSWORD: ${DB_BACKUP_PASSWORD}
    command:
      - --server-id=1
      - --log-bin=mysql-bin
      - --binlog-format=ROW
      - --binlog-expire-logs-seconds=604800
    volumes:
      - ${provision_script}:/docker-entrypoint-initdb.d/01-provision-service-accounts.sh:ro
    healthcheck:
      test: ["CMD-SHELL", "MYSQL_PWD=\"\$\$MYSQL_ROOT_PASSWORD\" mysql --protocol=socket -h localhost -uroot -e 'SELECT 1' >/dev/null"]
      interval: 1s
      timeout: 1s
      retries: 60
      start_period: 1s
EOF

compose up -d db
wait_for_db_health

mysql_as MYSQL_ROOT_PASSWORD root '
  CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE);
  CREATE TABLE subscriptions (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, course_key VARCHAR(64) NOT NULL);
  CREATE TABLE feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL
  );
  CREATE TABLE feedback_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_attachment_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id) ON DELETE CASCADE
  );
'
mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "
  INSERT INTO users (email) VALUES ('dr-user@example.com');
  INSERT INTO subscriptions (user_id, course_key) VALUES (1, 'CS-DR-001');
  INSERT INTO feedbacks (user_id, type, title, content) VALUES (1, 'BUG', 'DR attachment', 'attachment recovery fixture');
  INSERT INTO feedback_attachments (feedback_id, file_url, original_name)
    VALUES (1, '/uploads/dr-attachment.png', 'dr-attachment.png');
"

printf '%s' 'feedback attachment bytes' > "${uploads_dir}/dr-attachment.png"
printf '%s' 'grafana state sentinel' > "${grafana_dir}/grafana.db"
printf '%s' 'npm data sentinel' > "${npm_data_dir}/database.sqlite"
printf '%s' 'npm certificate sentinel' > "${npm_letsencrypt_dir}/cert.pem"

ALLOW_NON_ROOT=1 \
DR_TEST_STATE_ROOT="${state_root}" \
COMPOSE_DIR="${compose_dir}" \
DOCKER_BIN="${docker_bin}" \
SOURCE_UPLOADS_DIR="${uploads_dir}" \
SOURCE_GRAFANA_DIR="${grafana_dir}" \
SOURCE_NPM_DATA_DIR="${npm_data_dir}" \
SOURCE_NPM_LETSENCRYPT_DIR="${npm_letsencrypt_dir}" \
BACKUP_ROOT="${backup_root}" \
BACKUP_ENCRYPTION_PASSWORD_FILE="${encryption_password_file}" \
BACKUP_AUTHENTICATION_KEY_FILE="${authentication_key_file}" \
DR_BACKUP_RETENTION_DAYS=14 \
bash "${backup_script}"

archive="$(find "${backup_root}" -maxdepth 1 -name 'dr-state-*.tar.gz.enc' -type f | head -1)"
if [ -z "${archive}" ] || [ ! -f "${archive}.sha256" ] || [ ! -f "${archive}.hmac" ]; then
  echo "DR encrypted archive or checksum was not created" >&2
  exit 1
fi
if ! openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 \
  -pass "file:${encryption_password_file}" -in "${archive}" | tar -tzf - | grep -Eq '^database/binlogs/mysql-bin\.'; then
  echo "DR archive did not include MySQL binary logs" >&2
  exit 1
fi

tampered_archive="${backup_root}/dr-state-tampered.tar.gz.enc"
cp "${archive}" "${tampered_archive}"
cp "${archive}.sha256" "${tampered_archive}.sha256"
cp "${archive}.hmac" "${tampered_archive}.hmac"
printf '%s' x >> "${tampered_archive}"
set +e
ALLOW_NON_ROOT=1 \
DR_TEST_STATE_ROOT="${state_root}" \
COMPOSE_DIR="${compose_dir}" \
DOCKER_BIN="${docker_bin}" \
SOURCE_UPLOADS_DIR="${uploads_dir}" \
SOURCE_GRAFANA_DIR="${grafana_dir}" \
SOURCE_NPM_DATA_DIR="${npm_data_dir}" \
SOURCE_NPM_LETSENCRYPT_DIR="${npm_letsencrypt_dir}" \
BACKUP_ENCRYPTION_PASSWORD_FILE="${encryption_password_file}" \
BACKUP_AUTHENTICATION_KEY_FILE="${authentication_key_file}" \
CONFIRM_DR_RESTORE=RESTORE \
bash "${restore_script}" "${tampered_archive}" > "${workdir}/tampered-restore.out" 2>&1
tampered_status=$?
set -e
if [ "${tampered_status}" -eq 0 ] \
  || ! grep -q 'checksum verification failed' "${workdir}/tampered-restore.out"; then
  echo "restore accepted a checksum-tampered archive" >&2
  exit 1
fi

invalid_hmac_archive="${backup_root}/dr-state-invalid-hmac.tar.gz.enc"
cp "${archive}" "${invalid_hmac_archive}"
cp "${archive}.sha256" "${invalid_hmac_archive}.sha256"
printf '%064d  %s\n' 0 "$(basename "${invalid_hmac_archive}")" > "${invalid_hmac_archive}.hmac"
set +e
ALLOW_NON_ROOT=1 \
DR_TEST_STATE_ROOT="${state_root}" \
COMPOSE_DIR="${compose_dir}" \
DOCKER_BIN="${docker_bin}" \
SOURCE_UPLOADS_DIR="${uploads_dir}" \
SOURCE_GRAFANA_DIR="${grafana_dir}" \
SOURCE_NPM_DATA_DIR="${npm_data_dir}" \
SOURCE_NPM_LETSENCRYPT_DIR="${npm_letsencrypt_dir}" \
BACKUP_ENCRYPTION_PASSWORD_FILE="${encryption_password_file}" \
BACKUP_AUTHENTICATION_KEY_FILE="${authentication_key_file}" \
CONFIRM_DR_RESTORE=RESTORE \
bash "${restore_script}" "${invalid_hmac_archive}" > "${workdir}/invalid-hmac-restore.out" 2>&1
invalid_hmac_status=$?
set -e
if [ "${invalid_hmac_status}" -eq 0 ] \
  || ! grep -q 'HMAC verification failed' "${workdir}/invalid-hmac-restore.out"; then
  echo "restore accepted an archive with an invalid HMAC" >&2
  exit 1
fi

special_dir="${workdir}/special-entry"
special_archive="${backup_root}/dr-state-special-entry.tar.gz.enc"
mkdir -p "${special_dir}/extracted"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 \
  -pass "file:${encryption_password_file}" -in "${archive}" -out "${special_dir}/archive.tar.gz"
tar -xzf "${special_dir}/archive.tar.gz" -C "${special_dir}/extracted"
find "${special_dir}/extracted" -name '._*' -type f -delete
ln -s /etc/passwd "${special_dir}/extracted/uploads/untrusted-link"
COPYFILE_DISABLE=1 tar -C "${special_dir}/extracted" -czf "${special_dir}/archive.tar.gz" \
  manifest.env database uploads grafana nginx-proxy-manager
openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 -salt \
  -pass "file:${encryption_password_file}" -in "${special_dir}/archive.tar.gz" -out "${special_archive}"
write_archive_integrity_files "${special_archive}"
set +e
ALLOW_NON_ROOT=1 \
DR_TEST_STATE_ROOT="${state_root}" \
COMPOSE_DIR="${compose_dir}" \
DOCKER_BIN="${docker_bin}" \
SOURCE_UPLOADS_DIR="${uploads_dir}" \
SOURCE_GRAFANA_DIR="${grafana_dir}" \
SOURCE_NPM_DATA_DIR="${npm_data_dir}" \
SOURCE_NPM_LETSENCRYPT_DIR="${npm_letsencrypt_dir}" \
BACKUP_ENCRYPTION_PASSWORD_FILE="${encryption_password_file}" \
BACKUP_AUTHENTICATION_KEY_FILE="${authentication_key_file}" \
CONFIRM_DR_RESTORE=RESTORE \
bash "${restore_script}" "${special_archive}" > "${special_dir}/restore.out" 2>&1
special_status=$?
set -e
if [ "${special_status}" -eq 0 ] \
  || ! grep -q 'unsupported tar entry type' "${special_dir}/restore.out"; then
  echo "restore accepted an archive with a symbolic link" >&2
  exit 1
fi

mismatch_dir="${workdir}/mismatch"
mismatch_archive="${backup_root}/dr-state-mismatch.tar.gz.enc"
mkdir -p "${mismatch_dir}/extracted"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 \
  -pass "file:${encryption_password_file}" -in "${archive}" -out "${mismatch_dir}/archive.tar.gz"
tar -xzf "${mismatch_dir}/archive.tar.gz" -C "${mismatch_dir}/extracted"
perl -0pi -e 's/^DATABASE=.*/DATABASE=other_database/m' "${mismatch_dir}/extracted/manifest.env"
find "${mismatch_dir}/extracted" -name '._*' -type f -delete
COPYFILE_DISABLE=1 tar -C "${mismatch_dir}/extracted" -czf "${mismatch_dir}/archive.tar.gz" \
  manifest.env database uploads grafana nginx-proxy-manager
openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -md sha512 -salt \
  -pass "file:${encryption_password_file}" -in "${mismatch_dir}/archive.tar.gz" -out "${mismatch_archive}"
write_archive_integrity_files "${mismatch_archive}"
set +e
ALLOW_NON_ROOT=1 \
DR_TEST_STATE_ROOT="${state_root}" \
COMPOSE_DIR="${compose_dir}" \
DOCKER_BIN="${docker_bin}" \
SOURCE_UPLOADS_DIR="${uploads_dir}" \
SOURCE_GRAFANA_DIR="${grafana_dir}" \
SOURCE_NPM_DATA_DIR="${npm_data_dir}" \
SOURCE_NPM_LETSENCRYPT_DIR="${npm_letsencrypt_dir}" \
BACKUP_ENCRYPTION_PASSWORD_FILE="${encryption_password_file}" \
BACKUP_AUTHENTICATION_KEY_FILE="${authentication_key_file}" \
CONFIRM_DR_RESTORE=RESTORE \
bash "${restore_script}" "${mismatch_archive}" > "${mismatch_dir}/restore.out" 2>&1
mismatch_status=$?
set -e
if [ "${mismatch_status}" -eq 0 ] \
  || ! grep -q 'does not match configured database' "${mismatch_dir}/restore.out"; then
  echo "restore accepted an archive for a different database" >&2
  exit 1
fi

compose down --volumes --remove-orphans >/dev/null
rm -rf "${uploads_dir}" "${grafana_dir}" "${state_root}/nginx-proxy-manager"
mkdir -p "${state_root}"
compose up -d db
wait_for_db_health

ALLOW_NON_ROOT=1 \
DR_TEST_STATE_ROOT="${state_root}" \
COMPOSE_DIR="${compose_dir}" \
DOCKER_BIN="${docker_bin}" \
SOURCE_UPLOADS_DIR="${uploads_dir}" \
SOURCE_GRAFANA_DIR="${grafana_dir}" \
SOURCE_NPM_DATA_DIR="${npm_data_dir}" \
SOURCE_NPM_LETSENCRYPT_DIR="${npm_letsencrypt_dir}" \
BACKUP_ENCRYPTION_PASSWORD_FILE="${encryption_password_file}" \
BACKUP_AUTHENTICATION_KEY_FILE="${authentication_key_file}" \
CONFIRM_DR_RESTORE=RESTORE \
bash "${restore_script}" "${archive}"

if [ "$(mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "SELECT email FROM users WHERE id = 1" | tail -1)" != "dr-user@example.com" ]; then
  echo "DR restore did not recover the login identity record" >&2
  exit 1
fi
if [ "$(mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "SELECT course_key FROM subscriptions WHERE user_id = 1" | tail -1)" != "CS-DR-001" ]; then
  echo "DR restore did not recover the subscription" >&2
  exit 1
fi
if [ "$(mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "SELECT file_url FROM feedback_attachments WHERE feedback_id = 1" | tail -1)" != "/uploads/dr-attachment.png" ]; then
  echo "DR restore did not recover the feedback attachment metadata" >&2
  exit 1
fi
if [ "$(cat "${uploads_dir}/dr-attachment.png")" != "feedback attachment bytes" ] \
  || [ "$(cat "${grafana_dir}/grafana.db")" != "grafana state sentinel" ] \
  || [ "$(cat "${npm_data_dir}/database.sqlite")" != "npm data sentinel" ] \
  || [ "$(cat "${npm_letsencrypt_dir}/cert.pem")" != "npm certificate sentinel" ]; then
  echo "DR restore did not recover host state" >&2
  exit 1
fi
if [ "$("${docker_bin}" run --rm --entrypoint cat --user 10001:10001 \
  -v "${uploads_dir}:/uploads:ro" redis:6-alpine /uploads/dr-attachment.png)" != "feedback attachment bytes" ]; then
  echo "restored attachment is not readable by the runtime app UID" >&2
  exit 1
fi

echo "DR state recovery verification passed"
