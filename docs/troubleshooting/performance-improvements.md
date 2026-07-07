# Performance Improvements Troubleshooting

Use this guide when a request is slow, a benchmark is noisy, or the before/after comparison is hard to trust.

## Summary
Keep the performance workflow repeatable: capture a baseline, rerun the same workload after the change, compare the result, write a compact summary, and document any measurement limits.

## Baseline
- Record the starting metric, workload, and environment.
- Capture the exact command or benchmark that produced the baseline.

## Compact Summary
- Record the scenario, baseline, after/result, workload or iterations, key metric, raw report path, limitations, and follow-up issue if any.
- Keep the summary short enough that a reviewer can judge the result without opening the raw artifact first.

## Measurement
- Run the same workload after the change.
- Use the same metric whenever possible.
- Note any differences in machine, cache state, sample size, or input data.

## Troubleshooting
- If the result is noisy, rerun the measurement and record the noise source.
- If the metric is unclear, narrow the request to one user flow or endpoint.
- If the raw output is large, link it and keep the compact summary as the primary review surface.
- If the change is durable, update `performance-improvement.md` and record the approval in `rule-update-log.md`.

## Evidence
- Attach the baseline output, the follow-up output, the comparison summary, and the raw report path.
- Link to the relevant source files or commands that prove the change.

## Related Files
- [Evidence Summary](evidence-summary.md)
