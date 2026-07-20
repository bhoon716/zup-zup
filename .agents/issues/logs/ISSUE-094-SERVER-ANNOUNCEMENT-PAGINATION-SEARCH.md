# ISSUE-094 verification log

- 2026-07-13: public/admin announcement endpoints now return bounded `Page` responses (default 20, max 100); pageable guard rejects oversized page/offset requests.
- 2026-07-13: keyword is trimmed and limited to 100 characters; blank searches use the same bounded query. Pinned-first ordering is retained. `announcement.search.latency` records search type and keyword presence.
- 2026-07-13: `AnnouncementControllerTest` verifies paged JSON content/metadata and ordering contract. Announcement/dashboard tests, server `./gradlew check`, web lint, and web Vitest (60 files/181 tests) passed.
- 2026-07-13: `%keyword%` query-plan follow-up is documented; full-text/trigram index adoption is deferred until production data growth justifies it.
