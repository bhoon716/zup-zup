# Improvement Backlog

## P0

| Title | Description | Expected Benefit | Difficulty | Estimated Impact | Prerequisite |
| --- | --- | --- | --- | --- | --- |
| Rotate and externalize secrets | Remove production literals and rotate exposed values | Prevent credential takeover | Medium | Critical | Ops access |
| Secure Discord OAuth state | Server-side one-time state validation | Prevent account-link CSRF | Medium | High | OAuth flow update |
| Lock down internal ports | Remove/bind host-published internal services | Reduce attack surface | Medium | High | Operator access plan |
| Validate Flyway fresh migration | Fix mixed DB syntax and add migration test | Prevent failed fresh deploy | High | High | DB strategy |
| Stop destructive review migration | Define recovery/backfill or accepted loss | Preserve user data | High | High | Backup status |

## P1

| Title | Description | Expected Benefit | Difficulty | Estimated Impact | Prerequisite |
| --- | --- | --- | --- | --- | --- |
| Add DB uniqueness constraints | Enforce subscriptions, devices, reviews, primary timetable | Prevent duplicates/races | Medium | High | Duplicate cleanup |
| Atomic notification dedupe | Use SET NX or outbox | Prevent duplicate alerts | Medium | High | Retry policy |
| Dispatch events after commit | Use after-commit/outbox | Prevent false notifications | Medium | High | Test updates |
| Fail or explicitly skip batch item errors | Do not silently swallow crawl failures | Data correctness | Medium | High | Bad-input policy |
| Add CI gates | Run server/web/infra checks before deploy | Prevent regressions shipping | Medium | High | Workflow setup |

## P2

| Title | Description | Expected Benefit | Difficulty | Estimated Impact | Prerequisite |
| --- | --- | --- | --- | --- | --- |
| Standardize API envelopes | Align success/error bodies | Client simplicity | Medium | Medium | Contract decision |
| Add history pagination | Notification and course history paging/indexes | Runtime scalability | Medium | Medium | Frontend changes |
| Batch crawler writes | Bulk lookups and changed-row saves | Lower DB load | Medium | Medium | Event tests |
| Fix web lint | Remove boundary cycle and typed mocks | Clean quality gate | Medium | Medium | Auth adapter design |
| Improve PWA/service worker safety | URL allowlist and correct icons | Safer PWA | Low | Medium | Asset generation |

## P3

| Title | Description | Expected Benefit | Difficulty | Estimated Impact | Prerequisite |
| --- | --- | --- | --- | --- | --- |
| Split large UI pages | Extract hooks/sections | Lower UI maintenance cost | Medium | Medium | Current behavior tests |
| Update architecture docs | Describe real stack and repo model | Durable decisions | Low | Medium | Repo governance decision |
