# ISSUE-022-LOGIN-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-28T09:42:23.266Z
- Updated At: 2026-06-28T09:42:23.266Z
- Log: .agents/issues/logs/ISSUE-022-LOGIN-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-022-LOGIN-PLAN/workflow-state.json

## Goal
Grill log item: convert the authentication stack to a stateless JWT flow. Remove HttpSession reads/writes from JwtAuthenticationFilter, AuthService, and OAuth2SuccessHandler; keep OAuth2 success on a silent-login path that sets only the refresh-token cookie and redirects; ensure access tokens are obtained after redirect via reissue. Acceptance: no auth-token session storage remains, OAuth2 login works without server-side session token state, and verification evidence is recorded.

## Acceptance Criteria
- The issue "[server] Remove HttpSession from auth flow and adopt silent login" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Grill log item: convert the authentication stack to a stateless JWT flow. Remove HttpSession reads/writes from JwtAuthenticationFilter, AuthService, and OAuth2SuccessHandler; keep OAuth2 success on a silent-login path that sets only the refresh-token cookie and redirects; ensure access tokens are obtained after redirect via reissue. Acceptance: no auth-token session storage remains, OAuth2 login works without server-side session token state, and verification evidence is recorded.

## Decisions
- Decisions are stored under `decisions/`.
