# ISSUE-023-RULE-UPDATE-PROJECT-OVERRIDES

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-28T09:42:31.013Z
- Updated At: 2026-06-28T09:42:31.013Z
- Log: .agents/issues/logs/ISSUE-023-RULE-UPDATE-PROJECT-OVERRIDES.md
- Workflow State: .agents/issues/closed/ISSUE-023-RULE-UPDATE-PROJECT-OVERRIDES/workflow-state.json

## Goal
Grill log item: secure the refresh-token pipeline. Make /api/auth/reissue POST-only, require a custom request header to reduce CSRF/RTR abuse, add refresh-token rotation with a short grace period and reuse detection, and apply Redis failure policies that fail open for access-token validation while failing closed for refresh-token reissue. Acceptance: refresh reuse is detected and revoked outside the grace window, concurrent legitimate refreshes are handled safely, access-token auth remains usable when Redis blacklist checks are unavailable, and refresh reissue is denied on Redis failure.

## Acceptance Criteria
- The issue "[server] Harden refresh reissue with CSRF guard, rotation, reuse detection, and Redis failure policy" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Grill log item: secure the refresh-token pipeline. Make /api/auth/reissue POST-only, require a custom request header to reduce CSRF/RTR abuse, add refresh-token rotation with a short grace period and reuse detection, and apply Redis failure policies that fail open for access-token validation while failing closed for refresh-token reissue. Acceptance: refresh reuse is detected and revoked outside the grace window, concurrent legitimate refreshes are handled safely, access-token auth remains usable when Redis blacklist checks are unavailable, and refresh reissue is denied on Redis failure.

## Decisions
- Decisions are stored under `decisions/`.
