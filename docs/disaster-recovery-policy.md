# Database and durable-state recovery policy

> Current runtime policy (2026-07-20): MySQL uses the Docker named volume `sugang-helper-db-data`, and `/home/ubuntu/jbnu-sugang-helper/backup-db-local.sh` runs from an OCI systemd timer. The backup script and timer unit are installed directly on the server and are not part of the git release. The encrypted multi-state archive described below is a legacy design record and is not part of the current minimal Compose runtime.

## Database access boundary

The current minimal runtime deliberately keeps the existing MySQL `root` account. The application and the one-shot Flyway `migrate` service both use `DB_ROOT_PASSWORD`; no runtime account, migrator account, account initializer, or grant rotation is created.

| Consumer | Account | Purpose |
| --- | --- | --- |
| MySQL container | `root` | database administration and initialization |
| application container | `root` | runtime queries and writes |
| one-shot `migrate` service | `root` | schema migration during deployment |

This is an explicit 1-person-operations trade-off: it removes account bootstrap and credential synchronization, but a compromised application has schema-level privileges. The app still keeps automatic Flyway disabled; deployment runs the one-shot migration before starting the new app.

The host root `.env` contains `DB_ROOT_PASSWORD` and `DB_JDBC_URL`. The release `apps/server/.env` contains application-only settings and does not contain raw `DB_*` or `SPRING_FLYWAY_*` keys; Compose injects the database and Redis runtime values.

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
