# Monorepo Migration Decision

## Context
- The workspace previously operated as three separate repositories: `server/`, `web/`, and `infra/`.
- Cross-repo changes made CI, deployment, and ownership boundaries harder to maintain.
- The current operating model still keeps web on Vercel and server/infra on the OCI host.

## Decision
- Consolidate the project into a monorepo-style root repository.
- Use `apps/web`, `apps/server`, and `infra` as the active layout.
- Keep web deployed on Vercel.
- Keep server and infra on the OCI host.
- Use path-based CI separation so web, server, and infra changes can be verified independently.

## Consequences
- Add a shared package only when a real cross-application contract exists; an empty placeholder package is not maintained.
- Web changes remain isolated from OCI operations, preserving the low-cost Vercel deployment path.
- Server and infra continue to share the same host/runtime model, reducing deployment churn.
- The old standalone directories can exist temporarily during migration, but the monorepo layout is the source of truth.

## Deployment Alignment
- Configure the existing Vercel project to use `apps/web` as the root directory.
- Keep server deployment commands rooted at `apps/server`.
- Keep infra verification and compose operations rooted at `infra`.
- Use a root GitHub Actions workflow to verify path-specific changes and deploy the server artifact to the OCI host.

## Rollout Notes
- Roll out in the order `web` -> `server` -> `infra`.
- Use a one-shot web cutover to the existing Vercel project once the monorepo path is ready.
- Keep the rollback threshold tied to repeated errors on authentication, login, or major pages.
