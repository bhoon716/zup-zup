# ISSUE-018-SERVER-ENHANCE-SESSION-COOKIE-FETCH-ROBUSTNESS-IN-JBNUCOURSEAPICLIENT

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T07:59:13.297Z
- Updated At: 2026-06-23T07:59:13.297Z
- Log: .agents/issues/logs/ISSUE-018-SERVER-ENHANCE-SESSION-COOKIE-FETCH-ROBUSTNESS-IN-JBNUCOURSEAPICLIENT.md
- Workflow State: .agents/issues/closed/ISSUE-018-SERVER-ENHANCE-SESSION-COOKIE-FETCH-ROBUSTNESS-IN-JBNUCOURSEAPICLIENT/workflow-state.json

## Goal
JbnuCourseApiClient.fetchSessionCookies()에서 쿠키 획득 실패 시 빈 Map을 반환하고 있습니다. 이로 인해 fetchCourseDataXml() 호출 시 빈 쿠키 값으로 잘못된 API 요청을 보내고 에러를 발생시킨 후 재시도하게 됩니다. 쿠키 획득 실패 시 바로 예외를 던지거나 재시도하도록 개선해야 합니다.

## Acceptance Criteria
- The issue "[server] Enhance session cookie fetch robustness in JbnuCourseApiClient" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
JbnuCourseApiClient.fetchSessionCookies()에서 쿠키 획득 실패 시 빈 Map을 반환하고 있습니다. 이로 인해 fetchCourseDataXml() 호출 시 빈 쿠키 값으로 잘못된 API 요청을 보내고 에러를 발생시킨 후 재시도하게 됩니다. 쿠키 획득 실패 시 바로 예외를 던지거나 재시도하도록 개선해야 합니다.

## Decisions
- Decisions are stored under `decisions/`.
