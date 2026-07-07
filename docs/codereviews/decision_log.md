# Decision Log

| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-07-06 | Ignore prior `docs/reviews/` for this audit | User explicitly stated prior review content is separate. |
| 2026-07-06 | Write this audit under `docs/codereviews/` | User explicitly requested this output folder. |
| 2026-07-06 | Treat generated dirs as excluded from code audit | `.git`, `.gradle`, `node_modules`, `.next`, and build outputs are generated/noisy. |
| 2026-07-06 | Prioritize security, data integrity, and deploy correctness over style | Matches repository audit specification and operational risk. |
| 2026-07-06 | Keep performance findings with "measurement needed" below P0/P1 | Several performance risks need `EXPLAIN` or runtime measurements before high-severity remediation. |
