# Testing Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `TEST-001` High: CD skips server tests and no web/infra CI gate was found.
- `TEST-002` Medium: Request-level authorization and Flyway migration paths lack integration coverage.
- `TEST-003` Medium: Web lint is red and one server logging test uses timing sleep.

## Current Command Evidence

- `server`: `./gradlew test` passed.
- `web`: `npm test -- --run` passed.
- `web`: `npm run lint` failed.

## Required Gates

- Server: `./gradlew clean test`.
- Server migration: Testcontainers MySQL Flyway migrate + JPA validate.
- Web: `npm ci`, `npm test -- --run`, `npm run lint`, `npm run build`.
- Infra: compose config and policy scripts.
- Security: request-level auth matrix for public/authenticated/admin endpoints.
