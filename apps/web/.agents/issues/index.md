# Issues

Local issue tracking lives under `.agents/issues/`.

## State Model

- `open`: created, not yet started
- `in-progress`: actively being worked
- `blocked`: work is paused on a dependency or unresolved question
- `closed`: finished

## Execution Rules

- Start from the issue goal and acceptance criteria.
- Prefer the smallest change that satisfies the request.
- Ask or record assumptions when the request is ambiguous.
- Do not add unrelated refactors, abstractions, or cleanup.
- Verify the result with targeted evidence before closing.

## Layout

- `.agents/issues/open/ISSUE-.../`
- `.agents/issues/in-progress/ISSUE-.../`
- `.agents/issues/blocked/ISSUE-.../`
- `.agents/issues/closed/ISSUE-.../`
- `.agents/issues/logs/ISSUE-....md`

## Issue Folder Contents

- `issue.md`: human-readable summary
- `issue.json`: machine-readable metadata
- `decisions/`: short decision records when needed
- `reviews/`: review notes when needed

## Log Rules

- Logs are append-only.
- Each issue gets one log file under `.agents/issues/logs/`.
- Keep each log entry short and evidence-backed.

## Migration Notes

- Imported historical issue records live under `.agents/issues/closed/`.
