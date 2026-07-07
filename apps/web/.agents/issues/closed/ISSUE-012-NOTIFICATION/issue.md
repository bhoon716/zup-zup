# ISSUE-012-NOTIFICATION

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T03:55:23.371Z
- Updated At: 2026-06-23T03:55:23.371Z
- Log: .agents/issues/logs/ISSUE-012-NOTIFICATION.md
- Workflow State: .agents/issues/closed/ISSUE-012-NOTIFICATION/workflow-state.json

## Goal
Notification history and subscription views render literal \n escape sequences in the message body instead of line breaks. The current seat-opened notification body is composed with escaped newline characters in server/src/main/java/bhoon/sugang_helper/notification/application/NotificationService.java, and the web notification card renders the message text directly with whitespace-pre-wrap. Fix the notification payload formatting and/or rendering so users see proper multiline text instead of '\n'. Acceptance criteria: notification history shows readable line breaks for seat-opened messages, recent notifications remain readable, and no literal backslash-n strings appear in the UI.

## Acceptance Criteria
- The issue "[server/web] Notification history shows literal newline escape sequences" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
Notification history and subscription views render literal \n escape sequences in the message body instead of line breaks. The current seat-opened notification body is composed with escaped newline characters in server/src/main/java/bhoon/sugang_helper/notification/application/NotificationService.java, and the web notification card renders the message text directly with whitespace-pre-wrap. Fix the notification payload formatting and/or rendering so users see proper multiline text instead of '\n'. Acceptance criteria: notification history shows readable line breaks for seat-opened messages, recent notifications remain readable, and no literal backslash-n strings appear in the UI.

## Decisions
- Decisions are stored under `decisions/`.
