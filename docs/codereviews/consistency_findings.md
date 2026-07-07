# Consistency Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `API-001` Medium: API success response shape varies by controller.
- `API-002` Medium: Error response shape varies between MVC and security handlers.
- `ARCH-002` High: Frontend boundaries are declared but violated.
- `SEC-004` High: Sensitive logging policy conflicts with durable log retention.
- `CONS-001` Medium: Custom schedule validation and crawler setting read/write behavior drift from local conventions.
- `CONS-002` Medium: GET-style crawler setting reads create default database rows.

## Consistency Rules Proposed

- One JSON success envelope policy.
- One JSON error envelope policy.
- One frontend dependency direction policy.
- Sensitive data never appears as full-value log arguments.
- GET-style operations do not create default rows; initialization is explicit.
