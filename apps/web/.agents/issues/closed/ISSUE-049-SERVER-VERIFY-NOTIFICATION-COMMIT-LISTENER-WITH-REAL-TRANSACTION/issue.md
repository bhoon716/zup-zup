# ISSUE-049-SERVER-VERIFY-NOTIFICATION-COMMIT-LISTENER-WITH-REAL-TRANSACTION

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-07T01:50:32+09:00
- Updated At: 2026-07-07T01:55:02+09:00
- Completed At: 2026-07-07T01:55:02+09:00
- Log: .agents/issues/logs/ISSUE-049-SERVER-VERIFY-NOTIFICATION-COMMIT-LISTENER-WITH-REAL-TRANSACTION.md
- Workflow State: .agents/issues/closed/ISSUE-049-SERVER-VERIFY-NOTIFICATION-COMMIT-LISTENER-WITH-REAL-TRANSACTION/workflow-state.json

## Goal
Verify notification dispatch after commit with a real transaction manager instead of a resourceless test transaction so the regression covers production-like commit and rollback behavior.

## Acceptance Criteria
- The regression test uses a real transactional resource manager or JPA-backed transaction manager.
- Commit and rollback behavior are both covered.
- The test does not rely on a weak stand-in that bypasses transaction synchronization semantics.

## Dependencies
- Current notification event listener flow
- Test context support for transactional event synchronization

## Evidence Required
- Implementation diff
- Regression test output
- Review evidence

## Description
The current regression test proves the listener shape, but not the production transaction path. Strengthen it so commit/rollback behavior is exercised through a real transaction manager.
