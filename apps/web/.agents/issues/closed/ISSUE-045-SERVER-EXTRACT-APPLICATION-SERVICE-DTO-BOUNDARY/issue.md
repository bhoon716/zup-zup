# ISSUE-045-SERVER-EXTRACT-APPLICATION-SERVICE-DTO-BOUNDARY

- Type: refactor
- State: closed
- Workflow: refactoring
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T22:31:02+09:00
- Log: .agents/issues/logs/ISSUE-045-SERVER-EXTRACT-APPLICATION-SERVICE-DTO-BOUNDARY.md
- Workflow State: .agents/issues/closed/ISSUE-045-SERVER-EXTRACT-APPLICATION-SERVICE-DTO-BOUNDARY/workflow-state.json

## Goal
Introduce an application command/result boundary so application services no longer depend directly on presentation DTOs.

## Acceptance Criteria
- Application services stop importing presentation-layer DTOs.
- Command/result or equivalent application types are introduced where needed.
- The refactor is behavior-preserving and covered by targeted tests.

## Dependencies
- Current service layer structure
- Existing application and presentation tests

## Evidence Required
- Implementation diff
- Targeted test output
- Review evidence

## Description
The backend application layer reaches upward into presentation DTOs, which makes the boundary hard to change safely. Add an explicit application-layer contract so services stay decoupled from HTTP concerns.

## Decisions
- Decisions are stored under `decisions/`.
