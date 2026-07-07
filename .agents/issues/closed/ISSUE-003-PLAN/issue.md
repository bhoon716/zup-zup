# ISSUE-003-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:26.541Z
- Updated At: 2026-06-23T02:11:26.541Z
- Log: .agents/issues/logs/ISSUE-003-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-003-PLAN/workflow-state.json

## Goal
HttpLoggingFilter logs the full request URI including query strings, which exposes OAuth callback codes. JwtProvider also logs the raw blacklisted token value. These values are reusable credentials and should never enter application logs. Evidence: server/src/main/java/bhoon/sugang_helper/common/filter/HttpLoggingFilter.java, server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtProvider.java.

## Acceptance Criteria
- The issue "[server] Stop logging OAuth codes and JWTs" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
HttpLoggingFilter logs the full request URI including query strings, which exposes OAuth callback codes. JwtProvider also logs the raw blacklisted token value. These values are reusable credentials and should never enter application logs. Evidence: server/src/main/java/bhoon/sugang_helper/common/filter/HttpLoggingFilter.java, server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtProvider.java.

## Decisions
- Decisions are stored under `decisions/`.
