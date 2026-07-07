# ISSUE-030-SERVER-REMOVE-HARDCODED-PRODUCTION-SECRETS-FROM-APPLICATION-PROD

- 2026-07-03T13:13:58+09:00 Created from review finding: production secrets are hardcoded in `application-prod.yml`.
- 2026-07-06T16:00:00+09:00 Backed up current prod config files under `.agents/backups/issue-030/`.
- 2026-07-06T16:00:00+09:00 Externalized prod secrets in `server/src/main/resources/application-prod.yml`, added server-side `.env` loading in `server/docker-compose.yml`, and changed `server/.github/workflows/cd.yml` to write `.env` from GitHub Secrets at deploy time.
- 2026-07-06T16:00:00+09:00 Consolidated deployment configuration into a single `SERVER_DOTENV` secret that is rendered once into `.env` and copied to the server.
- 2026-07-06T16:00:00+09:00 Verified with `./gradlew test --tests bhoon.sugang_helper.common.config.ProductionConfigTemplateTest` and `./gradlew test`.
- 2026-07-06T16:00:00+09:00 `rg` check confirmed no hardcoded prod secret literals remain in `application-prod.yml`, `cd.yml`, or `docker-compose.yml`.
- 2026-07-06T16:00:00+09:00 Closed after verifying the prod secrets were externalized and the deployment path reads them from `.env` at deploy time.
