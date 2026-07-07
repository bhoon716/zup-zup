# ISSUE-047-CI-UPGRADE-GITHUB-ACTIONS-FOR-NODE-24-RUNNERS

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-06T16:40:30+09:00
- Updated At: 2026-07-06T18:04:00+09:00
- Completed At: 2026-07-06T18:04:00+09:00
- Log: .agents/issues/logs/ISSUE-047-CI-UPGRADE-GITHUB-ACTIONS-FOR-NODE-24-RUNNERS.md
- Workflow State: .agents/issues/closed/ISSUE-047-CI-UPGRADE-GITHUB-ACTIONS-FOR-NODE-24-RUNNERS/workflow-state.json

## Goal
Update the GitHub Actions workflow so it no longer relies on actions that trigger the Node.js 20 deprecation warning on current GitHub-hosted runners.

## Acceptance Criteria
- The deploy workflow runs without the Node.js 20 deprecation warning for `actions/checkout@v4` and `actions/setup-java@v4`.
- Workflow action versions are confirmed compatible with the current runner runtime.
- CI still completes successfully after the workflow update.

## Dependencies
- Current GitHub Actions workflow
- GitHub-hosted runner compatibility

## Evidence Required
- Workflow diff
- CI run output without the Node.js 20 deprecation warning

## Description
GitHub Actions currently emits a warning that `actions/checkout@v4` and `actions/setup-java@v4` target Node.js 20 while the runner forces Node.js 24. Update the workflow actions so the deploy pipeline stays forward-compatible and the warning is removed.

## Decisions
- Decisions are stored under `decisions/`.
