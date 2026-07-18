# Contract and Test Review Lens

## Purpose

Find concrete mismatches between changed behavior and the contracts expected by callers, clients, serializers, persistence boundaries, or tests. Report test gaps only when they leave a specific regression or contract violation unprotected.

## When to use

Use for runtime changes, public APIs, response/request schemas, error behavior, serialization, persistence contracts, events, CLI interfaces, and changes that should be protected by tests.

## Responsibilities

- identify changed public and internal contracts
- inspect callers and consumers for old assumptions
- check success, error, serialization, ordering, and compatibility behavior
- determine whether tests exercise the changed invariant rather than merely the happy path
- detect mocks, fixtures, snapshots, or assertions that can pass while the implementation is wrong

## Out of scope

Do not report “there is no test” by itself, test style preferences, broad test coverage targets, or unrelated existing contract debt. Do not duplicate a correctness finding unless the contract evidence changes the root cause or impact.

## Review procedure

1. Map changed inputs, outputs, errors, schemas, events, and persistence effects.
2. Locate callers, client code, adapters, fixtures, and contract tests.
3. Compare old and new behavior at each boundary.
4. Check backward compatibility, optional/required fields, defaults, serialization formats, status codes, error codes, and idempotency expectations.
5. Evaluate whether tests would fail for the concrete defect and whether mocks hide it.
6. Emit a candidate only when a specific scenario is unprotected or a contract is violated.

## Valid finding requirements

Include the affected contract, caller or consumer, triggering scenario, expected versus actual behavior, concrete impact, exact changed location, and the test or guard that fails to protect it. A useful test-gap finding names the missing invariant and the smallest regression case that should be added.

## Common false positives

- internal implementation details with no observable contract
- optional fields whose defaults are explicitly compatible
- tests that are absent but the behavior is fully covered at another relevant boundary
- snapshots or mocks that are intentionally not contract tests
- a theoretical client that is not reachable in the reviewed scope

## Escalation signals

Escalate `security` for authorization or sensitive-data contract changes, `reliability` for retry/duplicate/order semantics, `architecture` for public abstraction ownership, and `infrastructure` for deployment/runtime contract changes.

## Output requirements

Return `candidate_finding` objects with `lens: contract-tests`, contract location, changed location, root cause, failure scenario, expected/actual behavior, impact, test evidence, invalidating evidence checked, severity proposal, evidence grade proposal, and escalation.

## Stopping conditions

Stop after changed contracts and their relevant consumers/tests are mapped, or when missing external consumers are explicitly recorded as a limitation. Do not infer a contract from naming alone.
