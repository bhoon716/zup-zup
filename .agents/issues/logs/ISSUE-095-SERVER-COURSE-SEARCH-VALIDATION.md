# ISSUE-095 verification log

- 2026-07-13: Course search request now uses `@Valid`; text/list/time/credit bounds, sort whitelist (`name|popular|rating|current|available`) and order whitelist (`asc|desc`) are enforced.
- 2026-07-13: enum conversion failures for classifications, grading methods, lecture languages, statuses, day, and disclosure are rejected as `400/G002` before QueryDSL construction; invalid schedule ranges and credits are rejected too.
- 2026-07-13: oversized text, invalid target grade, enum/range regression tests passed. Server `./gradlew check --no-daemon` passed.
