# ISSUE-016-LOGIN-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T04:11:09.432Z
- Updated At: 2026-07-01T13:53:09.009Z
- Log: .agents/issues/logs/ISSUE-016-LOGIN-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-016-LOGIN-PLAN/workflow-state.json
- Parent Issue: ISSUE-011-INFRA-LOG-STORAGE-PLAN

## Goal
Goal: make Promtail scrape the host log directory directly and remove the Docker socket dependency. Acceptance criteria: Promtail reads from /var/log/jbnu-sugang-helper/<service> or an equivalent host path, the compose file no longer mounts /var/run/docker.sock, and logs continue to flow to Loki after a restart. Dependencies: the application log path issue and the persistent storage issue. Evidence required: implementation or review evidence and verification output.

## Acceptance Criteria
- The issue "[infra] Scrape host log files in Promtail and drop Docker socket" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Goal: make Promtail scrape the host log directory directly and remove the Docker socket dependency. Acceptance criteria: Promtail reads from /var/log/jbnu-sugang-helper/<service> or an equivalent host path, the compose file no longer mounts /var/run/docker.sock, and logs continue to flow to Loki after a restart. Dependencies: the application log path issue and the persistent storage issue. Evidence required: implementation or review evidence and verification output.

## Decisions
- Decisions are stored under `decisions/`.
