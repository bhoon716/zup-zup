# Architecture Knowledge Base

## Current System

- Backend: Spring Boot 3.x, Java 21, JPA/Hibernate, Flyway, Spring Security, OAuth2, Redis, Spring Batch.
- Frontend: Next.js App Router, React, TanStack Query, Vitest, ESLint boundaries.
- Infrastructure: Docker Compose, Nginx Proxy Manager, MySQL, Redis, Prometheus, Grafana, Loki, Promtail.
- Deployment: Server CD workflow builds and deploys backend container on `main`.

## Intended Patterns

- Backend package structure implies `presentation -> application -> domain` with `infra` adapters.
- Frontend structure implies `app/widgets/features/shared` boundaries.
- Durable logs and observability state should live under host-owned `/var/log` and `/var/lib` paths.

## Observed Drift

- Backend application services import presentation DTOs.
- Frontend shared client imports feature auth store.
- API response shapes are mixed.
- Tests do not exercise production Flyway migrations.
- Root docs currently describe log retention more than the full application architecture.

## Rules Proposed By This Audit

- No production secrets in source or monolithic deploy config blobs.
- Migrations must be forward-only, non-destructive by default, and production-DB tested.
- External side effects must occur after transaction commit.
- Business uniqueness belongs in the DB as well as service code.
- CI deploys must depend on server/web/infra gates.
