# ISSUE-107 verification log

- 2026-07-14: Baseline `npm run web:lint` completed with exit 0 but reported 8 warnings for `window.location.href`/`assign` navigation. Web baseline was 61 Vitest files and 183 tests.
- 2026-07-14: Login/logout internal `/login` navigation now uses Next Router. OAuth, Discord authorization, and sanitized PWA notification links retain full-document navigation with narrowly scoped ESLint rationale comments. Spring OAuth, Redis, and Spring Batch tests now use typed Mockito matchers/captors instead of raw generic calls.
- 2026-07-14: `npm run web:lint` passed with zero warnings/errors; web Vitest passed (61 files/183 tests); production build passed. `apps/server ./gradlew clean check --no-daemon --warning-mode all` passed with Checkstyle and PMD violation counts at zero and no Java unchecked compiler notes. The JVM class-sharing message is an external runtime notice, not a source warning.
- 2026-07-14: Issue closed after documentation updates and separate commit.
