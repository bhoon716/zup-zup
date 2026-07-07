# ISSUE-005-INFRA-REMOVE-DOCKER-SOCKET-FROM-PROMTAIL-DEPLOYMENT

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:27.279Z
- Updated At: 2026-06-23T02:11:27.279Z
- Log: .agents/issues/logs/ISSUE-005-INFRA-REMOVE-DOCKER-SOCKET-FROM-PROMTAIL-DEPLOYMENT.md
- Workflow State: .agents/issues/closed/ISSUE-005-INFRA-REMOVE-DOCKER-SOCKET-FROM-PROMTAIL-DEPLOYMENT/workflow-state.json

## Goal
promtail mounts /var/run/docker.sock, which grants host-level Docker control if the container is compromised. This is a high-risk deployment smell and should be replaced with a less privileged log collection path. Evidence: infra/docker-compose.yml.

## Acceptance Criteria
- The issue "[infra] Remove Docker socket from promtail deployment" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
promtail mounts /var/run/docker.sock, which grants host-level Docker control if the container is compromised. This is a high-risk deployment smell and should be replaced with a less privileged log collection path. Evidence: infra/docker-compose.yml.

## Decisions
- Decisions are stored under `decisions/`.
