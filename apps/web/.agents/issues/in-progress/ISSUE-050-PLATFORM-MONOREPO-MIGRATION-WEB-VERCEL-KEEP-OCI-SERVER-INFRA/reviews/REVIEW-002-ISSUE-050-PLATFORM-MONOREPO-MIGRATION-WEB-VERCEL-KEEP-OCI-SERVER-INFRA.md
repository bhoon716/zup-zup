# REVIEW-002-ISSUE-050-PLATFORM-MONOREPO-MIGRATION-WEB-VERCEL-KEEP-OCI-SERVER-INFRA.md

- Issue: ISSUE-050-PLATFORM-MONOREPO-MIGRATION-WEB-VERCEL-KEEP-OCI-SERVER-INFRA
- Issue Title: [platform] Migrate to monorepo while keeping web on Vercel and server/infra on OCI
- Workflow: mvp-planning
- Reviewed At: 2026-07-08T03:41:00+09:00
- Passed: yes
- Hard Blocking Roles: none
- Deferrable Roles: none

## Scope
- Reviewed the root GitHub Actions workflow after adding the monorepo server deploy job, plus the deployment alignment docs and app README path fixes.

## Findings
- No hard blockers.
- The deploy path now matches the monorepo layout: build in `apps/server`, ship to `~/jbnu-sugang-helper/apps/server/`, and restart from that directory.

## Risk
- Remaining risk is external, not local: missing GitHub secrets or OCI host-side state could still block the actual deployment run.

## Next Action
- If the external Vercel/OCI settings are already in place, this issue can move toward closure after a final live deployment confirmation.
