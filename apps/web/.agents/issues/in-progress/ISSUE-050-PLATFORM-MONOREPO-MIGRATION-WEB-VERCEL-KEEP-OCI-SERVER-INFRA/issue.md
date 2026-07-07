# ISSUE-050-PLATFORM-MONOREPO-MIGRATION-WEB-VERCEL-KEEP-OCI-SERVER-INFRA

- Type: planning
- State: in-progress
- Workflow: mvp-planning
- Created At: 2026-07-08T02:06:44+09:00
- Updated At: 2026-07-08T02:06:44+09:00
- Log: .agents/issues/logs/ISSUE-050-PLATFORM-MONOREPO-MIGRATION-WEB-VERCEL-KEEP-OCI-SERVER-INFRA.md
- Workflow State: .agents/issues/in-progress/ISSUE-050-PLATFORM-MONOREPO-MIGRATION-WEB-VERCEL-KEEP-OCI-SERVER-INFRA/workflow-state.json

## Goal
Consolidate web, server, and infra into a single GitHub monorepo with apps/web, apps/server, infra, and packages/shared while keeping web deployed on Vercel and preserving the agreed rollout and rollback plan.

## Acceptance Criteria
- The repository topology is documented and implemented as `apps/web`, `apps/server`, `infra`, and `packages/shared`.
- Web remains deployed on Vercel while server and infra continue on OCI.
- CI uses path-based separation for web, server, and infra changes.
- The rollout and rollback strategy from the grill session is recorded and referenceable from the issue.

## Dependencies
- Current web, server, and infra repositories
- Vercel project access
- OCI deployment access
- Existing CI workflow

## Evidence Required
- Repository topology diff
- CI configuration diff
- Verification output
- Review evidence
- Grill log reference

## Description
This issue tracks the agreed architecture direction: keep web on Vercel, merge web/server/infra into one GitHub monorepo, use `apps/web`, `apps/server`, `infra`, introduce `packages/shared`, and apply path-based CI. The decision record lives in the grill log at [/Users/bhoon/Project/jbnu-sugang-helper/.agents/skills/ultra-grill-me/logs/2026-07-08-infra-deployment-cicd-grill.md](/Users/bhoon/Project/jbnu-sugang-helper/.agents/skills/ultra-grill-me/logs/2026-07-08-infra-deployment-cicd-grill.md).

## Decisions
- Decisions are stored under `decisions/`.
