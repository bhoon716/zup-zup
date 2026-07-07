# Risk Register

| Risk | Severity | Likelihood | Impact | Owner Area | Mitigation |
| --- | --- | --- | --- | --- | --- |
| Credential compromise from literal production secrets | Critical | High | Critical | Security/Ops | Rotate keys, externalize secrets |
| Fresh DB deploy fails or schema drifts unnoticed | Critical | Medium | High | Backend/DB | Flyway integration tests |
| Review data loss from destructive migration | High | Medium | High | Backend/DB | Backup/restore/backfill plan |
| Internal services exposed publicly | High | Medium | High | Infra | Remove/bind ports, firewall/VPN |
| Duplicate notifications from race conditions | High | Medium | Medium | Backend | Atomic dedupe/outbox |
| Premature notifications before transaction commit | High | Medium | Medium | Backend | After-commit listener/outbox |
| Orphaned user data after account deletion | High | Medium | High | Backend/DB | FK/cascade or explicit cleanup/anonymization |
| CD ships untested code | High | Medium | High | DevOps | Required CI gates |
| API contract drift | Medium | High | Medium | Backend/Web | Standard response/error policy |
| Search/crawl performance degrades at peak | Medium | Medium | Medium | Backend | Indexing and batch writes |
| Docs/repo topology drift | Medium | High | Medium | Governance | Project architecture docs and repo decision |
