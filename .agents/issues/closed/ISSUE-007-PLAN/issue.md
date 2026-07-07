# ISSUE-007-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:28.120Z
- Updated At: 2026-06-23T02:11:28.120Z
- Log: .agents/issues/logs/ISSUE-007-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-007-PLAN/workflow-state.json

## Goal
Email verification codes are generated with java.util.Random, and the feedback-style abuse controls are still stubbed elsewhere in the service layer. Use a cryptographically stronger generator and enforce a real request throttle. Evidence: server/src/main/java/bhoon/sugang_helper/user/application/EmailVerificationService.java, server/src/main/java/bhoon/sugang_helper/feedback/application/FeedbackService.java.

## Acceptance Criteria
- The issue "[server] Harden email verification codes and add rate limiting" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Email verification codes are generated with java.util.Random, and the feedback-style abuse controls are still stubbed elsewhere in the service layer. Use a cryptographically stronger generator and enforce a real request throttle. Evidence: server/src/main/java/bhoon/sugang_helper/user/application/EmailVerificationService.java, server/src/main/java/bhoon/sugang_helper/feedback/application/FeedbackService.java.

## Decisions
- Decisions are stored under `decisions/`.
