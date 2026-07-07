# ISSUE-010-API

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:29.415Z
- Updated At: 2026-06-23T02:11:29.415Z
- Log: .agents/issues/logs/ISSUE-010-API.md
- Workflow State: .agents/issues/closed/ISSUE-010-API/workflow-state.json

## Goal
Refresh tokens are currently signed and validated like access tokens, and JwtAuthenticationFilter accepts any valid JWT from the Authorization header or session. A leaked refresh token therefore becomes a bearer credential for normal API access. Evidence: server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtProvider.java, server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtAuthenticationFilter.java, server/src/main/java/bhoon/sugang_helper/auth/application/AuthService.java.

## Acceptance Criteria
- The issue "[server] Refresh token must not authenticate normal API requests" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Refresh tokens are currently signed and validated like access tokens, and JwtAuthenticationFilter accepts any valid JWT from the Authorization header or session. A leaked refresh token therefore becomes a bearer credential for normal API access. Evidence: server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtProvider.java, server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtAuthenticationFilter.java, server/src/main/java/bhoon/sugang_helper/auth/application/AuthService.java.

## Decisions
- Decisions are stored under `decisions/`.
