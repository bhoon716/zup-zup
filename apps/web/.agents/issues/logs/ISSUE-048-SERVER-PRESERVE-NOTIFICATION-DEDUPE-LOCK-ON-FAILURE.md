# ISSUE-048-SERVER-PRESERVE-NOTIFICATION-DEDUPE-LOCK-ON-FAILURE

- 2026-07-07T01:50:32+09:00 Created from review finding: delivery failure currently deletes the dedupe key, which can re-open double-send risk after a partial send.
- 2026-07-07T01:55:02+09:00 Implemented by preserving the dedupe lock on failure and covering the failure path with a regression test.
