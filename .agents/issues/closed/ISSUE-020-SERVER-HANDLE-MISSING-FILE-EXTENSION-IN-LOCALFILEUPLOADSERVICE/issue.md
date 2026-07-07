# ISSUE-020-SERVER-HANDLE-MISSING-FILE-EXTENSION-IN-LOCALFILEUPLOADSERVICE

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T07:59:17.007Z
- Updated At: 2026-06-23T07:59:17.007Z
- Log: .agents/issues/logs/ISSUE-020-SERVER-HANDLE-MISSING-FILE-EXTENSION-IN-LOCALFILEUPLOADSERVICE.md
- Workflow State: .agents/issues/closed/ISSUE-020-SERVER-HANDLE-MISSING-FILE-EXTENSION-IN-LOCALFILEUPLOADSERVICE/workflow-state.json

## Goal
파일 업로드 시 파일명에 확장자가 없는 경우(MIME 타입은 유효하더라도) 확장자 없이 UUID명으로만 저장됩니다. 이 경우 클라이언트 서빙 시 브라우저가 콘텐츠 타입을 올바르게 판별하지 못할 수 있으므로, MIME 타입에 맞춰 적절한 확장자(예: .png, .jpg)를 부여하는 폴백 처리가 필요합니다.

## Acceptance Criteria
- The issue "[server] Handle missing file extension in LocalFileUploadService" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
파일 업로드 시 파일명에 확장자가 없는 경우(MIME 타입은 유효하더라도) 확장자 없이 UUID명으로만 저장됩니다. 이 경우 클라이언트 서빙 시 브라우저가 콘텐츠 타입을 올바르게 판별하지 못할 수 있으므로, MIME 타입에 맞춰 적절한 확장자(예: .png, .jpg)를 부여하는 폴백 처리가 필요합니다.

## Decisions
- Decisions are stored under `decisions/`.
