# ISSUE-030-SERVER-REMOVE-HARDCODED-PRODUCTION-SECRETS-FROM-APPLICATION-PROD

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-07-03T13:13:58+09:00
- Updated At: 2026-07-06T16:00:00+09:00
- Completed At: 2026-07-06T16:00:00+09:00
- Log: .agents/issues/logs/ISSUE-030-SERVER-REMOVE-HARDCODED-PRODUCTION-SECRETS-FROM-APPLICATION-PROD.md
- Workflow State: .agents/issues/closed/ISSUE-030-SERVER-REMOVE-HARDCODED-PRODUCTION-SECRETS-FROM-APPLICATION-PROD/workflow-state.json

## Goal
Remove hardcoded production secrets from `server/src/main/resources/application-prod.yml` and move them to environment variables or mounted secret files so the production config no longer contains sensitive credentials.

## Acceptance Criteria
- No production secret values remain committed in `application-prod.yml`.
- Production runtime reads secret values from environment variables or mounted secret sources.
- Verification evidence shows the secrets were removed and the application still resolves required config.

## Dependencies
- None.

## Evidence Required
- Config diff or grep output showing the secrets were removed.
- Verification output showing the app still boots or loads config with externalized values.

## Notes
- Keep the fix limited to production secret externalization and secret rotation support where needed.
