# ISSUE-026-INFRA-CAP-DOCKER-JSON-LOGS-FOR-OBSERVABILITY-STACK-CONTAINERS

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-01T13:44:40.476Z
- Updated At: 2026-07-01T15:17:42.759Z
- Log: .agents/issues/logs/ISSUE-026-INFRA-CAP-DOCKER-JSON-LOGS-FOR-OBSERVABILITY-STACK-CONTAINERS.md
- Workflow State: .agents/issues/closed/ISSUE-026-INFRA-CAP-DOCKER-JSON-LOGS-FOR-OBSERVABILITY-STACK-CONTAINERS/workflow-state.json

## Goal
Goal: prevent Promtail, Loki, Grafana, and Prometheus container stdout logs from growing without bounds on the host root disk. Acceptance criteria: the observability stack services in docker-compose use explicit json-file rotation limits, the chosen max-size and max-file values are documented, and a restart or log-growth check confirms rotation is active. Dependencies: the disk-full postmortem analysis and the existing observability compose layout. Evidence required: compose diff or review evidence and verification output.

## Acceptance Criteria
- The issue "[infra] Cap Docker JSON logs for observability stack containers" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Goal: prevent Promtail, Loki, Grafana, and Prometheus container stdout logs from growing without bounds on the host root disk. Acceptance criteria: the observability stack services in docker-compose use explicit json-file rotation limits, the chosen max-size and max-file values are documented, and a restart or log-growth check confirms rotation is active. Dependencies: the disk-full postmortem analysis and the existing observability compose layout. Evidence required: compose diff or review evidence and verification output.

## Decisions
- Decisions are stored under `decisions/`.
