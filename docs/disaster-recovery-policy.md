# Database and durable-state recovery policy

## Scope and account separation

The production Compose stack uses three non-root MySQL accounts.

| Account | Where it runs | Privileges |
| --- | --- | --- |
| `DB_RUNTIME_USER` | application container | `SELECT`, `INSERT`, `UPDATE`, `DELETE` on the application schema |
| `DB_MIGRATOR_USER` | one-shot `migrate` service during deployment only | runtime DML plus `CREATE`, `ALTER`, `DROP`, `INDEX`, `REFERENCES` |
| `DB_BACKUP_USER@localhost` | inside the MySQL container during backup | schema `SELECT`, `SHOW VIEW`, `TRIGGER`, `EVENT`, plus global `RELOAD` and `REPLICATION CLIENT` for dump position and binary-log rotation |

`DB_ROOT_PASSWORD` stays only in the MySQL service environment. It is used by the first-volume initializer and the root-only restore path; it is never included in the application container environment. The application also has Flyway disabled in production. Deployment runs the `migrate` profile before replacing the application container, so the migrator password is never present in the runtime JVM.

The host `infra/.env` must contain `DB_ROOT_PASSWORD`, `DB_JDBC_URL`, and the three service-account credential pairs. Generate distinct high-entropy passwords. The release `apps/server/.env` must not contain any `SPRING_DATASOURCE_*`, `SPRING_FLYWAY_*`, `DB_*`, or root database values; CI rejects them.

Routine deployment only creates missing accounts and reapplies grants; it deliberately does not rotate passwords. Password rotation is a planned maintenance action: quiesce the app, use `ROTATE_DB_SERVICE_PASSWORDS=ROTATE` with the provisioner, verify the new account credentials, and deploy the matching release. This avoids a failed deployment changing credentials underneath the still-running old app.

## Backup policy

`infra/scripts/backup-dr-state.sh` creates a coherent recovery point by briefly stopping the application, Grafana, and Nginx Proxy Manager while MySQL is dumped. It then restarts only services that had been running.

The encrypted archive contains:

- a MySQL logical dump created with `--single-transaction --source-data=2`;
- MySQL `mysql-bin.*` files present at that snapshot after a binary-log rotation;
- `/var/lib/jbnu-sugang-helper/uploads`;
- `/var/lib/jbnu-sugang-helper/grafana`;
- `infra/nginx-proxy-manager/data` and `infra/nginx-proxy-manager/letsencrypt`.

MySQL uses row-format binary logs and retains them for seven days. A full encrypted archive is retained for 14 days by default. The archive is AES-256-CBC encrypted with PBKDF2 (600,000 iterations, SHA-512), mode `0600`, and accompanied by a SHA-256 checksum plus an HMAC-SHA-256 sidecar. Restore verifies both before decryption. The checksum detects accidental corruption; the HMAC detects an archive or sidecar replacement by a party that does not hold the separate authentication key.

Keep the archive, `.sha256`, and `.hmac` sidecars on a secondary mounted disk or another operator-controlled host. `/var/backups` on the same system protects against accidental deletion, not loss of that host or its disk.

The encryption passphrase file and HMAC authentication-key file are outside the repository, root-owned, and mode `0400` or `0600`. Their default paths are `/etc/jbnu-sugang-helper/backup-passphrase` and `/etc/jbnu-sugang-helper/backup-authentication-key`. Store both as versioned entries in an access-controlled off-host secret manager or offline escrow that is separate from the archive storage and production host. The incident runbook must name the custodian, recovery approval path, key version, and retrieval test; never put either secret in the archive or its storage volume.

```bash
sudo BACKUP_ENCRYPTION_PASSWORD_FILE=/etc/jbnu-sugang-helper/backup-passphrase \
  BACKUP_AUTHENTICATION_KEY_FILE=/etc/jbnu-sugang-helper/backup-authentication-key \
  bash infra/scripts/backup-dr-state.sh
```

Schedule this command once per day during a low-traffic period. The short write pause is intentional: it makes database rows and uploaded files correspond to the same recovery point.

Deployment, backup, and restore share `infra/.operation-lock`, so they cannot overlap and accidentally restart writers in the middle of a snapshot. If the host is forcibly terminated, inspect the stopped services and the lock owner before manually removing a stale lock.

## Restore and drill

Restore requires an explicit destructive confirmation and verifies the encrypted archive checksum before decrypting or touching state.

```bash
sudo CONFIRM_DR_RESTORE=RESTORE \
  BACKUP_ENCRYPTION_PASSWORD_FILE=/etc/jbnu-sugang-helper/backup-passphrase \
  BACKUP_AUTHENTICATION_KEY_FILE=/etc/jbnu-sugang-helper/backup-authentication-key \
  bash infra/scripts/restore-dr-state.sh \
  /mnt/secondary-backup/dr-state-<timestamp>.tar.gz.enc
```

The restore script stages and validates the archive, rejects links, devices, FIFOs, and privileged tar modes before extraction, checks that its database name matches the configured DB before destructive work, keeps the previous host-state directories beside their restored paths, and rolls back the logical database/state swap if a later step fails. If rollback itself fails, writer services remain stopped rather than serving a partial restore. Archives created before the HMAC sidecar policy are intentionally rejected until they are replaced by a newly verified backup. Before starting application traffic on a clean host, retrieve the matching passphrase and authentication key from escrow, then place the matching runtime files, Firebase file, and Ubuntu-owned release staging tree on the host and run the normal Ubuntu SSH deploy script. That flow starts the runtime dependencies, applies the one-shot migration, and starts the runtime application with only the runtime account. Because MySQL DDL can be irreversible, a failure after migration requires manual recovery rather than an unsafe old-image rollback.

The automated clean-host drill is `infra/scripts/test-dr-state-recovery.sh`. It proves restoration of a durable user identity record, subscription, feedback attachment metadata and bytes, Grafana state, NPM state, certificates, checksum validation, encryption, and archived binary logs. It runs for infra changes and every Monday in `.github/workflows/dr-drill.yml`.

Google OAuth itself requires a third-party callback/client and is deliberately not simulated as a database-only login. After a real-host restore, an operator must complete one controlled Google login before declaring the incident resolved; the automated drill is evidence for the recoverable local state, not a claim that the external provider was exercised. Once per quarter, retrieve the passphrase and authentication key through the documented escrow path, restore one real encrypted production archive to an isolated host, and record the archive timestamp, checksum/HMAC result, key version, app readiness, controlled login, subscription, and attachment-read result in the incident/operations log.

## Point-in-time boundary

The supported recovery target is the consistent full-backup point only. The snapshot includes binary logs and the dump source position for inspection, but it does not continuously export post-snapshot binary logs or version uploaded files. Therefore it must not be described as point-in-time recovery after a host loss: replaying only DB changes could also leave `feedback_attachments.file_url` pointing at files that were created after the snapshot.

True PITR requires continuous off-host binary-log archiving, a tested `mysqlbinlog` replay path, and versioned/incremental attachment storage that advances with the same recovery point. That is intentionally outside this single-host snapshot policy.
