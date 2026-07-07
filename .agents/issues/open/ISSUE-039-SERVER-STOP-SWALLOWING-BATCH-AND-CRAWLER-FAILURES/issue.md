# ISSUE-039-SERVER-STOP-SWALLOWING-BATCH-AND-CRAWLER-FAILURES

- Type: bugfix
- State: open
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T02:46:59+09:00
- Log: .agents/issues/logs/ISSUE-039-SERVER-STOP-SWALLOWING-BATCH-AND-CRAWLER-FAILURES.md
- Workflow State: .agents/issues/open/ISSUE-039-SERVER-STOP-SWALLOWING-BATCH-AND-CRAWLER-FAILURES/workflow-state.json

## Goal
Make batch item failures visible or explicitly skipped, batch crawler reads and writes more efficiently, and remove hidden write side effects from settings reads.

## Acceptance Criteria
- Batch item failures are surfaced, recorded, or intentionally skipped with explicit policy.
- Crawler processing avoids per-row read/write chatter where batching is practical.
- Settings reads do not silently create or mutate database rows unless that behavior is explicitly modeled.

## Dependencies
- Batch failure policy
- Crawler write path

## Evidence Required
- Implementation diff
- Representative batch or crawler test output
- Review evidence

## Description
The batch and crawler flows hide too much failure behavior and do unnecessary per-row work. Make failures explicit, batch the hot path where possible, and remove implicit writes from reads.

## Decisions
- Decisions are stored under `decisions/`.
