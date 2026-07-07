# ISSUE-027-SEARCH

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-01T17:36:45.575Z
- Updated At: 2026-07-01T17:40:11.634Z
- Log: .agents/issues/logs/ISSUE-027-SEARCH.md
- Workflow State: .agents/issues/closed/ISSUE-027-SEARCH/workflow-state.json

## Goal
강의 검색에서 2학기가 기본인 상태에서 1학기로 검색한 후, 강의 상세 옵션(필터)을 선택했다가 X 버튼을 눌러 해제하면, 사용자가 설정한 1학기가 아닌 기본 학기인 2학기로 검색 조건이 리셋되는 버그가 발생합니다. resolveVisibleSearchCondition 함수에서 base key만 남았을 때 fallback condition과 비교하는 로직으로 인해 기본 학기(2학기)로 교체되는 현상이 원인입니다.

## Acceptance Criteria
- The issue "Fix course search semester resetting to default when clearing detailed filter option" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
강의 검색에서 2학기가 기본인 상태에서 1학기로 검색한 후, 강의 상세 옵션(필터)을 선택했다가 X 버튼을 눌러 해제하면, 사용자가 설정한 1학기가 아닌 기본 학기인 2학기로 검색 조건이 리셋되는 버그가 발생합니다. resolveVisibleSearchCondition 함수에서 base key만 남았을 때 fallback condition과 비교하는 로직으로 인해 기본 학기(2학기)로 교체되는 현상이 원인입니다.

## Decisions
- Decisions are stored under `decisions/`.
