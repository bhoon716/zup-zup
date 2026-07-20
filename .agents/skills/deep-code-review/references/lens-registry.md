# Lens Registry

## Purpose

Route a concrete code change to the smallest set of independent review lenses that can find meaningful risk. This file selects work; it does not review code or create findings.

## Mandatory lenses

When the change can alter runtime behavior, always run:

- `correctness`
- `contract-tests`

If the scope contains only documentation, comments, formatting, or generated output with no behavior or contract impact, record why mandatory runtime lenses were skipped.

## Conditional lenses

| Lens | Hard trigger | Soft trigger | Skip when |
| --- | --- | --- | --- |
| `security` | authn/authz, session, cookie, token, user input, sensitive data, tenant isolation, payment/admin, crypto, secret, credential, file/DB/network access | new external input or boundary | no trust, identity, data, or access-control impact |
| `reliability` | async work, shared mutable state, queue/worker, lock, transaction, retry, timeout, external service, partial failure, rollback, duplicate processing, cleanup | new long-running operation or resource | no state, concurrency, failure, or resource-lifecycle impact |
| `architecture` | new module/service, dependency direction, responsibility move, public abstraction, cross-component refactor, shared library | new abstraction or repeated coupling | small local change with no boundary impact |
| `infrastructure` | Terraform, Kubernetes, Docker, CI/CD, IAM, networking, runtime config, secret management, deployment manifest | application change with deployment or capacity implications | no deployment, runtime, network, identity, or operational configuration impact |

## Routing algorithm

1. Read the `change_map`, not the source, to identify signals.
2. Select mandatory lenses when behavior changes.
3. Select every hard-triggered conditional lens.
4. Inspect soft triggers only enough to decide whether a conditional lens is useful.
5. Record selected lenses, skipped lenses, and reasons in the routing result.
6. Never route based only on file extension or a fashionable technology name.

```yaml
routing:
  mandatory:
    - correctness
    - contract-tests
  conditional:
    - security
    - reliability
  skipped:
    architecture:
      reason: "No module boundary or dependency direction changed"
```

## Escalation

A reviewer may return a signal when its assigned evidence exposes another lens:

```yaml
escalation:
  requested_lens: reliability
  reason: "Two workers can update the same session state concurrently"
  evidence:
    file: src/jobs/processor.ts
    lines: 72-94
```

Route the signal through the orchestrator. Do not let a reviewer launch another reviewer. Re-run routing only for new signals, allow at most two rounds, and record budget-limited or duplicate requests.

## Routing output

Return lens IDs, assigned reference paths, trigger evidence, skipped reasons, and the next execution batch. Do not include findings or severity judgments.
