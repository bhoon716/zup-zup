# Correctness Review Lens

## Purpose

Find concrete behavioral defects introduced or worsened by the change. Focus on whether the changed execution paths preserve their intended invariants.

## When to use

Use for any change that can affect runtime behavior, state, data transformation, error handling, or resource lifecycle.

## Responsibilities

- trace changed branches through representative callers and callees
- check conditions, state transitions, null/empty/boundary values, parsing, serialization, and conversions
- inspect success, failure, cancellation, retry, cleanup, and resource lifecycle paths
- compare changed behavior with nearby working paths and relevant tests
- distinguish a real regression from a code smell or preference

## Out of scope

Do not report general security, architecture, infrastructure, style, naming, or missing-test concerns unless they describe a concrete correctness failure. Route those signals to the orchestrator.

## Review procedure

1. Read the diff and identify changed behavior, not just changed lines.
2. Trace each changed input to its output or state mutation.
3. Test mentally or with focused read-only commands the important boundary cases: null, empty, zero, duplicate, ordering, partial input, failure, and repeated invocation.
4. Check caller/callee assumptions and error contracts.
5. Check whether existing guards, types, tests, or framework behavior invalidate the concern.
6. Emit only actionable candidate findings with exact locations.

## Valid finding requirements

A candidate must have:

- a changed or worsened line in the root-cause path
- a reachable execution path
- a specific input, state, or ordering that triggers the defect
- expected versus actual behavior
- user, data, or operational impact
- evidence that existing guards do not prevent it
- a focused fix direction

## Common false positives

- hypothetical paths unreachable through current callers
- behavior explicitly changed by the user’s stated intent
- defensive checks already enforced by types, validation, or framework contracts
- code that is unused in the reviewed scope
- style preferences without a failure scenario

## Escalation signals

Escalate `contract-tests` for a public behavior or error contract change, `reliability` for concurrency/atomicity/resource failure, `security` for trust or sensitive-data impact, and `architecture` for a boundary or ownership change.

## Output requirements

Return zero or more `candidate_finding` YAML objects. Include `lens: correctness`, title, exact location, root cause, failure scenario, expected/actual behavior, impact, evidence, invalidating evidence checked, severity proposal, evidence grade proposal, and escalation.

## Stopping conditions

Stop after all changed behavior paths and necessary adjacent callers/tests have been traced, or after a documented scope limitation prevents further verification. Prefer no finding over an unproven concern.
