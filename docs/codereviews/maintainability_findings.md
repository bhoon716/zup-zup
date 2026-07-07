# Maintainability Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `ARCH-001` High: Application services depend on presentation DTOs.
- `ARCH-002` High: Frontend dependency cycle and feature boundary drift.
- `MAINT-001` Medium: Core modules combine too many responsibilities.
- `CONS-001` Medium: Local validation/transaction conventions drift.

## Refactoring Candidates

- Course search: split criteria parsing, predicate construction, sorting, and QueryDSL assembly.
- Notification: introduce channel handlers for target resolution, enabled policy, and dispatch.
- Settings/timetable pages: extract hooks and section components.
- Backend service layer: introduce application command/result types.

## Guardrail

Refactoring should be behavior-preserving and test-backed. Start with failing lint and the smallest boundary violation before broad module moves.
