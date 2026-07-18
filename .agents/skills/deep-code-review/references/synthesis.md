# Review Synthesis

## Contents

- [Purpose](#purpose)
- [Inputs](#inputs)
- [Deduplication](#deduplication)
- [Finding format](#finding-format)
- [Verdict rules](#verdict-rules)
- [Final output](#final-output)
- [Stopping conditions](#stopping-conditions)

## Purpose

Turn verified findings into one concise, actionable code review. Use only verification results; do not create new findings or re-review the code.

## Inputs

Use:

- resolved scope and change intent
- selected and skipped lenses
- candidate counts
- `confirmed` verification results
- supporting test gaps tied to confirmed findings
- limitations and independence status

Never use rejected or inconclusive candidates as final issues. Mention their counts only in Review execution when useful.

## Deduplication

Merge findings by root cause and violated invariant, not text similarity. Treat findings as the same only when they share:

- affected state or resource
- violated invariant
- triggering condition or execution order
- root cause that must be fixed

Keep findings separate when the affected resource, invariant, trigger, or remediation differs. A security race, reliability race, and test gap may become one finding with supporting evidence when they describe the same atomicity defect.

## Finding format

For each final finding include:

- severity and lens
- concise title stating the failure
- exact file and minimal line range
- root cause
- concrete failure scenario
- expected versus actual behavior
- impact
- focused suggested fix direction
- verification result and evidence grade
- supporting test gap when it is specific to the finding

Do not include style advice, broad refactoring recommendations, speculative risks, or fixes that require inventing product policy.

## Verdict rules

- **Incorrect:** at least one confirmed critical/high blocking issue.
- **Correct with non-blocking issues:** no blocking issue, but one or more confirmed medium/low findings.
- **Correct:** no confirmed findings and review coverage is sufficient for the scope.
- **Unable to verify:** required scope, context, or verification was unavailable enough that correctness cannot be stated honestly.

If a high-impact area was skipped because of budget or failure, do not silently return `Correct`; use `Unable to verify` or state the material limitation clearly.

## Final output

```md
# Code Review

## Verdict
<Correct | Correct with non-blocking issues | Incorrect | Unable to verify>

## Blocking findings

### [HIGH][Security] <failure title>

Location: `src/auth/session.ts:83-96`

Root cause: <changed cause>

Failure scenario: <specific trigger or sequence>

Expected behavior: <invariant>

Actual behavior: <observed or inferred behavior>

Impact: <concrete consequence>

Suggested fix: <focused direction>

Verification: <confirmed evidence and grade>

## Non-blocking findings
<same structure, or None>

## Supporting test gaps
- <specific regression or contract test tied to a finding, or None>

## Review execution
Mandatory reviewers: <ids>
Conditional reviewers: <ids>
Dynamically added reviewers: <ids or none>
Candidate findings: <N>
Confirmed: <N>
Rejected: <N>
Inconclusive: <N>

## Coverage
Reviewed:
- <component, file group, or flow>

Skipped:
- <lens>: <reason>

## Limitations
- <test, history, environment, or independence limitation>

## Report
Path: `.agents/reviews/deep-code-review/<review-id>.md`
```

Use exact paths and tight line ranges. Keep the report ordered by severity, then by evidence strength and location. Put `None` rather than padding the report with weak concerns.

Return the complete report body to the orchestrator. The orchestrator creates `.agents/reviews/deep-code-review/` relative to the repository root and writes `<review-id>.md` only after synthesis completes; do not persist intermediate candidates or verifier packets.

## Stopping conditions

Stop when all confirmed findings are represented once, the verdict follows from their severity and coverage, rejected/inconclusive counts are accurate, and limitations are explicit. Do not append a new issue discovered during synthesis; return it to the appropriate review/verifier stage.
