# ISSUE-034-WEB-FIX-INFINITE-RELOAD-ON-LOGOUT

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-03T15:15:00+09:00
- Updated At: 2026-07-03T15:14:00+09:00
- Completed At: 2026-07-03T15:14:00+09:00
- Log: .agents/issues/logs/ISSUE-034-WEB-FIX-INFINITE-RELOAD-ON-LOGOUT.md
- Workflow State: .agents/issues/closed/ISSUE-034-WEB-FIX-INFINITE-RELOAD-ON-LOGOUT/workflow-state.json

## Goal
Resolve the infinite reloading loop that occurs when clicking logout or when session checks fail on the login page. Prevent checkSession from executing when no isLoggedIn cookie is present, and prevent redundant redirects when already on the login page.

## Acceptance Criteria
- Logging out correctly deletes auth state and routes to `/login` without infinite page reloads.
- Client-side initialization does not request `/api/v1/users/me` if the `is_logged_in` cookie is missing or false.
- The `redirectToLogin` utility does not trigger a page reload if the browser is already on the `/login` page.

## Dependencies
- None.

## Evidence Required
- Successful test execution of `providers.test.tsx` and `client.test.ts`.
- No infinite loop occurs when logging out or accessing login routes.

## Description
When logout is triggered or token refresh fails on the login route, the app attempts to redirect to `/login` via page reload, which boots `Providers`, fires `checkSession`, fails with `401`, attempts refresh, fails, and redirects to `/login` again, causing an infinite loop.
