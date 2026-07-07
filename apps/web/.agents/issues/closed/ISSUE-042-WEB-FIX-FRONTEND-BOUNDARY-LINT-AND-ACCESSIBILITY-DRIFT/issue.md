# ISSUE-042-WEB-FIX-FRONTEND-BOUNDARY-LINT-AND-ACCESSIBILITY-DRIFT

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T17:58:43+09:00
- Completed At: 2026-07-06T17:58:43+09:00
- Log: .agents/issues/logs/ISSUE-042-WEB-FIX-FRONTEND-BOUNDARY-LINT-AND-ACCESSIBILITY-DRIFT.md
- Workflow State: .agents/issues/closed/ISSUE-042-WEB-FIX-FRONTEND-BOUNDARY-LINT-AND-ACCESSIBILITY-DRIFT/workflow-state.json

## Goal
Fix the frontend shared-to-feature dependency violation, remove lint failures from the touched surfaces, and correct accessibility or edit-state drift on interactive controls.

## Acceptance Criteria
- Shared code no longer imports feature state or otherwise violates the declared frontend dependency direction.
- The current lint failures on the affected frontend surfaces are resolved.
- Interactive controls and edit states have correct accessible names and semantics.

## Dependencies
- Frontend boundary rules
- Current lint configuration

## Evidence Required
- Lint output
- Targeted frontend test output
- Review evidence

## Description
The frontend currently violates its own dependency boundary, has red lint output, and shows accessibility/edit-state drift on some controls. Fix the boundary and the quality gates together so the web layer stays maintainable.

## Decisions
- Decisions are stored under `decisions/`.
