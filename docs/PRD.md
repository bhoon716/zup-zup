# PRD

Product requirements document for the current project.

## Summary
Capture the topic, target users, main problem, core features, non-goals, acceptance criteria, and open questions before implementation starts.

## Product Topic / Users / Problem
- Product topic: Self-hosted server log retention and storage for JBNU Sugang Helper.
- Target users: Operators and maintainers of the self-hosted deployment.
- Main problem: Application logs, Loki state, and Promtail positions currently rely on ephemeral or loosely defined paths, which makes log retention fragile across container recreation and complicates backup and recovery.

## Core Features / Non-goals
- Core features:
  - Define a durable host directory layout for application logs and observability state.
  - Map compose volumes so Loki and Promtail persist under host-owned storage.
  - Specify rotation, retention, and backup behavior for local storage.
  - Provide a migration checklist from the current `/tmp`-backed configuration.
- Non-goals:
  - Moving logs to AWS or any external IaaS.
  - Introducing a distributed logging backend or multi-node cluster design.
  - Redesigning unrelated application logging behavior beyond storage and retention.
- Acceptance criteria:
  - The plan states exact host paths for app logs, Loki data, and Promtail state.
  - The plan includes compose volume mappings for those paths.
  - The plan defines retention and rotation policy.
  - The plan defines a backup strategy to a local or secondary disk.
  - The plan includes a migration checklist.

## Open Questions
- None blocking for the current MVP plan.
- Any future off-host disaster recovery work should be tracked as a separate follow-up issue.

## Related Files
- [ARD](ARD.md)
