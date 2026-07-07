# Database Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `DB-001` Critical: Fresh Flyway migration is not validated and mixes H2/MySQL statements.
- `DB-002` High: V6 drops review data and V7 recreates empty tables.
- `DB-003` High: `baseline-version: 3` can skip required migrations.
- `DATA-001` High: Business invariants lack database constraints.
- `DATA-002` High: User-owned data can remain orphaned after account deletion.
- `PERF-001` Medium: Course search lacks supporting indexes.
- `PERF-003` Medium: History APIs are unbounded and under-indexed.

## Data Integrity Rules To Add

- `subscriptions`: unique active identity for `(user_id, course_key)`.
- `user_devices`: unique token or `(type, token)`.
- `course_reviews`: unique review scope per user.
- `timetables`: one primary timetable per user.
- User-owned tables: explicit FK/cascade or documented cleanup policy.

## Migration Safety Rule

Do not edit already-applied migrations casually. Use forward-only corrective migrations unless all environments can be coordinated with Flyway repair and checksum control.
