# ISSUE-036-DOCS-UPDATE-PROJECT-ARCHITECTURE-DOCS-AND-REPOSITORY-TOPOLOGY

- Type: planning
- State: closed
- Workflow: mvp-planning
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T22:31:02+09:00
- Log: .agents/issues/logs/ISSUE-036-DOCS-UPDATE-PROJECT-ARCHITECTURE-DOCS-AND-REPOSITORY-TOPOLOGY.md
- Workflow State: .agents/issues/closed/ISSUE-036-DOCS-UPDATE-PROJECT-ARCHITECTURE-DOCS-AND-REPOSITORY-TOPOLOGY/workflow-state.json

## Goal
Update the project-level architecture docs so they describe the actual Spring Boot, Next.js, and nested repository layout instead of stale assumptions about the stack and topology.

## Acceptance Criteria
- Project docs describe the current backend, frontend, infrastructure, and nested repository layout accurately.
- Repository topology is documented with a clear, durable decision.
- Durable decisions are written to docs rather than only to chat or issue notes.

## Dependencies
- `docs/PRD.md`
- `docs/ARD.md`
- `docs/index.md`

## Evidence Required
- Updated documentation diff
- Review evidence
- Verification output or doc review notes

## Description
The existing project documentation does not describe the current system topology accurately enough for review, deployment, and maintenance work. Update the durable docs so they match the actual repository layout and stack.

## Decisions
- Decisions are stored under `decisions/`.
