# API Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `API-001` Medium: Success envelopes are inconsistent.
- `API-002` Medium: Security error responses differ from runtime error responses.
- `API-003` Medium: `courseKey` path encoding is inconsistent.
- `WEB-001` Medium: Public detail routes lack server metadata.

## API Contract Rules Proposed

- Use `CommonResponse<T>` for JSON success responses unless endpoint intentionally returns `204`.
- Use `ErrorResponse` for all runtime and security failures.
- Encode all path variables at the frontend boundary.
- Document status semantics: `201 Created` for resource creation, `200 OK` for commands/toggles/searches.

## Verification Needed

Add controller/API contract tests for representative endpoints in each response category.
