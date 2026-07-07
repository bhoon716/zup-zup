# Security Findings

See full issue details in [Audit Ledger](audit_ledger.md).

## Findings

- `SEC-001` Critical: Production secrets are literal config values.
- `SEC-002` High: Discord OAuth state is not CSRF-bound.
- `SEC-003` High: Internal services, actuator, direct app port, and unauthenticated Loki are host-published or public.
- `SEC-004` High: Query strings, raw tokens, device tokens, and user identifiers are logged.
- `WEB-002` Medium: Service worker opens push payload URLs without allowlisting.
- `API-002` Medium: Security error responses do not use the common runtime error shape.

## Security Posture

The most urgent issue is credential exposure. This should be treated as already compromised until keys are rotated. Infrastructure exposure and raw-token logging compound the blast radius because durable logs can contain reusable secret material.

## Required Verification After Fixes

- Secret scan returns no production credential material.
- OAuth linking tests reject missing, reused, or mismatched state.
- External port scan confirms only intended public ports are reachable.
- Loki/Prometheus/Admin surfaces require internal network, VPN, or auth.
- Logs contain masked or hashed token identifiers only.
