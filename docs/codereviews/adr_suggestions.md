# Architecture Decision Suggestions

## ADR-001: Secret Management Model

Decision needed: Use environment variables, mounted secret files, or a managed secret store for production values.

Recommended direction: Start with mounted secret files/env vars for current single-host deployment, with mandatory rotation and deploy preflight.

## ADR-002: API Envelope Policy

Decision needed: Whether all JSON responses use `CommonResponse<T>` or whether raw responses are explicitly allowed.

Recommended direction: Standardize on `CommonResponse<T>` for success and `ErrorResponse` for failures, except `204 No Content`.

## ADR-003: Migration Validation Strategy

Decision needed: How production migrations are validated locally/CI.

Recommended direction: Testcontainers MySQL Flyway migration plus JPA validate in CI.

## ADR-004: Notification Delivery Semantics

Decision needed: Best-effort async event listener vs transactional outbox.

Recommended direction: Outbox for durable seat-opened events if notifications are user-facing commitments; otherwise after-commit listener with atomic dedupe.

## ADR-005: Repository Topology

Decision needed: Monorepo, submodules, or explicit multi-repo workspace.

Recommended direction: If cross-stack issues/docs remain in this root, make the root a real versioned repository or formalize pinned submodules.
