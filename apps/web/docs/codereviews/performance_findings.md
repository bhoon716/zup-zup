# Performance Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `PERF-001` Medium: Course search lacks supporting indexes.
- `PERF-002` Medium: Minute-level crawler does per-course lookup/save.
- `PERF-003` Medium: Notification and seat history APIs are unbounded.
- `PERF-004` Medium: Timetable detail can lazy-load custom schedule times per schedule.
- `WEB-001` Medium: Cookie read in root layout forces request-time rendering for public routes.
- `MAINT-001` Medium: Search and notification complexity increases regression/performance tuning cost.

## Measurements Still Needed

- `EXPLAIN ANALYZE` for dominant search queries.
- Crawl execution time and DB query count at current production course count.
- History endpoint response size at realistic retention.
- Browser trace for infinite scroll and public route cache behavior.

## First Improvements

- Add pagination and indexes for growing history tables.
- Batch course crawl reads/writes.
- Split public and auth-dependent frontend rendering.
