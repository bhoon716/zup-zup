# ISSUE-037-SERVER-ADD-DB-CONSTRAINTS-AND-CLEANUP-ORPHANED-USER-DATA

- 2026-07-06T02:46:59+09:00 Created from review finding: database invariants lack constraints and user-owned data can remain orphaned after account deletion.
- 2026-07-06T20:42:19+09:00 Added DB uniqueness migration, explicit user-owned-data cleanup in withdrawal, and verified targeted server tests.
