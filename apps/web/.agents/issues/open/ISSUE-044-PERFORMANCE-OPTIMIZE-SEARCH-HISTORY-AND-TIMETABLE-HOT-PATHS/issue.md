# ISSUE-044-PERFORMANCE-OPTIMIZE-SEARCH-HISTORY-AND-TIMETABLE-HOT-PATHS

- Type: refactor
- State: open
- Workflow: refactoring
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T02:46:59+09:00
- Log: .agents/issues/logs/ISSUE-044-PERFORMANCE-OPTIMIZE-SEARCH-HISTORY-AND-TIMETABLE-HOT-PATHS.md
- Workflow State: .agents/issues/open/ISSUE-044-PERFORMANCE-OPTIMIZE-SEARCH-HISTORY-AND-TIMETABLE-HOT-PATHS/workflow-state.json

## Goal
Add the missing indexes and pagination for search and history paths, reduce timetable lazy-loading overhead, and extract query builders so the hot paths are easier to tune.

## Acceptance Criteria
- Search queries have the supporting indexes or query shape needed for the dominant access paths.
- History APIs are paginated and bounded for realistic retention sizes.
- Timetable detail avoids repeated lazy-load work where an eager projection or batching is warranted.

## Dependencies
- Current production query patterns
- Performance measurements or query plans

## Evidence Required
- Query plan or benchmark evidence
- Implementation diff
- Review evidence

## Description
The current search, history, and timetable paths have avoidable overhead from missing indexes, unbounded result sets, and complex query assembly. Tighten those paths so they remain predictable as data grows.

## Decisions
- Decisions are stored under `decisions/`.
