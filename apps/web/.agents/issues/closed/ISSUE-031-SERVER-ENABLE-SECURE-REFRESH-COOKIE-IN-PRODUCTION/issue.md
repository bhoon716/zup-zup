# ISSUE-031-SERVER-ENABLE-SECURE-REFRESH-COOKIE-IN-PRODUCTION

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-03T13:13:58+09:00
- Updated At: 2026-07-03T14:10:00+09:00
- Completed At: 2026-07-03T14:10:00+09:00
- Log: .agents/issues/logs/ISSUE-031-SERVER-ENABLE-SECURE-REFRESH-COOKIE-IN-PRODUCTION.md
- Workflow State: .agents/issues/closed/ISSUE-031-SERVER-ENABLE-SECURE-REFRESH-COOKIE-IN-PRODUCTION/workflow-state.json

## Goal
Make the refresh token cookie secure in production so the cookie is only transmitted over HTTPS in the deployed environment.

## Acceptance Criteria
- Production refresh cookies are issued with `Secure=true`.
- Existing `HttpOnly`, path, and `SameSite` behavior is preserved unless a documented exception exists.
- Verification evidence shows the production cookie attributes are correct.

## Dependencies
- None.

## Evidence Required
- Code diff or test output showing the cookie is emitted with secure attributes in production.
- Verification output from the relevant auth path or cookie assertion test.

## Notes
- Keep the change limited to production cookie handling and avoid widening auth behavior.
