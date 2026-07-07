# ISSUE-013-SERVER-WEB-WRITE-APPLICATION-LOGS-TO-HOST-FILES

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T04:10:29.450Z
- Updated At: 2026-06-23T04:43:53.276Z
- Log: .agents/issues/logs/ISSUE-013-SERVER-WEB-WRITE-APPLICATION-LOGS-TO-HOST-FILES.md
- Workflow State: .agents/issues/closed/ISSUE-013-SERVER-WEB-WRITE-APPLICATION-LOGS-TO-HOST-FILES/workflow-state.json
- Parent Issue: ISSUE-011-INFRA-LOG-STORAGE-PLAN

## Goal
Goal: route server and web application logs to host files under /var/log/jbnu-sugang-helper/<service> so Promtail can scrape them without Docker metadata. Acceptance criteria: application logs are written to stable host paths, the paths are documented, and verification evidence shows the files are emitted as expected. Dependencies: the logging path and compose volume plan from ISSUE-011-INFRA-LOG-STORAGE-PLAN. Evidence required: implementation or review evidence and verification output.

## Acceptance Criteria
- The issue "[server/web] Write application logs to host files" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Goal: route server and web application logs to host files under /var/log/jbnu-sugang-helper/<service> so Promtail can scrape them without Docker metadata. Acceptance criteria: application logs are written to stable host paths, the paths are documented, and verification evidence shows the files are emitted as expected. Dependencies: the logging path and compose volume plan from ISSUE-011-INFRA-LOG-STORAGE-PLAN. Evidence required: implementation or review evidence and verification output.

## Decisions
- Decisions are stored under `decisions/`.
