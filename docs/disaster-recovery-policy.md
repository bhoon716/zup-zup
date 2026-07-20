# Database and durable-state recovery policy

> Current runtime policy (2026-07-20): MySQL uses the Docker named volume `sugang-helper-db-data`, and `/home/ubuntu/jbnu-sugang-helper/backup-db-local.sh` runs from an OCI systemd timer. The backup script and timer unit are installed directly on the server and are not part of the git release. The encrypted multi-state archive described below is a legacy design record and is not part of the current minimal Compose runtime.

## Scope and account separation

The production Compose stack uses three non-root MySQL accounts.

| Account | Where it runs | Privileges |
| --- | --- | --- |
| `DB_RUNTIME_USER` | application container | `SELECT`, `INSERT`, `UPDATE`, `DELETE` on the application schema |
| `DB_MIGRATOR_USER` | one-shot `migrate` service during deployment only | runtime DML plus `CREATE`, `ALTER`, `DROP`, `INDEX`, `REFERENCES` |

`DB_ROOT_PASSWORD` stays only in the MySQL service environment. It is used by the first-volume initializer and the root-only restore path; it is never included in the application container environment. The application also has Flyway disabled in production. Deployment runs the `migrate` profile before replacing the application container, so the migrator password is never present in the runtime JVM.

The host root `.env` must contain `DB_ROOT_PASSWORD`, `DB_JDBC_URL`, and the runtime/migrator credential pairs. Generate distinct high-entropy passwords. The release `apps/server/.env` must not contain any `SPRING_DATASOURCE_*`, `SPRING_FLYWAY_*`, `DB_*`, or root database values; CI rejects them.

Routine deployment only creates missing accounts and reapplies grants; it deliberately does not rotate passwords. Password rotation is a planned maintenance action: quiesce the app, use `ROTATE_DB_SERVICE_PASSWORDS=ROTATE` with the provisioner, verify the new account credentials, and deploy the matching release. This avoids a failed deployment changing credentials underneath the still-running old app.

## Backup policy

The active backup is a MySQL logical dump created by `/home/ubuntu/jbnu-sugang-helper/backup-db-local.sh`. It runs as the `ubuntu` user every Monday at 04:00 (`Asia/Seoul`) through `jbnu-sugang-helper-db-backup.timer`, does not stop the application, and uses `--single-transaction --source-data=2 --no-tablespaces --routines --events --triggers`.

Each successful dump is gzip-compressed and accompanied by a SHA-256 sidecar under `/home/ubuntu/jbnu-sugang-helper/backups/mysql`. Files are mode `0600` and the directory is mode `0700`. The current timer keeps the backup on the same OCI instance; it protects against accidental application or schema mistakes but not loss of the instance or its boot disk.

The current minimal runtime does not create a `DB_BACKUP_USER`, does not rotate binary logs for backup, and does not pause NPM/Grafana/app writers. Off-host/Object Storage retention, encrypted archive escrow, and full durable-state recovery are separate follow-up work.

The former encrypted multi-state archive was removed from the minimal runtime. Do not recreate or schedule that design alongside the current timer.

## Restore and drill

Restoring a production database is destructive and requires an explicit confirmation. The current verification path restores the latest gzip dump into an isolated temporary MySQL container before any production volume is considered.

```bash
docker run --rm \
  --env MYSQL_ROOT_PASSWORD=<temporary-test-password> \
  mysql:8.0 \
  bash -ceu 'mysql -h <temporary-db> -uroot -p"$MYSQL_ROOT_PASSWORD" < /backup/sugang_helper.sql'
```

The production named volume is never used as the restore test target. The test must verify the gzip archive, checksum, schema, and representative rows in an isolated temporary database. A production restore remains a separate, explicitly approved maintenance operation; application SHA rollback does not roll back database migrations.

A one-time server-side dry-run and a post-install `systemctl status` check verify the current script and timer contract. A full clean-host durable-state drill is not claimed by the current local-only backup and must be designed separately if off-host recovery becomes required.

Google OAuth itself requires a third-party callback/client and is deliberately not simulated as a database-only login. After a real-host restore, an operator must complete one controlled Google login before declaring the incident resolved.

## Point-in-time boundary

The supported recovery target is the last successful local logical dump only. It does not provide point-in-time recovery, host-loss recovery, or versioned uploaded-file recovery.

True PITR requires continuous off-host binary-log archiving, a tested `mysqlbinlog` replay path, and versioned/incremental attachment storage that advances with the same recovery point. That is intentionally outside this single-host snapshot policy.
