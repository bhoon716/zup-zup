# ISSUE-004-LOGIN-PLAN

- Type: bugfix
- State: closed
- Workflow: bug-fix
- Created At: 2026-06-23T02:11:26.910Z
- Updated At: 2026-06-23T02:11:26.910Z
- Log: .agents/issues/logs/ISSUE-004-LOGIN-PLAN.md
- Workflow State: .agents/issues/closed/ISSUE-004-LOGIN-PLAN/workflow-state.json

## Goal
The refresh token cookie is emitted with secure(false), and the deployment still exposes the app over plain HTTP paths. This leaves authentication material vulnerable to interception and makes HTTPS enforcement inconsistent across environments. Evidence: server/src/main/java/bhoon/sugang_helper/auth/application/AuthService.java, server/docker-compose.yml, infra/npm/data/nginx/proxy_host/1.conf, infra/npm/data/nginx/proxy_host/2.conf.

## Acceptance Criteria
- The issue "[server/infra] Enforce secure auth cookies and HTTPS-only delivery" is resolved.
- Verification evidence is recorded.

## Dependencies
No dependencies provided.

## Evidence Required
- Implementation or review evidence
- Verification output

## Description
The refresh token cookie is emitted with secure(false), and the deployment still exposes the app over plain HTTP paths. This leaves authentication material vulnerable to interception and makes HTTPS enforcement inconsistent across environments. Evidence: server/src/main/java/bhoon/sugang_helper/auth/application/AuthService.java, server/docker-compose.yml, infra/npm/data/nginx/proxy_host/1.conf, infra/npm/data/nginx/proxy_host/2.conf.

## Decisions
- Decisions are stored under `decisions/`.
