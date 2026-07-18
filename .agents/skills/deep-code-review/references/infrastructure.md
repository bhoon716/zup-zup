# Infrastructure Review Lens

## Purpose

Find concrete deployment, runtime, identity, network, configuration, secret-management, or capacity defects introduced by a code or infrastructure change.

## When to use

Use for Terraform, Kubernetes, Docker, CI/CD, IAM, networking, runtime configuration, secret management, deployment manifests, data migration infrastructure, or application changes with direct operational impact.

## Responsibilities

- inspect resource identity, permissions, network reachability, and trust boundaries
- check environment-specific configuration, defaults, rollout, rollback, and drift risks
- verify deployment ordering, health checks, scaling, resource limits, and dependency availability
- inspect secret references and sensitive configuration without reading secret values
- assess data migration, backup, recovery, and capacity effects when directly changed

## Out of scope

Do not report generic cloud advice, unsupported capacity estimates, unrelated operational debt, or application-only correctness issues. Do not read credentials or secret values.

## Review procedure

1. Identify changed resources, environments, owners, dependencies, and rollout path.
2. Compare permissions, network paths, configuration defaults, and resource lifecycle before and after.
3. Trace startup, deployment, health, scaling, failure, rollback, and migration ordering.
4. Check whether application assumptions match the runtime configuration and platform guarantees.
5. Confirm existing policy, CI validation, or deployment guards before creating a candidate.
6. Report a specific failure condition and operational impact.

## Valid finding requirements

Include the affected environment/resource, reachable failure path, exact changed location, violated operational or security invariant, impact, and evidence that existing policy or deployment checks do not prevent it. State whether the issue is environment-specific.

## Common false positives

- hypothetical capacity problems without workload or configuration evidence
- permissions that are intentionally required and constrained by policy elsewhere
- local configuration differences with no affected deployment path
- secrets referenced by name without exposing or assuming their value
- unchanged infrastructure debt

## Escalation signals

Escalate `security` for IAM, network, secret, or sensitive-data exposure; `reliability` for rollout/recovery/capacity failure; `contract-tests` for runtime contract changes; and `architecture` for deployable-unit or ownership changes.

## Output requirements

Return `candidate_finding` objects with `lens: infrastructure`, exact resource/file location, root cause, failure scenario, expected/actual behavior, impact, evidence, invalidating evidence checked, severity proposal, evidence grade proposal, and escalation.

## Stopping conditions

Stop after changed operational resources and their rollout/runtime dependencies are traced, or when unavailable environment facts are documented as limitations. Never fill missing infrastructure evidence with guesses.
