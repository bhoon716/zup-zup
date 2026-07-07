# Architecture Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `ARCH-001` High: Backend application services import presentation DTOs.
- `ARCH-002` High: Frontend shared API client imports feature auth state and lint fails.
- `DOC-001` Medium: Project architecture docs and repository topology do not match the actual system.
- `MAINT-001` Medium: Search, notification, settings, and timetable modules concentrate multiple responsibilities.

## Observed Architecture

- Backend is organized by feature package with `presentation`, `application`, `domain`, and `infra` subpackages.
- Frontend appears intended to follow feature-sliced boundaries with `app`, `widgets`, `features`, and `shared`.
- Infrastructure is a separate nested Git repository with Docker Compose and observability config.

## Architecture Drift

Declared boundaries exist, but enforcement is inconsistent:

- Backend application layer is coupled upward to HTTP DTOs.
- Frontend `shared` depends on `features`, and features depend on other features.
- Root documentation does not describe the actual Spring Boot / Next.js / Docker Compose system.
