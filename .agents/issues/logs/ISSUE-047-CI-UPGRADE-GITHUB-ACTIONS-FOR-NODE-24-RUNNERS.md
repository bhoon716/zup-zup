# ISSUE-047-CI-UPGRADE-GITHUB-ACTIONS-FOR-NODE-24-RUNNERS

- 2026-07-06T16:40:30+09:00 Created from GitHub Actions warning: `actions/checkout@v4` and `actions/setup-java@v4` are being forced to run on Node.js 24 while targeting Node.js 20.
- 2026-07-06T18:04:00+09:00 Updated `server/.github/workflows/cd.yml` to `actions/checkout@v5` and `actions/setup-java@v5`, which are documented as Node 24-compatible.
- 2026-07-06T18:04:00+09:00 Verified the workflow diff locally; no additional server tests were required because this change only updates GitHub Actions metadata.
