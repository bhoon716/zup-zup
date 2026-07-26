# Database and durable-state recovery policy

> Current runtime policy (2026-07-26): MySQL uses the Docker named volume `sugang-helper-db-data`, and `/home/ubuntu/jbnu-sugang-helper/backup-db-local.sh` runs from an OCI systemd timer. In addition, an Off-host backup strategy syncing encrypted DB dumps and user uploads to the operator's Local MacBook (`infra/scripts/backup-to-macbook.sh`) is established for complete host-loss protection (ISSUE-136).

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

### 1. Same-Host Local Backup (OCI Instance)
The active same-host backup is a MySQL logical dump created by `/home/ubuntu/jbnu-sugang-helper/backup-db-local.sh`. It runs as the `ubuntu` user every Monday at 04:00 (`Asia/Seoul`) through `jbnu-sugang-helper-db-backup.timer`, does not stop the application, and uses `--single-transaction --source-data=2 --no-tablespaces --routines --events --triggers`.

Each successful dump is gzip-compressed and accompanied by a SHA-256 sidecar under `/home/ubuntu/jbnu-sugang-helper/backups/mysql`. Files are mode `0600` and the directory is mode `0700`.

### 2. MacBook Off-Host Backup (Host-Loss Disaster Recovery)
To protect against complete OCI host or boot disk loss, off-host backups are periodically pulled to the operator's Local MacBook:
- **Server Export Script**: `infra/scripts/backup-server-export.sh` (exports DB dump + uploads archive + SHA-256 sidecar)
- **MacBook Sync Script**: `infra/scripts/backup-to-macbook.sh` (pulls backups via rsync/SSH, verifies SHA-256 checksums, enforces 30-day retention)
- **Target RPO**: 24 hours (daily/weekly sync)
- **Target RTO**: 30 minutes (clean restore on MacBook or fresh target host)

## Restore and drill

### MacBook Clean Restore Drill
Restoring a production database is destructive and requires an explicit confirmation. To verify backup integrity without affecting production, run the clean target restore drill on the operator's MacBook:

```bash
# Run isolated restore drill in a temporary local Docker MySQL container
infra/scripts/verify-mac-restore.sh [path/to/backup.sql.gz]
```

The drill:
1. Launches an isolated temporary `mysql:8.4` container.
2. Restores the specified gzip dump.
3. Asserts schema existence and counts representative records (`users`, `courses`).
4. Automatically cleans up the temporary test container upon completion.

A production restore remains a separate, explicitly approved maintenance operation; application SHA rollback does not roll back database migrations.

## Point-in-time boundary

The supported recovery target is the last successful local or MacBook off-host logical dump. PITR requiring continuous binary-log archiving is intentionally outside this single-host snapshot policy.
