# Docs Index

Use this page as the entry point for repo knowledge.

## Core Docs
- [PRD](PRD.md): product scope, users, non-goals, and acceptance criteria.
- [ARD](ARD.md): architecture, deployment, storage, and test expectations.

## Project Docs
- [Project Release Notes](../../docs/feature-updates.md): web and server release history.
- [Troubleshooting](../../docs/troubleshooting.md): web, server, and operational troubleshooting notes with continuous numbering.

## Decisions
- [Repository topology decision](../../docs/decisions/2026-07-06-repository-topology.md): historical nested multi-repo workspace model and ownership boundaries.
- [Monorepo migration decision](../../docs/decisions/2026-07-08-monorepo-migration-web-vercel-server-infra.md): current root monorepo layout, Vercel retention, and OCI deployment split.
- [Single OCI CI/CD decision](../../docs/decisions/2026-07-19-single-oci-cicd.md): target minimal CI/CD, migration, rollback, and cutover contract.
- [Ubuntu SSH-only CI/CD decision](../../docs/decisions/2026-07-20-ubuntu-ssh-cicd-simplification.md): lightweight direct Ubuntu deployment contract.
- [Deployment runbook](../../docs/operations/deployment.md): target bootstrap, deployment, rollback, and server replacement procedures.
- [GitHub settings checklist](../../docs/operations/github-settings.md): main protection and repository Actions secrets.
- [Cutover checklist](../../docs/operations/cutover-checklist.md): old-stack preservation, cutover acceptance, and rebuild rehearsal.
