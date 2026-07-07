# ISSUE-040-PLATFORM-ADD-CI-TEST-AND-INFRA-VERIFICATION-GATES

- Type: bugfix
- State: open
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T02:46:59+09:00
- Log: .agents/issues/logs/ISSUE-040-PLATFORM-ADD-CI-TEST-AND-INFRA-VERIFICATION-GATES.md
- Workflow State: .agents/issues/open/ISSUE-040-PLATFORM-ADD-CI-TEST-AND-INFRA-VERIFICATION-GATES/workflow-state.json

## Goal
Add CI and deployment verification gates so server tests, web tests/lint/build, migration checks, and infrastructure policy checks must pass before deploy.

## Acceptance Criteria
- Server, web, and infrastructure verification steps run in CI before deploy.
- Migration and request-level regression coverage exists for the critical paths called out by the audit.
- Infrastructure checks cover exposed ports, secret mounts, logging paths, and image/version reproducibility.

## Dependencies
- Current GitHub Actions workflow
- CI runner access

## Evidence Required
- Workflow diff
- CI run output
- Review evidence

## Description
The current deploy flow can skip tests, and the infra verification surface is too weak. Add the missing gates so regressions, config drift, and deployment mistakes are caught before production.

## Decisions
- Decisions are stored under `decisions/`.
