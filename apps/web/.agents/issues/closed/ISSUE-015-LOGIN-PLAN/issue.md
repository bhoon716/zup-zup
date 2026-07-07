# ISSUE-015-LOGIN-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T04:10:40.321Z
- Updated At: 2026-07-01T13:58:19.660Z
- Log: .agents/issues/logs/ISSUE-015-LOGIN-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-015-LOGIN-PLAN/workflow-state.json
- Parent Issue: ISSUE-011-INFRA-LOG-STORAGE-PLAN

## Goal
Goal: enforce a 30-day local retention policy and back up log state to a host-local backup path. Acceptance criteria: application logs rotate and compress, Loki retention matches 30 days, the backup job copies the required data to the chosen local backup path, and a restore drill succeeds. Dependencies: the persistent storage issue and the application log path issue. Evidence required: implementation or review evidence and verification output.

## Acceptance Criteria
- The issue "[infra] Add log rotation, 30-day retention, and local backups" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Goal: enforce a 30-day local retention policy and back up log state to a host-local backup path. Acceptance criteria: application logs rotate and compress, Loki retention matches 30 days, the backup job copies the required data to the chosen local backup path, and a restore drill succeeds. Dependencies: the persistent storage issue and the application log path issue. Evidence required: implementation or review evidence and verification output.

## Decisions
- Decisions are stored under `decisions/`.
