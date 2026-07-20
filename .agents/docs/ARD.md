# ARD

Architecture requirements document for the current project.

## Summary
This workspace is being consolidated into a monorepo-style root repository with `apps/web`, `apps/server`, and `infra`. Web remains on Vercel; server and infra continue on the OCI host.

## Stack
- Backend: Spring Boot 3.5.9 on Java 21, built with Gradle. The backend uses Spring Data JPA, Spring Security, OAuth2, Redis, Flyway, Spring Batch, QueryDSL, and OpenAPI.
- Frontend: Next.js 16.1.6 App Router on React 19.2.7, built with TypeScript, TanStack Query, Zustand, Vitest, and ESLint.
- Infrastructure: Docker Compose on a single OCI A1 ARM64 host. The runtime provides the backend app, MySQL, Redis, the existing host Nginx Proxy Manager edge, DuckDNS, an external uptime monitor, and an opt-in Prometheus/Loki/Alloy/Grafana observability profile. Prometheus scrapes only the app Actuator endpoint; host exporters, cAdvisor, and Alertmanager are not part of the minimal runtime.

## Storage / Auth / Deployment / Scale
- Database / storage: MySQL uses the Docker-managed named volume `sugang-helper-db-data` on the OCI instance. A scheduled logical dump is stored under the release root for protection from application-level mistakes; it is not a host-loss backup. Redis is ephemeral, and app uploads use a named volume. Prometheus uses `/var/lib/jbnu-sugang-helper/prometheus`, Loki uses `/var/lib/jbnu-sugang-helper/loki`, Alloy keeps read positions in `/var/lib/jbnu-sugang-helper/alloy`, and Grafana uses `/var/lib/jbnu-sugang-helper/grafana`.
- Auth requirement: No new auth surface for the plan itself; preserve existing deployment access controls.
- Expected scale: Single-host deployment with local log retention sized for one production instance.
- Deployment target: One OCI A1 ARM64 host managed through ARM64 GHCR images, Docker Compose, the existing NPM edge, and direct SSH/SCP as the OCI `ubuntu` account. NPM state and certificates remain outside git and application CD. The frontend remains on Vercel.

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
- Security concerns: Keep DB/Redis off host ports, bind the app to localhost, restrict SSH and sudo wrappers, keep runtime secrets root-only, and limit host mounts to MySQL/app data and configuration files. The minimal CD uses runtime SSH host-key collection instead of a pinned host-key secret.

## Related Files
- [PRD](PRD.md)
- [Repository topology decision](../../docs/decisions/2026-07-06-repository-topology.md)

## Minimal CI/CD redesign (2026-07-19)

The operating contract is recorded in the [single OCI CI/CD ADR](../../docs/decisions/2026-07-19-single-oci-cicd.md) and its [deployment runbook](../../docs/operations/deployment.md). It keeps Vercel frontend deployment separate from a single OCI A1 ARM64 backend Compose stack, publishes commit-SHA images to private GHCR, runs a least-privilege one-shot Flyway migration before readiness, and provides manual application-image redeployment by SHA.

ISSUE-128 reduces the GitHub path to PR-only CI and push-main CD, removes GitHub concurrency and the dedicated rollback workflow, and uses a temporary per-deploy staging directory. Prometheus/Loki/Alloy/Grafana are included as an opt-in production profile; Prometheus stores app metrics for a short local retention window and Loki provides searchable 30-day filesystem logs. MySQL uses a Docker named volume and a scheduled OCI-local logical dump; off-host Object Storage backup remains a separate future improvement. The design intentionally does not provide DB automatic rollback or infrastructure-image rollback.
