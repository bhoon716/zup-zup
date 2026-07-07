# Infrastructure Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `SEC-003` High: Internal services and Loki are host-published.
- `INFRA-001` High: Firebase key mount is referenced but not delivered by CD.
- `INFRA-002` Medium: Grafana state and image versions are not reproducible.
- `INFRA-003` Medium: Log retention checks and mounts are incomplete.
- `TEST-001` High: CD skips tests and no web/infra CI gate was found.

## Operational Risks

- A fresh server may need manual secret pre-provisioning before FCM works.
- Observability stack state can drift or disappear.
- Mutable `latest` images make production behavior dependent on pull time.
- Log retention requirements are documented but only partially enforced.

## Minimum Infrastructure Gate

Add CI checks for:

- `docker compose config`
- no public datastore/cache/log service ports
- no Docker socket mount
- expected Loki/Promtail host paths and positions file
- json-file log caps
- backup/restore script dry run against temporary paths
