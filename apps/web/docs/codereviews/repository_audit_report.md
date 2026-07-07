# Repository Audit Report

Audit date: 2026-07-06

This was a full repository audit across backend, frontend, infrastructure, database migrations, tests, and durable project documentation. It intentionally ignores prior `docs/reviews/` material and records this run under `docs/codereviews/`.

## Scope

- Backend: Spring Boot, JPA/Hibernate, Flyway, Batch, Security, notification, crawling, domain modules, tests.
- Frontend: Next.js App Router, React components/hooks, shared API client, PWA/service worker, lint/test harness.
- Infrastructure: Docker Compose, Nginx Proxy Manager, Loki/Promtail/Grafana/Prometheus, deployment workflow, log backup scripts.
- Documentation/governance: `docs/PRD.md`, `docs/ARD.md`, issue metadata, repository topology.

Excluded from source review: `.git`, `.gradle`, `node_modules`, `.next`, build outputs, and generated artifacts, except as repository-hygiene evidence.

## Verification

- `server`: `./gradlew test` passed.
- `web`: `npm test -- --run` passed.
- `web`: `npm run lint` failed.
  - `web/src/shared/api/client.ts:3`: boundary violation.
  - `web/src/widgets/header/header.test.tsx:12`, `:31`, `:46`: `no-explicit-any`.

## Executive Summary

The largest risks are not style-level. They are production-impacting:

- Production secrets and token material are present in deploy configuration.
- Fresh schema creation and migration safety are not currently proven by tests.
- Notification and subscription paths have real concurrency/data-integrity risks.
- Internal observability and datastore surfaces are published too broadly.
- Account deletion can leave orphaned user-owned data without FK/cascade or cleanup policy.
- CI/CD can deploy without running tests, and frontend lint is currently failing.

The codebase has meaningful test coverage and a recognizable layered package layout, but the actual implementation frequently crosses those boundaries: application services depend on presentation DTOs, shared frontend code imports feature state, and response contracts are mixed.

## Priority Counts

- P0: 6
- P1: 10
- P2: 16
- P3: 2

## Top P0/P1 Work

1. Rotate secrets and externalize production credentials.
2. Bind Discord OAuth `state` to a server-side nonce.
3. Remove public exposure of internal DB/Redis/Loki/metrics/admin ports.
4. Fix Flyway fresh migration and add production-like migration validation.
5. Stop destructive course-review migration behavior and define recovery.
6. Add DB constraints for subscriptions, devices, reviews, and primary timetables.
7. Make notification dedupe atomic and dispatch events after transaction commit.
8. Stop swallowing Spring Batch item failures.
9. Add CI gates that run server tests, web tests/lint/build, and infra checks.

## Artifacts

- [Audit Ledger](audit_ledger.md)
- [Index](index.md)
- [Security Findings](security_findings.md)
- [Database Findings](database_findings.md)
- [Infrastructure Findings](infrastructure_findings.md)
- [Architecture Findings](architecture_findings.md)
- [API Findings](api_findings.md)
- [Performance Findings](performance_findings.md)
- [Testing Findings](testing_findings.md)
- [Maintainability Findings](maintainability_findings.md)
- [Consistency Findings](consistency_findings.md)
- [Readability Findings](readability_findings.md)
- [Risk Register](risk_register.md)
- [Technical Debt Register](technical_debt_register.md)
- [Improvement Backlog](improvement_backlog.md)
- [Repository Scorecard](repository_scorecard.md)
- [Improvement Roadmap](improvement_roadmap.md)
- [Architecture Knowledge Base](architecture_knowledge_base.md)
- [ADR Suggestions](adr_suggestions.md)
- [Decision Log](decision_log.md)
