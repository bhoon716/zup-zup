# ISSUE-035-WEB-SPLIT-LARGE-ROUTE-PAGES-INTO-HOOKS-AND-SECTIONS

- Type: refactor
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T21:25:28+09:00
- Completed At: 2026-07-06T21:25:28+09:00
- Log: .agents/issues/logs/ISSUE-035-WEB-SPLIT-LARGE-ROUTE-PAGES-INTO-HOOKS-AND-SECTIONS.md
- Workflow State: .agents/issues/closed/ISSUE-035-WEB-SPLIT-LARGE-ROUTE-PAGES-INTO-HOOKS-AND-SECTIONS/workflow-state.json

## Goal
Split the large Settings and Timetable route pages into smaller hooks and section components so orchestration logic is easier to maintain without changing behavior.

## Acceptance Criteria
- Settings and Timetable route pages are decomposed into smaller hooks and section components.
- User-facing behavior remains unchanged and is covered by focused regression tests where needed.
- No new lint, type, or build regressions are introduced in the touched frontend surfaces.

## Dependencies
- Existing frontend test coverage

## Evidence Required
- Implementation diff
- Targeted test output
- Review evidence

## Description
The Settings and Timetable route pages currently concentrate too much orchestration in a few route components. Extract data loading, state handling, and section rendering into smaller units to reduce review and change risk.

## Decisions
- Decisions are stored under `decisions/`.
