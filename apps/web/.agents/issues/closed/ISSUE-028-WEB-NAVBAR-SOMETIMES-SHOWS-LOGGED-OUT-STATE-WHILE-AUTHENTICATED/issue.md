# ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-02T19:48:10+09:00
- Updated At: 2026-07-03T13:45:00+09:00
- Completed At: 2026-07-03T13:45:00+09:00
- Log: .agents/issues/logs/ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED.md
- Workflow State: .agents/issues/closed/ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED/workflow-state.json

## Goal
The top bar sometimes renders as if the user is logged out even though the session is authenticated. Investigate the UI state source, find the mismatch or timing issue, and make the navbar consistently reflect the real login state.

## Acceptance Criteria
- The intermittent logged-out navbar state is reproducible or explained with a concrete root cause.
- The navbar always shows the correct authenticated state after login and on page refresh.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Reproduction or root-cause evidence
- Implementation or review evidence
- Verification output

## Description
The top bar sometimes appears logged out even when the user is actually authenticated. This suggests a client-side state synchronization or initialization timing issue around session loading, auth hydration, or navbar rendering.

## Decisions
- Decisions are stored under `decisions/`.
