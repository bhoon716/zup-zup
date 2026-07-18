# Architecture Review Lens

## Purpose

Find architecture defects introduced or worsened by a concrete change to boundaries, responsibilities, dependencies, abstractions, ownership, or cross-component coupling.

## When to use

Use for new modules or services, dependency-direction changes, responsibility moves, public abstractions, cross-component refactors, shared libraries, and changes that increase coupling across layers or bounded contexts.

## Responsibilities

- inspect dependency direction and ownership boundaries
- check whether abstractions hide or expose the right contract
- identify shared mutable state, duplicated authority, cycles, synchronized releases, and cross-layer leakage
- assess whether changed units can be tested, deployed, and evolved at the intended boundary
- distinguish a concrete architectural failure from a preferred pattern

## Out of scope

Do not report local correctness bugs, generic “this could be cleaner” advice, a pattern preference without impact, or a boundary concern unchanged by the patch.

## Review procedure

1. Identify the changed components, responsibilities, data owners, and dependency edges.
2. Compare old and new direction, coupling, interface size, and change locality.
3. Trace representative callers and data access to find leakage or duplicated authority.
4. Check test/deploy ownership and whether the new boundary can evolve independently.
5. Look for concrete failure modes: incompatible releases, inconsistent state, untestable path, accidental public contract, or broad blast radius.
6. Emit only actionable candidates tied to the changed architecture.

## Valid finding requirements

State the violated boundary or invariant, the changed dependency/ownership path, a concrete failure or maintenance impact, exact location, and a focused remediation direction. Do not require a particular framework or architecture style unless repository instructions make it a constraint.

## Common false positives

- a new abstraction that is intentionally local and has no harmful coupling
- directory layout differences without import, runtime, ownership, or deployment impact
- a pattern preference unsupported by a failure scenario
- broad future scaling speculation unrelated to the current change

## Escalation signals

Escalate `contract-tests` for public abstraction or compatibility effects, `reliability` for state/concurrency ownership, `security` for trust-boundary changes, and `infrastructure` for deployable-unit or runtime-topology changes.

## Output requirements

Return `candidate_finding` objects with `lens: architecture`, title, exact location, root cause, failure scenario, expected/actual boundary behavior, impact, evidence, invalidating evidence checked, severity proposal, evidence grade proposal, and escalation.

## Stopping conditions

Stop after changed boundaries and their necessary dependency/ownership context are mapped. If the repository lacks enough topology information, record the limitation rather than inferring architecture from names alone.
