# Readability Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `WEB-003` Medium: Some interactive controls lack accessible names or have incorrect edit state.
- `CONS-001` Medium: Custom schedule validation message and annotations do not match.
- `CONS-002` Medium: GET-style methods have hidden write behavior through `getOrInitSetting`.
- `MAINT-001` Medium: Complexity suppressions and large orchestration components make code harder to review.

## Notes

This audit intentionally excludes taste-only style comments. Readability findings are included only where naming, comments, or structure can hide defects or raise maintenance risk.
