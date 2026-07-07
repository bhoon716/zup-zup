# ISSUE-045-PLATFORM-ADD-CROSS-REPO-CHECKOUT-AUTHORIZATION-FOR-CI-VERIFY-JOBS

- Type: bugfix
- State: open
- Workflow: bug-fix
- Created At: 2026-07-07T02:42:49+09:00
- Updated At: 2026-07-07T02:42:49+09:00
- Log: .agents/issues/logs/ISSUE-045-PLATFORM-ADD-CROSS-REPO-CHECKOUT-AUTHORIZATION-FOR-CI-VERIFY-JOBS.md
- Workflow State: .agents/issues/open/ISSUE-045-PLATFORM-ADD-CROSS-REPO-CHECKOUT-AUTHORIZATION-FOR-CI-VERIFY-JOBS/workflow-state.json

## Goal
Make the web and infra verification jobs able to check out their target repositories reliably in GitHub Actions.

## Acceptance Criteria
- The CI workflow can authenticate to the required repository checkouts or is rewritten to avoid unauthorized cross-repo access.
- Verify-web and verify-infra run successfully with the chosen access pattern.
- The workflow fails clearly if the required checkout permission is unavailable.

## Dependencies
- GitHub repository access policy
- CI secrets or GitHub App token

## Evidence Required
- Workflow diff
- CI run output
- Checkout access proof

## Description
The new CI gate checks out sibling repositories without an explicit access strategy. If those repositories are private or permissioned differently, verification jobs will fail before they can validate anything.

## Decisions
- Decisions are stored under `decisions/`.
