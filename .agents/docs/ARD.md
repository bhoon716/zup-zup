# ARD

Architecture requirements document for the current project.

## Summary
This workspace is being consolidated into a monorepo-style root repository with `apps/web`, `apps/server`, and `infra`. Web remains on Vercel; server and infra continue on the OCI host.

## Stack
- Backend: Spring Boot 3.5.9 on Java 21, built with Gradle. The backend uses Spring Data JPA, Spring Security, OAuth2, Redis, Flyway, Spring Batch, QueryDSL, and OpenAPI.
- Frontend: Next.js 16.1.6 App Router on React 19.2.7, built with TypeScript, TanStack Query, Zustand, Vitest, and ESLint.
- Infrastructure: Docker Compose on a single OCI A1 ARM64 host. The runtime provides the backend app, MySQL, Redis, host Nginx, certbot, DuckDNS, an external uptime monitor, and an opt-in Loki/Alloy/Grafana log-search profile. Prometheus and Alertmanager are not part of the minimal runtime.

## Storage / Auth / Deployment / Scale
- Database / storage: MySQL uses an OCI block volume mounted at `/var/lib/jbnu-sugang-helper/mysql` through a Docker named volume. Redis is ephemeral, and app uploads use a named volume. Loki uses `/var/lib/jbnu-sugang-helper/loki`, Alloy keeps read positions in `/var/lib/jbnu-sugang-helper/alloy`, and Grafana uses `/var/lib/jbnu-sugang-helper/grafana`.
- Auth requirement: No new auth surface for the plan itself; preserve existing deployment access controls.
- Expected scale: Single-host deployment with local log retention sized for one production instance.
- Deployment target: One OCI A1 ARM64 host managed through GHCR images, Docker Compose, host Nginx/certbot, and a restricted SSH wrapper accessed by the OCI `ubuntu` account. The frontend remains on Vercel.

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
- Security concerns: Keep DB/Redis off host ports, bind the app to localhost, restrict SSH and sudo wrappers, keep runtime secrets root-only, and limit host mounts to MySQL/app data and configuration files.

## Related Files
- [PRD](PRD.md)
- [Repository topology decision](../../docs/decisions/2026-07-06-repository-topology.md)

## Minimal CI/CD redesign (2026-07-19)

The operating contract is recorded in the [single OCI CI/CD ADR](../../docs/decisions/2026-07-19-single-oci-cicd.md) and its [deployment runbook](../../docs/operations/deployment.md). It keeps Vercel frontend deployment separate from a single OCI A1 ARM64 backend Compose stack, publishes commit-SHA images to private GHCR, runs a least-privilege one-shot Flyway migration before readiness, and provides manual application-image redeployment by SHA.

ISSUE-128 reduces the GitHub path to PR-only CI and push-main CD, removes GitHub concurrency and the dedicated rollback workflow, and uses a server-side deploy lock. Loki/Alloy/Grafana are included as an opt-in production profile for searchable 30-day filesystem logs; Object Storage long-term retention and DB backup remain separate operational work because their bucket, IAM, and restore contracts are not yet defined. The design intentionally does not provide DB automatic rollback or infrastructure-image rollback.
