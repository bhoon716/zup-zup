# ISSUE-011-INFRA-LOG-STORAGE-PLAN

- Type: planning
- State: closed
- Workflow: mvp-planning
- Created At: 2026-06-23T03:49:28.413Z
- Updated At: 2026-06-23T04:18:00.000Z
- Log: .agents/issues/logs/ISSUE-011-INFRA-LOG-STORAGE-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-011-INFRA-LOG-STORAGE-PLAN/workflow-state.json
- Child Issues: ISSUE-013-SERVER-WEB-WRITE-APPLICATION-LOGS-TO-HOST-FILES, ISSUE-014-PLAN, ISSUE-015-LOGIN-PLAN, ISSUE-016-LOGIN-PLAN

## Goal
Design a self-hosted log retention and storage plan for the server without AWS or external IaaS. The plan should define where application logs live on the host, how Loki/Promtail state persists across container recreation, how rotation and retention are handled, and how backups are performed to a local or secondary disk. Evidence reviewed so far shows the current Loki and Promtail configs use /tmp, Docker json-file logging is only a transport layer, and promtail currently mounts the Docker socket. Target paths should follow Unix conventions: application logs under /var/log/<service>, Loki/Promtail state under /var/lib/<service>, or another clearly justified host path. Acceptance criteria should include a concrete directory layout, compose volume mapping plan, retention policy, backup strategy, and a migration checklist.

## Acceptance Criteria
- The issue "[infra] Log storage plan for self-hosted server" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Design a self-hosted log retention and storage plan for the server without AWS or external IaaS. The plan should define where application logs live on the host, how Loki/Promtail state persists across container recreation, how rotation and retention are handled, and how backups are performed to a local or secondary disk. Evidence reviewed so far shows the current Loki and Promtail configs use /tmp, Docker json-file logging is only a transport layer, and promtail currently mounts the Docker socket. Target paths should follow Unix conventions: application logs under /var/log/<service>, Loki/Promtail state under /var/lib/<service>, or another clearly justified host path. Acceptance criteria should include a concrete directory layout, compose volume mapping plan, retention policy, backup strategy, and a migration checklist.

## Decisions
- Decisions are stored under `decisions/`.
