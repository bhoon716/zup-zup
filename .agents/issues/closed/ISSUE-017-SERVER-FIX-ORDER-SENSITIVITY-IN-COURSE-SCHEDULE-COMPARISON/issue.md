# ISSUE-017-SERVER-FIX-ORDER-SENSITIVITY-IN-COURSE-SCHEDULE-COMPARISON

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T07:59:11.062Z
- Updated At: 2026-06-23T07:59:11.062Z
- Log: .agents/issues/logs/ISSUE-017-SERVER-FIX-ORDER-SENSITIVITY-IN-COURSE-SCHEDULE-COMPARISON.md
- Workflow State: .agents/issues/closed/ISSUE-017-SERVER-FIX-ORDER-SENSITIVITY-IN-COURSE-SCHEDULE-COMPARISON/workflow-state.json

## Goal
Course.java의 isSameSchedules()에서 데이터베이스에서 조회한 this.schedules와 크롤링 결과인 otherSchedules를 인덱스 기준으로 비교하고 있습니다. this.schedules는 정렬 순서가 보장되지 않아 데이터베이스 반환 순서에 따라 매번 변경 여부가 참(false)으로 판정되어 불필요한 DELETE/INSERT 쿼리가 발생할 수 있습니다.

## Acceptance Criteria
- The issue "[server] Fix order-sensitivity in Course schedule comparison" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Course.java의 isSameSchedules()에서 데이터베이스에서 조회한 this.schedules와 크롤링 결과인 otherSchedules를 인덱스 기준으로 비교하고 있습니다. this.schedules는 정렬 순서가 보장되지 않아 데이터베이스 반환 순서에 따라 매번 변경 여부가 참(false)으로 판정되어 불필요한 DELETE/INSERT 쿼리가 발생할 수 있습니다.

## Decisions
- Decisions are stored under `decisions/`.
