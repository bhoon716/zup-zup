# ISSUE-025-INFRA-SHORTEN-MYSQL-BINLOG-RETENTION-TO-2-DAYS

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-01T13:44:33.976Z
- Updated At: 2026-07-01T15:17:42.696Z
- Log: .agents/issues/logs/ISSUE-025-INFRA-SHORTEN-MYSQL-BINLOG-RETENTION-TO-2-DAYS.md
- Workflow State: .agents/issues/closed/ISSUE-025-INFRA-SHORTEN-MYSQL-BINLOG-RETENTION-TO-2-DAYS/workflow-state.json

## Goal
Goal: bound MySQL binlog growth so the database can still support recent incident tracing without filling the root disk again. Acceptance criteria: MySQL binlog auto-expiration is set to 2 days, older binlogs are purged automatically, and a restart or verification check confirms the retention policy is active. Dependencies: the disk-full postmortem analysis and the current MySQL deployment configuration. Evidence required: configuration diff or review evidence and verification output.

## Acceptance Criteria
- The issue "[infra] Shorten MySQL binlog retention to 2 days" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Goal: bound MySQL binlog growth so the database can still support recent incident tracing without filling the root disk again. Acceptance criteria: MySQL binlog auto-expiration is set to 2 days, older binlogs are purged automatically, and a restart or verification check confirms the retention policy is active. Dependencies: the disk-full postmortem analysis and the current MySQL deployment configuration. Evidence required: configuration diff or review evidence and verification output.

## Decisions
- Decisions are stored under `decisions/`.
