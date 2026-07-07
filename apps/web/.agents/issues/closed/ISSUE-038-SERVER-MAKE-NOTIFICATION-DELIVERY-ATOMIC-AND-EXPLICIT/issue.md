# ISSUE-038-SERVER-MAKE-NOTIFICATION-DELIVERY-ATOMIC-AND-EXPLICIT

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-07T01:42:20+09:00
- Completed At: 2026-07-07T01:42:20+09:00
- Log: .agents/issues/logs/ISSUE-038-SERVER-MAKE-NOTIFICATION-DELIVERY-ATOMIC-AND-EXPLICIT.md
- Workflow State: .agents/issues/closed/ISSUE-038-SERVER-MAKE-NOTIFICATION-DELIVERY-ATOMIC-AND-EXPLICIT/workflow-state.json

## Goal
Make notification deduplication atomic, move dispatch after transaction commit, and extract channel policy so notification delivery cannot double-send or emit false alerts.

## Acceptance Criteria
- Notification dedupe is atomic under concurrent requests.
- Notification dispatch happens only after commit or through an equivalent outbox-style guarantee.
- Channel policy is split into testable handlers or otherwise documented explicitly.

## Dependencies
- Current notification delivery flow
- Transaction/event dispatch strategy

## Evidence Required
- Implementation diff
- Concurrency or integration test output
- Review evidence

## Description
Notification delivery currently has a race window and mixes policy with dispatch. Make the dedupe and publish path atomic, then separate channel decisions so the behavior is testable and predictable.

## Decisions
- Decisions are stored under `decisions/`.
