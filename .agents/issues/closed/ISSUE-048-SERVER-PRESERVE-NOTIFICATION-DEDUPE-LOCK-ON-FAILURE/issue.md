# ISSUE-048-SERVER-PRESERVE-NOTIFICATION-DEDUPE-LOCK-ON-FAILURE

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-07T01:50:32+09:00
- Updated At: 2026-07-07T01:55:02+09:00
- Completed At: 2026-07-07T01:55:02+09:00
- Log: .agents/issues/logs/ISSUE-048-SERVER-PRESERVE-NOTIFICATION-DEDUPE-LOCK-ON-FAILURE.md
- Workflow State: .agents/issues/closed/ISSUE-048-SERVER-PRESERVE-NOTIFICATION-DEDUPE-LOCK-ON-FAILURE/workflow-state.json

## Goal
Keep the notification dedupe lock in place when delivery fails so a partially delivered alert is not immediately retried and double-sent.

## Acceptance Criteria
- Failed seat-opened notification delivery does not delete the dedupe key.
- Duplicate events remain suppressed for the configured TTL after a delivery failure.
- The failure-path behavior is covered by a regression test.

## Dependencies
- Current notification delivery flow
- Redis-based dedupe semantics

## Evidence Required
- Implementation diff
- Regression test output
- Review evidence

## Description
The current failure path removes the dedupe key, which reopens the race window after a partial send. Keep the lock or model failure explicitly so delivery does not double-send.
