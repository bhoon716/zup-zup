# ISSUE-049-SERVER-VERIFY-NOTIFICATION-COMMIT-LISTENER-WITH-REAL-TRANSACTION

- 2026-07-07T01:50:32+09:00 Created from review finding: the current regression test uses a resourceless transaction manager and does not prove production-like commit/rollback behavior.
- 2026-07-07T01:55:02+09:00 Implemented with a JPA-backed transaction manager in a data slice test to exercise real commit and rollback behavior.
