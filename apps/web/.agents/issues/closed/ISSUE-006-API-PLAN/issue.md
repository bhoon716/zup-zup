# ISSUE-006-API-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:27.657Z
- Updated At: 2026-06-23T02:11:27.657Z
- Log: .agents/issues/logs/ISSUE-006-API-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-006-API-PLAN/workflow-state.json

## Goal
The Axios client uses NEXT_PUBLIC_API_URL, while next.config.ts rewrites use API_URL. If API_URL is not separately supplied, OAuth callback and API/upload rewrites will break in deployment. Evidence: web/src/shared/api/client.ts, web/next.config.ts, web/docs/troubleshooting.md.

## Acceptance Criteria
- The issue "[web] Use a single API base URL env var for rewrites and client" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
The Axios client uses NEXT_PUBLIC_API_URL, while next.config.ts rewrites use API_URL. If API_URL is not separately supplied, OAuth callback and API/upload rewrites will break in deployment. Evidence: web/src/shared/api/client.ts, web/next.config.ts, web/docs/troubleshooting.md.

## Decisions
- Decisions are stored under `decisions/`.
