# ISSUE-037-SERVER-ADD-DB-CONSTRAINTS-AND-CLEANUP-ORPHANED-USER-DATA

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T20:42:19+09:00
- Completed At: 2026-07-06T20:42:19+09:00
- Log: .agents/issues/logs/ISSUE-037-SERVER-ADD-DB-CONSTRAINTS-AND-CLEANUP-ORPHANED-USER-DATA.md
- Workflow State: .agents/issues/closed/ISSUE-037-SERVER-ADD-DB-CONSTRAINTS-AND-CLEANUP-ORPHANED-USER-DATA/workflow-state.json

## Goal
Add database uniqueness constraints for subscriptions, devices, reviews, and primary timetables, and define a safe cleanup policy so user-owned data does not remain orphaned after account deletion.

## Acceptance Criteria
- Database constraints prevent duplicate subscriptions, devices, reviews, and multiple primary timetables per user.
- User withdrawal leaves no unintended orphaned user-owned records, or the cleanup policy is explicitly documented and tested.
- Constraint changes are covered by migration or integration verification.

## Dependencies
- Duplicate cleanup migration plan
- Current schema migration strategy

## Evidence Required
- Migration diff
- Integration or repository test output
- Review evidence

## Description
Several business invariants are only enforced in application logic today. Add database-level uniqueness and cleanup rules so concurrency and deletion paths cannot create duplicate or orphaned data.

## Decisions
- Decisions are stored under `decisions/`.
