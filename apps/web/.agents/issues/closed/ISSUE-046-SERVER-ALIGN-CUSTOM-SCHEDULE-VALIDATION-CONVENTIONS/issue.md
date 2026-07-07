# ISSUE-046-SERVER-ALIGN-CUSTOM-SCHEDULE-VALIDATION-CONVENTIONS

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T20:51:33+09:00
- Completed At: 2026-07-06T20:51:33+09:00
- Log: .agents/issues/logs/ISSUE-046-SERVER-ALIGN-CUSTOM-SCHEDULE-VALIDATION-CONVENTIONS.md
- Workflow State: .agents/issues/closed/ISSUE-046-SERVER-ALIGN-CUSTOM-SCHEDULE-VALIDATION-CONVENTIONS/workflow-state.json

## Goal
Align custom schedule validation messages and annotations, and remove hidden write behavior from crawler-setting reads so local conventions match runtime behavior.

## Acceptance Criteria
- Custom schedule validation annotations and messages agree with the actual rule enforced.
- GET-style crawler setting reads do not create or mutate database rows implicitly.
- The resulting behavior is covered by targeted tests.

## Dependencies
- Current validation annotations
- Crawler setting read/write path

## Evidence Required
- Implementation diff
- Targeted test output
- Review evidence

## Description
A small but real conventions drift remains in custom schedule validation and crawler setting reads. Align the annotations, messages, and side effects so the code says what it does.

## Decisions
- Decisions are stored under `decisions/`.
