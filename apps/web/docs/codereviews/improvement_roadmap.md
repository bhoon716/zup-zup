# Improvement Roadmap

## P0: Stop Active Exposure And Data Loss

1. Rotate all exposed credentials and remove literal production secrets.
2. Fix Discord OAuth state validation.
3. Restrict internal service and Loki exposure.
4. Fix fresh migration safety and validate with production-like DB.
5. Resolve destructive review migration impact.

## P1: Protect Correctness Under Load

1. Add DB constraints and duplicate cleanup migrations.
2. Make notification dedupe atomic.
3. Move notification dispatch after transaction commit.
4. Stop swallowing batch item failures.
5. Add required CI gates before deploy.

## P2: Standardize Contracts And Scale Hot Paths

1. Standardize API success/error envelopes.
2. Add pagination and indexes for growing history APIs.
3. Batch crawler DB work.
4. Fix frontend lint and boundary cycle.
5. Harden service worker URL handling and PWA icons.

## P3: Reduce Long-Term Drift

1. Extract search and notification complexity.
2. Split large frontend route pages into hooks/sections.
3. Update project-level architecture docs.
4. Decide and document repository topology.
