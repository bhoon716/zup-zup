# Security Review Lens

## Purpose

Find exploitable security defects introduced or worsened by the change, with a concrete trust boundary, attacker-controlled input, execution path, and impact.

## When to use

Use when the change touches authentication, authorization, sessions, cookies, tokens, user input, sensitive information, tenant isolation, payment/admin behavior, cryptography, secrets, credentials, files, databases, networks, or external trust boundaries.

## Responsibilities

- trace untrusted input to sinks and security decisions
- verify authentication, authorization, tenant, object-level, and role checks
- inspect token/session lifecycle, replay, fixation, expiration, rotation, and invalidation
- check secrets, sensitive data, logging, serialization, encryption, and error exposure
- inspect file, database, network, deserialization, SSRF, injection, and resource access paths as relevant
- verify that existing guards actually cover the changed path

## Out of scope

Do not report generic hardening advice, policy preferences without an exploit path, architecture taste, or a vulnerability in unchanged code unless the change newly exposes or worsens it.

## Review procedure

1. Identify actors, trust boundaries, assets, and attacker-controlled values affected by the diff.
2. Trace authorization and validation from entry point to sensitive operation.
3. Check bypasses through alternate callers, default values, error paths, race windows, and tenant/object identifiers.
4. Inspect storage, transport, logs, and responses for leakage or unsafe handling.
5. Check framework guarantees and existing controls before creating a candidate.
6. Describe a practical exploit or misuse scenario and its impact.

## Valid finding requirements

A candidate must state the attacker capability, reachable changed path, violated security property, concrete impact, exact location, and why existing validation, policy, transaction, or framework controls do not stop it. Avoid unsupported CVSS-like precision; propose a severity grounded in impact and exploitability.

## Common false positives

- attacker control that is impossible through current callers or authorization
- secrets in test fixtures that are demonstrably non-sensitive and scoped to tests
- framework-managed escaping or validation already applied at the relevant boundary
- general defense-in-depth suggestions without a new exploit scenario
- unchanged pre-existing exposure not affected by the patch

## Escalation signals

Escalate `reliability` for replay/race/cleanup behavior, `contract-tests` for auth/error or data-exposure contracts, `architecture` for trust-boundary ownership changes, and `infrastructure` for IAM, network, secret-management, or deployment controls.

## Output requirements

Return `candidate_finding` objects with `lens: security`, title, location, root cause, attacker/failure scenario, expected/actual security behavior, impact, evidence, invalidating evidence checked, severity proposal, evidence grade proposal, and escalation.

## Stopping conditions

Stop when all changed trust boundaries and sensitive operations are traced, or when the necessary deployment/runtime context is unavailable and is recorded as a limitation. Do not turn missing context into a confirmed vulnerability.
