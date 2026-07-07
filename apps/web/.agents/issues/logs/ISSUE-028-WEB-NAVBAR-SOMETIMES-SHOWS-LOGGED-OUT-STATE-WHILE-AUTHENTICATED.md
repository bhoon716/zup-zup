# ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED Log

- Issue: [web] Navbar sometimes shows logged-out state while authenticated
- Log File: .agents/issues/logs/ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED.md

## 2026-07-02T19:48:10+09:00

- Step: Issue Created
- Actions:
  - Created issue workspace for ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED
  - Selected workflow bug-fix
- Evidence:
  - [file] issue.md (issue.md)
  - [file] issue.json (issue.json)
  - [file] workflow-state.json (workflow-state.json)
  - [file] decisions/README.md (decisions/README.md)
  - [file] reviews/README.md (reviews/README.md)
- Summary: Issue ISSUE-028-WEB-NAVBAR-SOMETIMES-SHOWS-LOGGED-OUT-STATE-WHILE-AUTHENTICATED was initialized for the intermittent navbar authentication-state bug.
- Next Step: Intake

## 2026-07-03T13:45:00+09:00

- Step: Closed
- Actions:
  - Added isLoading prop and passed to NavLinks component
  - Rendered Skeleton loaders during checkSession loading state
  - Added NavLinks unit tests for verification
- Evidence:
  - [file] web/src/widgets/header/header.tsx
  - [file] web/src/widgets/header/ui/nav-links.tsx
  - [file] web/src/widgets/header/ui/nav-links.test.tsx
- Summary: The navbar flickering bug is successfully fixed and verified by running vitest and building next app.
- Next Step: None
