# ISSUE-046-PLATFORM-ALIGN-INFRA-VERIFICATION-WITH-THE-DEPLOYED-COMPOSE-FILE

- Type: bugfix
- State: open
- Workflow: bug-fix
- Created At: 2026-07-07T02:42:49+09:00
- Updated At: 2026-07-07T02:42:49+09:00
- Log: .agents/issues/logs/ISSUE-046-PLATFORM-ALIGN-INFRA-VERIFICATION-WITH-THE-DEPLOYED-COMPOSE-FILE.md
- Workflow State: .agents/issues/open/ISSUE-046-PLATFORM-ALIGN-INFRA-VERIFICATION-WITH-THE-DEPLOYED-COMPOSE-FILE/workflow-state.json

## Goal
Make the infra verification gate validate the same compose definition that deployment actually ships and runs.

## Acceptance Criteria
- The compose file validated in CI is the same file deployed on the server.
- The deploy job cannot drift from the verified infra definition without failing verification.
- Infra policy checks continue to cover ports, mounts, and image/version reproducibility.

## Dependencies
- Current deployment workflow structure
- Server and infra compose ownership

## Evidence Required
- Workflow diff
- CI run output
- Review evidence

## Description
The infra verification gate currently checks a different compose source than the one the deploy job copies and runs. That leaves a gap where CI can pass while production uses an unverified definition.

## Decisions
- Decisions are stored under `decisions/`.
