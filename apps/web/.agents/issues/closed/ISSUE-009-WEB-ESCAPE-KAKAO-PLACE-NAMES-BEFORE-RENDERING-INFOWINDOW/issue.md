# ISSUE-009-WEB-ESCAPE-KAKAO-PLACE-NAMES-BEFORE-RENDERING-INFOWINDOW

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:29.032Z
- Updated At: 2026-06-23T02:11:29.032Z
- Log: .agents/issues/logs/ISSUE-009-WEB-ESCAPE-KAKAO-PLACE-NAMES-BEFORE-RENDERING-INFOWINDOW.md
- Workflow State: .agents/issues/closed/ISSUE-009-WEB-ESCAPE-KAKAO-PLACE-NAMES-BEFORE-RENDERING-INFOWINDOW/workflow-state.json

## Goal
The Kakao map InfoWindow HTML is built with place.place_name inserted directly into markup. Because the value comes from an external API response, it should be escaped before rendering. Evidence: web/src/shared/lib/kakao-map.ts.

## Acceptance Criteria
- The issue "[web] Escape Kakao place names before rendering InfoWindow" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
The Kakao map InfoWindow HTML is built with place.place_name inserted directly into markup. Because the value comes from an external API response, it should be escaped before rendering. Evidence: web/src/shared/lib/kakao-map.ts.

## Decisions
- Decisions are stored under `decisions/`.
