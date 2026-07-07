# Repository Topology Decision

## Context
- The workspace contains three independent Git repositories: `server/`, `web/`, and `infra/`.
- The root directory is a coordination layer for project-level documentation and cross-repo planning.
- Earlier docs assumed a flatter single-repo layout and were no longer accurate.

## Decision
- Treat the project as a nested multi-repo workspace, not a monorepo.
- Keep service-specific implementation, dependencies, and commands inside their own repository.
- Keep durable cross-repo architecture and topology notes in the root `docs/` directory.

## Consequences
- Changes that touch backend, frontend, and infra should be split into repo-local commits when possible.
- Root documentation should describe shared architecture decisions and cross-cutting topology only.
- Repo-local READMEs remain the primary source for component-specific developer workflows.
