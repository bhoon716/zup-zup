# ISSUE-041-API-STANDARDIZE-RESPONSE-CONTRACTS-AND-PATH-ENCODING

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T21:10:04+09:00
- Completed At: 2026-07-06T21:10:04+09:00
- Log: .agents/issues/logs/ISSUE-041-API-STANDARDIZE-RESPONSE-CONTRACTS-AND-PATH-ENCODING.md
- Workflow State: .agents/issues/closed/ISSUE-041-API-STANDARDIZE-RESPONSE-CONTRACTS-AND-PATH-ENCODING/workflow-state.json

## Goal
Standardize API success and error response shapes across controllers and handlers, and fix frontend path variable encoding so courseKey and related identifiers are safe and consistent.

## Acceptance Criteria
- Success responses follow one documented envelope shape unless the endpoint intentionally returns no body.
- Security and runtime error responses share one documented error shape.
- Frontend path variables are encoded consistently before requests are sent.

## Dependencies
- API contract decision
- Controller and client regression tests

## Evidence Required
- Contract diff
- Controller/client test output
- Review evidence

## Description
The current API contracts are inconsistent across success and error paths, and path variables are not encoded uniformly at the frontend boundary. Standardize the contract surface so clients and controllers behave predictably.

## Decisions
- Decisions are stored under `decisions/`.
