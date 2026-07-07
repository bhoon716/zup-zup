# ISSUE-002-SERVER-VERIFY-DISCORD-OAUTH-STATE-BEFORE-LINKING-ACCOUNTS

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:26.126Z
- Updated At: 2026-06-23T02:11:26.126Z
- Log: .agents/issues/logs/ISSUE-002-SERVER-VERIFY-DISCORD-OAUTH-STATE-BEFORE-LINKING-ACCOUNTS.md
- Workflow State: .agents/issues/closed/ISSUE-002-SERVER-VERIFY-DISCORD-OAUTH-STATE-BEFORE-LINKING-ACCOUNTS/workflow-state.json

## Goal
The Discord callback accepts the authorization code and only uses state to choose the redirect path. The state value is not bound to the initiator and is not validated, so a forged callback can link the wrong Discord account to the victim session. Evidence: server/src/main/java/bhoon/sugang_helper/user/presentation/UserController.java, web/src/app/(user)/onboarding/page.tsx, web/src/app/(user)/settings/page.tsx.

## Acceptance Criteria
- The issue "[server] Verify Discord OAuth state before linking accounts" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
The Discord callback accepts the authorization code and only uses state to choose the redirect path. The state value is not bound to the initiator and is not validated, so a forged callback can link the wrong Discord account to the victim session. Evidence: server/src/main/java/bhoon/sugang_helper/user/presentation/UserController.java, web/src/app/(user)/onboarding/page.tsx, web/src/app/(user)/settings/page.tsx.

## Decisions
- Decisions are stored under `decisions/`.
