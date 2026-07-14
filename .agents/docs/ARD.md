# ARD

Architecture requirements document for the current project.

## Summary
This workspace is being consolidated into a monorepo-style root repository with `apps/web`, `apps/server`, and `infra`. Web remains on Vercel; server and infra continue on the OCI host.

## Stack
- Backend: Spring Boot 3.5.9 on Java 21, built with Gradle. The backend uses Spring Data JPA, Spring Security, OAuth2, Redis, Flyway, Spring Batch, QueryDSL, and OpenAPI.
- Frontend: Next.js 16.1.6 App Router on React 19.2.7, built with TypeScript, TanStack Query, Zustand, Vitest, and ESLint.
- Infrastructure: Docker Compose on a self-hosted Linux server. The infra repository provides MySQL, Redis, Nginx Proxy Manager, Prometheus, Grafana, Loki, and Promtail.

## Storage / Auth / Deployment / Scale
- Database / storage: Host-mounted storage on the server. Loki persists under `/var/lib/jbnu-sugang-helper/loki`, Promtail state persists under `/var/lib/jbnu-sugang-helper/promtail`, and application logs are written under `/var/log/jbnu-sugang-helper/server`.
- Auth requirement: No new auth surface for the plan itself; preserve existing deployment access controls.
- Expected scale: Single-host deployment with local log retention sized for one production instance.
- Deployment target: Self-hosted Linux host managed via Docker Compose, without AWS or external IaaS.

## Repository Topology
- The active source-of-truth layout is the root monorepo with `apps/web`, `apps/server`, and `infra`.
- `web/` and `server/` legacy standalone directories may remain locally during migration, but they are not the intended long-term topology.
- Web is an independent npm project managed from `apps/web`; server release commands run from `apps/server`, and infra verification and compose operations run from `infra`.
- Deployment alignment: Vercel should point at `apps/web`; server release commands should run from `apps/server`; infra verification and compose operations should run from `infra`.
- GitHub Actions should verify path-specific changes and publish the server from `apps/server` to the OCI host.
- Root documentation is the source of truth for architecture and topology decisions.
- Repo-local READMEs remain the source of truth for service-specific commands and developer workflow.

## Test Strategy / Security
- Test strategy: Use path-based CI to verify `apps/web`, `apps/server`, and `infra` independently. Add a shared package to the affected path set only when a real cross-application contract is introduced.
- Lint strategy: Not applicable for this planning document.
- Security concerns: Avoid ephemeral state for durable logs, keep host mounts minimal, remove the Docker socket from Promtail, and limit host access to the log and backup directories.

## Related Files
- [PRD](PRD.md)
- [Repository topology decision](../../docs/decisions/2026-07-06-repository-topology.md)
