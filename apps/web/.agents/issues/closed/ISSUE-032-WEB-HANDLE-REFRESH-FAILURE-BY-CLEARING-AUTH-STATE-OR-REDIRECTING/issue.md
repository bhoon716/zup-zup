# ISSUE-032-WEB-HANDLE-REFRESH-FAILURE-BY-CLEARING-AUTH-STATE-OR-REDIRECTING

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-03T13:13:58+09:00
- Updated At: 2026-07-03T13:45:00+09:00
- Completed At: 2026-07-03T13:45:00+09:00
- Log: .agents/issues/logs/ISSUE-032-WEB-HANDLE-REFRESH-FAILURE-BY-CLEARING-AUTH-STATE-OR-REDIRECTING.md
- Workflow State: .agents/issues/closed/ISSUE-032-WEB-HANDLE-REFRESH-FAILURE-BY-CLEARING-AUTH-STATE-OR-REDIRECTING/workflow-state.json

## Goal
Ensure that when auth refresh fails, the client clears stale authenticated state and moves the user to a safe logged-out state instead of leaving stale auth state behind.

## Acceptance Criteria
- Refresh failure clears or invalidates the client auth state consistently.
- The user is redirected or otherwise placed into the expected logged-out flow.
- Verification evidence covers the refresh-failure path.

## Dependencies
- None.

## Evidence Required
- Regression test or reproduction output covering refresh failure.
- Verification output showing the client no longer stays in a stale authenticated state.

## Notes
- Keep the fix focused on refresh-failure handling and avoid unrelated auth refactors.
