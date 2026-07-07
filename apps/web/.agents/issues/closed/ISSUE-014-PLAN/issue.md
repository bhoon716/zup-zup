# ISSUE-014-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T04:10:40.308Z
- Updated At: 2026-07-01T13:53:08.709Z
- Log: .agents/issues/logs/ISSUE-014-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-014-PLAN/workflow-state.json
- Parent Issue: ISSUE-011-INFRA-LOG-STORAGE-PLAN

## Goal
Goal: mount durable host storage for Loki and Promtail so their data and positions survive container recreation. Acceptance criteria: Loki uses a host-backed data path instead of /tmp, Promtail positions are stored under /var/lib/promtail, and a restart/recreate check confirms the state persists. Dependencies: the reviewed log storage plan and the application log path issue. Evidence required: implementation or review evidence and verification output.

## Acceptance Criteria
- The issue "[infra] Persist Loki and Promtail state on host storage" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Goal: mount durable host storage for Loki and Promtail so their data and positions survive container recreation. Acceptance criteria: Loki uses a host-backed data path instead of /tmp, Promtail positions are stored under /var/lib/promtail, and a restart/recreate check confirms the state persists. Dependencies: the reviewed log storage plan and the application log path issue. Evidence required: implementation or review evidence and verification output.

## Decisions
- Decisions are stored under `decisions/`.
