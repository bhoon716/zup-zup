#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
provision_script="${repo_root}/infra/mysql/init/01-provision-service-accounts.sh"
docker_bin="${DOCKER_BIN:-docker}"
workdir="$(mktemp -d)"
compose_dir="${workdir}/compose"
compose_file="${compose_dir}/docker-compose.yml"
project_name="issue100-db-accounts-${RANDOM}-$$"

export COMPOSE_PROJECT_NAME="${project_name}"
export MYSQL_ROOT_PASSWORD="root-password-for-issue100-test"
export MYSQL_DATABASE="sugang_helper"
export DB_RUNTIME_USER="sugang_runtime"
export DB_RUNTIME_PASSWORD="runtime password with 'quote and \\ slash"
export DB_MIGRATOR_USER="sugang_migrator"
export DB_MIGRATOR_PASSWORD="migrator password with 'quote and \\ slash"
export DB_BACKUP_USER="sugang_backup"
export DB_BACKUP_PASSWORD="backup password with 'quote and \\ slash"

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
      && mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "SELECT 1" >/dev/null 2>&1; then
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

expect_failure() {
  local description="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "${description} unexpectedly succeeded" >&2
    return 1
  fi
}

trap cleanup EXIT

if [ ! -x "${provision_script}" ]; then
  echo "DB service-account provisioner is missing or not executable: ${provision_script}" >&2
  exit 1
fi

mkdir -p "${compose_dir}"
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
  migrate:
    image: flyway/flyway@sha256:8ace7d9825bb3ad1d6e14ee27b3a830b638ac841ba424b99b2d92aa65a99d484
    environment:
      FLYWAY_URL: jdbc:mysql://db:3306/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      FLYWAY_USER: ${DB_MIGRATOR_USER}
      FLYWAY_PASSWORD: ${DB_MIGRATOR_PASSWORD}
      FLYWAY_BASELINE_ON_MIGRATE: "true"
      FLYWAY_BASELINE_VERSION: "3"
      FLYWAY_CLEAN_DISABLED: "true"
      FLYWAY_CONNECT_RETRIES: "60"
    command: migrate
    volumes:
      - ${repo_root}/apps/server/src/main/resources/db/migration:/flyway/sql:ro
EOF

compose up -d db
wait_for_db_health
compose run --rm --no-deps migrate

mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" \
  "INSERT INTO announcements (title, content, pinned, published) VALUES ('least privilege', 'runtime CRUD', 0, 1); DELETE FROM announcements WHERE title = 'least privilege';"
expect_failure "runtime account CREATE TABLE" \
  mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "CREATE TABLE runtime_forbidden (id BIGINT)"
expect_failure "runtime account ALTER TABLE" \
  mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "ALTER TABLE announcements ADD COLUMN runtime_forbidden INT"
expect_failure "runtime account DROP TABLE" \
  mysql_as DB_RUNTIME_PASSWORD "${DB_RUNTIME_USER}" "DROP TABLE announcements"

mysql_as DB_MIGRATOR_PASSWORD "${DB_MIGRATOR_USER}" "CREATE TABLE migration_probe (id BIGINT); DROP TABLE migration_probe"
expect_failure "migrator account CREATE USER" \
  mysql_as DB_MIGRATOR_PASSWORD "${DB_MIGRATOR_USER}" "CREATE USER 'forbidden'@'%' IDENTIFIED BY 'forbidden'"

mysql_as MYSQL_ROOT_PASSWORD root "
  CREATE PROCEDURE backup_routine_probe() SELECT 1;
  CREATE EVENT backup_event_probe ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 1 DAY DO SELECT 1;
  CREATE TRIGGER backup_trigger_probe BEFORE INSERT ON announcements FOR EACH ROW SET NEW.pinned = NEW.pinned;
"

backup_dump="${workdir}/backup.sql"
compose exec -T db sh -ceu '
  export MYSQL_PWD="$DB_BACKUP_PASSWORD"
  mysqldump --protocol=socket --user="$DB_BACKUP_USER" \
    --single-transaction --source-data=2 --no-tablespaces --routines --events --triggers "$MYSQL_DATABASE"
' > "${backup_dump}"
if ! grep -Eq "backup_routine_probe|backup_event_probe|backup_trigger_probe" "${backup_dump}"; then
  echo "backup account dump did not include routines, events, and triggers" >&2
  exit 1
fi
compose exec -T db sh -ceu '
  export MYSQL_PWD="$DB_BACKUP_PASSWORD"
  mysql --protocol=socket --user="$DB_BACKUP_USER" -e "SHOW BINARY LOGS; FLUSH BINARY LOGS" >/dev/null
'

echo "DB service-account verification passed"
