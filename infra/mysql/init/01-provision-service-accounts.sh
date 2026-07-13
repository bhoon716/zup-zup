#!/usr/bin/env bash
set -euo pipefail

require_variable() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "${name} must be set" >&2
    exit 1
  fi
}

validate_identifier() {
  local name="$1"
  local value="$2"
  if [[ ! "${value}" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "${name} must contain only letters, digits, or underscores" >&2
    exit 1
  fi
}

password_hex() {
  printf '%s' "$1" | od -An -tx1 | tr -d ' \n'
}

run_mysql() {
  MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
    --protocol=socket \
    --user=root \
    --database="${MYSQL_DATABASE}" \
    --batch \
    --skip-column-names
}

for variable in \
  MYSQL_ROOT_PASSWORD \
  MYSQL_DATABASE \
  DB_RUNTIME_USER \
  DB_RUNTIME_PASSWORD \
  DB_MIGRATOR_USER \
  DB_MIGRATOR_PASSWORD \
  DB_BACKUP_USER \
  DB_BACKUP_PASSWORD; do
  require_variable "${variable}"
done

validate_identifier "MYSQL_DATABASE" "${MYSQL_DATABASE}"
for account_variable in DB_RUNTIME_USER DB_MIGRATOR_USER DB_BACKUP_USER; do
  validate_identifier "${account_variable}" "${!account_variable}"
done

if [ "${DB_RUNTIME_USER}" = "root" ] \
  || [ "${DB_MIGRATOR_USER}" = "root" ] \
  || [ "${DB_BACKUP_USER}" = "root" ]; then
  echo "service accounts must not use the root username" >&2
  exit 1
fi

if [ "${DB_RUNTIME_USER}" = "${DB_MIGRATOR_USER}" ] \
  || [ "${DB_RUNTIME_USER}" = "${DB_BACKUP_USER}" ] \
  || [ "${DB_MIGRATOR_USER}" = "${DB_BACKUP_USER}" ]; then
  echo "runtime, migrator, and backup usernames must be distinct" >&2
  exit 1
fi

runtime_password_hex="$(password_hex "${DB_RUNTIME_PASSWORD}")"
migrator_password_hex="$(password_hex "${DB_MIGRATOR_PASSWORD}")"
backup_password_hex="$(password_hex "${DB_BACKUP_PASSWORD}")"
rotate_passwords="${ROTATE_DB_SERVICE_PASSWORDS:-}"

if [ -z "${runtime_password_hex}" ] \
  || [ -z "${migrator_password_hex}" ] \
  || [ -z "${backup_password_hex}" ]; then
  echo "service-account passwords must not be empty" >&2
  exit 1
fi

if [ -n "${rotate_passwords}" ] && [ "${rotate_passwords}" != "ROTATE" ]; then
  echo "set ROTATE_DB_SERVICE_PASSWORDS=ROTATE to rotate service-account passwords" >&2
  exit 1
fi

# Password values are encoded as hex and quoted by MySQL itself. This keeps apostrophes
# and backslashes out of the generated SQL while still allowing password-manager output.
run_mysql <<SQL
SET @runtime_password = CONVERT(0x${runtime_password_hex} USING utf8mb4);
SET @migrator_password = CONVERT(0x${migrator_password_hex} USING utf8mb4);
SET @backup_password = CONVERT(0x${backup_password_hex} USING utf8mb4);

SET @statement = CONCAT('CREATE USER IF NOT EXISTS ''${DB_RUNTIME_USER}''@''%'' IDENTIFIED BY ', QUOTE(@runtime_password));
PREPARE service_account_statement FROM @statement;
EXECUTE service_account_statement;
DEALLOCATE PREPARE service_account_statement;
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${DB_RUNTIME_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.* TO '${DB_RUNTIME_USER}'@'%';

SET @statement = CONCAT('CREATE USER IF NOT EXISTS ''${DB_MIGRATOR_USER}''@''%'' IDENTIFIED BY ', QUOTE(@migrator_password));
PREPARE service_account_statement FROM @statement;
EXECUTE service_account_statement;
DEALLOCATE PREPARE service_account_statement;
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${DB_MIGRATOR_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES ON \`${MYSQL_DATABASE}\`.* TO '${DB_MIGRATOR_USER}'@'%';

SET @statement = CONCAT('CREATE USER IF NOT EXISTS ''${DB_BACKUP_USER}''@''localhost'' IDENTIFIED BY ', QUOTE(@backup_password));
PREPARE service_account_statement FROM @statement;
EXECUTE service_account_statement;
DEALLOCATE PREPARE service_account_statement;
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${DB_BACKUP_USER}'@'localhost';
GRANT SELECT, SHOW VIEW, TRIGGER, EVENT ON \`${MYSQL_DATABASE}\`.* TO '${DB_BACKUP_USER}'@'localhost';
GRANT RELOAD, REPLICATION CLIENT ON *.* TO '${DB_BACKUP_USER}'@'localhost';
SQL

if [ "${rotate_passwords}" = "ROTATE" ]; then
  run_mysql <<SQL
SET @runtime_password = CONVERT(0x${runtime_password_hex} USING utf8mb4);
SET @migrator_password = CONVERT(0x${migrator_password_hex} USING utf8mb4);
SET @backup_password = CONVERT(0x${backup_password_hex} USING utf8mb4);

SET @statement = CONCAT('ALTER USER ''${DB_RUNTIME_USER}''@''%'' IDENTIFIED BY ', QUOTE(@runtime_password));
PREPARE service_account_statement FROM @statement;
EXECUTE service_account_statement;
DEALLOCATE PREPARE service_account_statement;
SET @statement = CONCAT('ALTER USER ''${DB_MIGRATOR_USER}''@''%'' IDENTIFIED BY ', QUOTE(@migrator_password));
PREPARE service_account_statement FROM @statement;
EXECUTE service_account_statement;
DEALLOCATE PREPARE service_account_statement;
SET @statement = CONCAT('ALTER USER ''${DB_BACKUP_USER}''@''localhost'' IDENTIFIED BY ', QUOTE(@backup_password));
PREPARE service_account_statement FROM @statement;
EXECUTE service_account_statement;
DEALLOCATE PREPARE service_account_statement;
SQL
fi
