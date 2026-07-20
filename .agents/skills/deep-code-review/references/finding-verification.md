# Finding Verification

## Purpose

Independently test whether a candidate finding is a real, changed, reachable, actionable problem. The verifier begins by trying to disprove the candidate and confirms it only when the rebuttal fails.

## Verification order

1. Read the candidate and cited lines without trusting its conclusion.
2. Confirm the location is in the reviewed change and still represents the claimed root cause.
3. Trace reachability from a real entry point, caller, event, or deployment path.
4. Check caller validation, callee behavior, types, framework guarantees, configuration, transaction/lock/constraint, and relevant tests.
5. Try alternate inputs, states, ordering, retries, and failure paths that would invalidate the claim.
6. Compare the relevant base and head behavior to confirm introduction or worsening.
7. Separate root cause from symptom and assess actual impact.
8. Return `confirmed`, `rejected`, or `inconclusive` with evidence.

## Required checks

- **Introduced by change:** the defect is new or materially worse in the reviewed patch.
- **Reachable:** a concrete caller, input, event, state, or deployment path can trigger it.
- **Location valid:** the cited file and minimal line range contain the root cause or necessary changed contract.
- **Root cause valid:** the proposed fix would address the failure, rather than only hide a symptom.
- **Guard check:** existing validation, authorization, transaction, lock, constraint, test, or framework behavior does not already prevent it.
- **Impact valid:** the consequence is concrete and not exaggerated.

## Disproof attempts

Record attempts explicitly, including when they fail:

- caller-side validation or normalization
- alternate caller restrictions
- type, schema, or compiler guarantees
- transaction boundary, lock, unique constraint, or idempotency guard
- framework/runtime behavior
- relevant unit, integration, contract, concurrency, or deployment tests
- feature flag, configuration, or environment limitation

Do not treat inability to run a test as proof of a bug. Record it as a limitation or use evidence grade B/C.

## Evidence grades

- **A:** directly demonstrated by a reproducible test, failed command, clear execution path, or direct contract violation.
- **B:** code path and triggering conditions are clear, but direct execution was not possible.
- **C:** plausible but depends on unverified caller, environment, state, or framework facts.

Only A and verified B candidates may become final findings. Exclude C from the final report unless additional evidence upgrades it.

## Verdict rules

- `confirmed`: introduced, reachable, location/root cause valid, guard checks do not invalidate it, and impact is actionable.
- `rejected`: a disproof attempt shows the issue cannot occur, is not changed by the patch, is already guarded, or is not a real problem.
- `inconclusive`: evidence remains insufficient after retry or necessary context is unavailable.

Use severity based on concrete impact and reachability:

- **critical:** likely severe security, data-loss, or system-wide impact with a practical path
- **high:** merge-blocking security, correctness, contract, reliability, or deployment failure
- **medium:** meaningful user or operational impact with narrower conditions or a viable workaround
- **low:** actionable but limited impact; do not use for style or preference

Do not invent numerical scores.

## Output contract

```yaml
verification:
  candidate_id: <id>
  verdict: confirmed | rejected | inconclusive
  disproof_attempts:
    - <check performed and result>
  introduced_by_change: true | false | unknown
  reachable: true | false | unknown
  location_valid: true | false | unknown
  root_cause_valid: true | false | unknown
  evidence_grade: A | B | C
  final_severity: critical | high | medium | low | null
  supporting_evidence:
    - file: <path>
      lines: <range>
      explanation: <why it supports or rejects the candidate>
  limitation: <optional>
```

## Stopping conditions

Stop once the required checks are complete and one verdict is supportable. Do not keep investigating a rejected candidate to find a different issue; return a new candidate through the appropriate reviewer if necessary.
