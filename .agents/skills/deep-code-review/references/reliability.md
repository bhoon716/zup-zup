# Reliability Review Lens

## Purpose

Find concrete failures involving concurrency, asynchronous work, state, retries, timeouts, transactions, partial failure, duplicate processing, rollback, or resource lifecycle.

## When to use

Use for queues, workers, jobs, shared mutable state, locks, transactions, external calls, retries, timeouts, long-running operations, cleanup, rollback, and other changes where timing or failure ordering matters.

## Responsibilities

- trace success, timeout, cancellation, retry, duplicate, and partial-failure paths
- inspect atomicity, isolation, locking, idempotency, ordering, backpressure, and queue semantics
- check resource ownership, release, leak, exhaustion, and lifecycle boundaries
- determine what fails together and what remains available
- distinguish code defects from unavailable environment or operational assumptions

## Out of scope

Do not report generic scalability advice, a missing retry without a concrete failure mode, infrastructure preferences outside the change, or ordinary performance concerns that do not create a correctness or reliability impact.

## Review procedure

1. Map state owners, concurrent actors, external dependencies, and failure boundaries.
2. Trace the operation under success, timeout, cancellation, retry, duplicate, and process restart.
3. Check atomicity between validation and mutation, transaction boundaries, locks, constraints, and idempotency keys.
4. Check retry/backoff, timeout, queue acknowledgement, ordering, and dead-letter behavior.
5. Check cleanup for connections, files, locks, memory, jobs, and temporary state.
6. Confirm guards and tests before emitting a candidate.

## Valid finding requirements

Include the triggering interleaving or failure sequence, violated invariant, reachable path, exact changed location, impact, and evidence that transaction/lock/constraint/framework behavior does not prevent it. Name the smallest regression or concurrency test that would demonstrate the issue when useful.

## Common false positives

- concurrency that cannot occur under the actual scheduler or ownership model
- retries already made safe by idempotency or deduplication
- cleanup delegated to a framework with a documented guarantee
- operational capacity speculation without evidence of a failure
- existing race conditions not changed or worsened by the patch

## Escalation signals

Escalate `security` for replay or authorization races, `contract-tests` for duplicate/order/error contracts, `architecture` for state ownership changes, and `infrastructure` for capacity, deployment, or runtime configuration effects.

## Output requirements

Return `candidate_finding` objects with `lens: reliability`, exact location, root cause, failure sequence, expected/actual behavior, impact, evidence, invalidating evidence checked, severity proposal, evidence grade proposal, and escalation.

## Stopping conditions

Stop after relevant state, failure, and resource paths are traced, or after unavailable runtime facts are recorded. Do not report a race or leak without a plausible execution sequence.
