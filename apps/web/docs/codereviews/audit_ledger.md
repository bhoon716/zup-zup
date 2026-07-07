# Repository Audit Ledger

Audit date: 2026-07-06

Scope: `server`, `web`, `infra`, root docs and issue metadata. Generated directories such as `.git`, `.gradle`, `node_modules`, `.next`, and build outputs were excluded except where their presence affects repository hygiene.

Verification evidence:
- `server`: `./gradlew test` passed.
- `web`: `npm test -- --run` passed.
- `web`: `npm run lint` failed with `boundaries/dependencies` and `no-explicit-any` errors.
- Existing `docs/reviews/` content was intentionally not used for this audit.

## Open Issues

### SEC-001
- Category: Security
- Severity: Critical
- Location: `server/src/main/resources/application-prod.yml:13`, `:44`, `:56`, `:68`, `:97`, `:101`; `server/.github/workflows/cd.yml:25`
- Description: Production credentials and signing secrets are stored as literal configuration values.
- Evidence: Production config contains DB/Redis passwords, Google OAuth secret, AWS keys, JWT secret, WebPush private key, Discord bot token and client secret. CD recreates that file from one opaque GitHub secret.
- Why: A single config leak gives direct access to infrastructure credentials and token-signing material.
- Impact: Token forgery, database/Redis compromise, cloud/email/Discord abuse, forced emergency rotation.
- Suggested Fix: Rotate every exposed credential. Replace literals with environment variables or mounted secret files. Split large config blobs into scoped secrets.
- Example Code: `password: ${SPRING_DATASOURCE_PASSWORD:?required}`
- Risk of Fix: Medium; deployment will fail if required variables are missing.
- Related Issues: `ISSUE-030-SERVER-REMOVE-HARDCODED-PRODUCTION-SECRETS-FROM-APPLICATION-PROD`
- Priority: P0
- Status: Open

### SEC-002
- Category: Security
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/user/presentation/UserController.java:240`; `web/src/app/(user)/onboarding/page.tsx:146`; `web/src/app/(user)/settings/page.tsx:237`
- Description: Discord OAuth account linking does not bind `state` to a server-generated CSRF token.
- Evidence: Callback accepts `code` and optional `state`; frontend sends fixed values such as `onboarding` and `settings`.
- Why: OAuth account linking requires a nonce bound to the initiating user/session.
- Impact: A logged-in victim can be induced to link an attacker-controlled Discord account.
- Suggested Fix: Generate `state` server-side, store it in session/Redis, verify once on callback, then delete it. Consider PKCE.
- Example Code: `if (!oauthStateStore.consume(userId, state)) throw FORBIDDEN;`
- Risk of Fix: Medium; OAuth start/callback flow changes across server and web.
- Related Issues: `ISSUE-002-SERVER-VERIFY-DISCORD-OAUTH-STATE-BEFORE-LINKING-ACCOUNTS`
- Priority: P0
- Status: Open

### SEC-003
- Category: Security / Infrastructure
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/common/config/SecurityConfig.java:32`; `server/docker-compose.yml:13`; `infra/docker-compose.yml:7`, `:30`, `:39`, `:57`, `:79`, `:95`; `infra/loki/loki-config.yaml:1`
- Description: Internal services and unauthenticated Loki are published on host ports.
- Evidence: `/actuator/**` is `permitAll`; the app publishes `8080`; MySQL, Redis, Prometheus, Grafana, NPM admin, and Loki ports are mapped to the host; Loki has `auth_enabled: false`.
- Why: Host port publishing is externally reachable unless firewall rules block it.
- Impact: Exposes datastore, session/cache, metrics, logs, admin UI, and direct app traffic that can bypass proxy controls.
- Suggested Fix: Remove host `ports` for internal services, bind admin UIs to `127.0.0.1`, restrict actuator to health/internal networks, force HTTPS at proxy, and block direct app port.
- Example Code: `- "127.0.0.1:3001:3000"`
- Risk of Fix: Medium; operators may need SSH tunnels or VPN access.
- Related Issues: `ISSUE-011`, `ISSUE-014`, `ISSUE-026`
- Priority: P0
- Status: Open

### SEC-004
- Category: Security / Observability
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/common/filter/HttpLoggingFilter.java:31`; `server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtProvider.java:93`; `server/src/main/java/bhoon/sugang_helper/user/application/UserDeviceService.java:36`; `server/src/main/java/bhoon/sugang_helper/user/application/EmailVerificationService.java:55`; `server/src/main/java/bhoon/sugang_helper/common/security/oauth/OAuth2SuccessHandler.java:53`
- Description: Reusable credentials and user identifiers are logged in durable logs.
- Evidence: HTTP logging appends full query strings; JWT blacklist detection logs the raw token; device registration logs push tokens; email verification/auth logs include email addresses and OAuth success logs user email.
- Why: Logs are persisted under host paths and shipped to Loki.
- Impact: Log readers can recover JWTs, push endpoints, and PII; impact compounds with exposed Loki.
- Suggested Fix: Mask or hash sensitive values before logging and add a static/logging regression check.
- Example Code: `log.warn("[JWT] Blacklisted token usage detected: hash={}", sha256(token));`
- Risk of Fix: Low; less debugging detail.
- Related Issues: `ISSUE-011`, `ISSUE-013`
- Priority: P0
- Status: Open

### DB-001
- Category: Database / Reliability
- Severity: Critical
- Location: `server/src/main/resources/db/migration/V1__initial_schema.sql:1`; `server/src/main/resources/application.yml:17`; `server/src/test/resources/application.yml:17`
- Description: Fresh Flyway migration is not validated and mixes H2-only and MySQL-only statements.
- Evidence: V1 includes `SET REFERENTIAL_INTEGRITY FALSE` and `SET FOREIGN_KEY_CHECKS=0`; production uses Flyway with MySQL, while tests disable Flyway and use Hibernate `create-drop`.
- Why: The production migration path is not exercised by tests.
- Impact: Fresh MySQL deployment can fail before schema creation, or schema drift can pass local tests.
- Suggested Fix: Split DB-specific migrations or remove H2-only statements from MySQL migrations. Add Testcontainers MySQL Flyway validation.
- Example Code: `jdbc:tc:mysql:8.0:///sugang_helper`
- Risk of Fix: High; already-applied migrations need checksum/history care.
- Related Issues: `ISSUE-033-SERVER-FIX-H2-FLYWAY-MIGRATION-COMPATIBILITY-IN-TESTS`
- Priority: P0
- Status: Open

### DB-002
- Category: Database / Data Integrity
- Severity: High
- Location: `server/src/main/resources/db/migration/V6__create_course_emoji_reviews.sql:1`; `server/src/main/resources/db/migration/V7__restore_course_reviews.sql:1`
- Description: A migration drops course review tables and later recreates empty tables.
- Evidence: V6 drops `course_review_reactions` and `course_reviews`; V7 recreates the schema without a data restore path.
- Why: Migrations should be non-destructive unless data loss is explicitly accepted and backed up.
- Impact: Existing course reviews and reactions are permanently lost on migration.
- Suggested Fix: Replace destructive migration with archive/rename/backfill strategy. For applied environments, restore from backup or document loss.
- Example Code: `CREATE TABLE course_reviews_archive AS SELECT * FROM course_reviews;`
- Risk of Fix: High; recovery depends on available backups.
- Related Issues: None found
- Priority: P0
- Status: Open

### DB-003
- Category: Database / Data Integrity
- Severity: High
- Location: `server/src/main/resources/application.yml:17`; `server/src/main/resources/application-prod.yml:19`; `server/src/main/resources/db/migration/V2__course_review_schema.sql:27`; `server/src/main/resources/db/migration/V3__audit_and_deleted_at_fixes.sql:2`
- Description: Flyway `baseline-version: 3` can skip required schema changes on non-empty databases.
- Evidence: V2 and V3 create/alter required review, feedback, and audit columns, but baseline version is 3.
- Why: Baseline-on-migrate marks prior versions as applied without running them.
- Impact: Existing DBs without Flyway history can miss required tables/columns, causing validate failures or partial schema.
- Suggested Fix: Align baseline policy with actual production state and add a pre-baseline schema assertion script.
- Example Code: `SELECT COUNT(*) FROM information_schema.columns WHERE table_name='courses' AND column_name='average_rating';`
- Risk of Fix: Medium; environment-specific Flyway history must be inspected.
- Related Issues: None found
- Priority: P1
- Status: Open

### DATA-001
- Category: Data Integrity / Concurrency
- Severity: High
- Location: `server/src/main/resources/db/migration/V1__initial_schema.sql:226`; `server/src/main/java/bhoon/sugang_helper/subscription/application/SubscriptionService.java:64`; `server/src/main/java/bhoon/sugang_helper/user/domain/UserDevice.java:21`; `server/src/main/resources/db/migration/V7__restore_course_reviews.sql:1`; `server/src/main/java/bhoon/sugang_helper/review/application/CourseReviewService.java:207`
- Description: Several business invariants are checked in application code but not enforced by database constraints.
- Evidence: Subscriptions lack `(user_id, course_key)` uniqueness; reviews lack user/scope uniqueness; `UserDevice` declares unique token but V1 lacks the unique index; timetable primary uniqueness is not enforced; review reaction counters are plain entity field increments.
- Why: Check-then-insert code is race-prone under concurrent requests.
- Impact: Duplicate subscriptions, devices, reviews, notifications, and non-deterministic primary timetable selection.
- Suggested Fix: Add unique constraints after duplicate cleanup and handle `DataIntegrityViolationException` as domain errors.
- Example Code: `ALTER TABLE subscriptions ADD CONSTRAINT uk_subscription_user_course UNIQUE (user_id, course_key);`
- Risk of Fix: Medium; existing duplicates and MySQL index length constraints must be handled.
- Related Issues: None found
- Priority: P1
- Status: Open

### DATA-002
- Category: Data Integrity / Privacy
- Severity: High
- Location: `server/src/main/resources/db/migration/V1__initial_schema.sql:145`; `:226`; `:242`; `:260`; `server/src/main/java/bhoon/sugang_helper/user/application/UserService.java:97`
- Description: User-owned rows can remain orphaned after account withdrawal.
- Evidence: Many tables store `user_id` without foreign keys or cascade rules; withdrawal deletes subscriptions and then the user, but not devices, wishlists, reviews, emoji reviews, notification histories, feedbacks, or timetables.
- Why: Account deletion must have a complete data retention/anonymization policy.
- Impact: Orphaned PII and user content can survive deletion, creating privacy and data-quality risk.
- Suggested Fix: Add FK/cascade where deletion is correct, explicit cleanup/anonymization where retention is required, and a migration to handle existing orphan rows.
- Example Code: `ALTER TABLE user_devices ADD CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;`
- Risk of Fix: High; existing orphan data and product retention semantics must be resolved.
- Related Issues: None found
- Priority: P1
- Status: Open

### CONC-001
- Category: Concurrency / Notification
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/notification/application/NotificationService.java:56`; `:59`; `:65`; `:66`
- Description: Notification deduplication is non-atomic.
- Evidence: Handler checks `hasKey`, sends notifications, then writes the dedupe key. `RedisService` has `setValuesIfAbsent`, but this path does not use it.
- Why: Concurrent async events can pass the check before either writes the key.
- Impact: Duplicate notifications and duplicate notification histories for the same seat-opened event.
- Suggested Fix: Acquire the dedupe key with Redis `SET NX EX` before dispatch, or use an outbox with a unique event key.
- Example Code: `if (!redisService.setValuesIfAbsent(redisKey, "SENDING", DEDUP_TTL)) return;`
- Risk of Fix: Medium; a crash after pre-send locking can suppress retries.
- Related Issues: `ISSUE-012-NOTIFICATION`
- Priority: P1
- Status: Open

### CONC-002
- Category: Concurrency / Transaction
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/crawling/config/SpringBatchConfig.java:151`; `:155`; `server/src/main/java/bhoon/sugang_helper/notification/application/NotificationService.java:56`
- Description: Seat-opened events are consumed asynchronously before the batch chunk transaction commits.
- Evidence: `publishSeatOpenedEvent` runs inside the writer update path; listener is `@Async @EventListener`, not an after-commit listener.
- Why: External side effects must not happen before durable state is committed.
- Impact: Users can receive notifications for changes that later roll back.
- Suggested Fix: Dispatch via `@TransactionalEventListener(phase = AFTER_COMMIT)` or a committed outbox table.
- Example Code: `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`
- Risk of Fix: Medium; tests expecting immediate event publication need adjustment.
- Related Issues: None found
- Priority: P1
- Status: Open

### SPRING-001
- Category: Spring / Correctness
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/crawling/config/SpringBatchConfig.java:81`; `:84`; `server/src/main/java/bhoon/sugang_helper/crawling/application/CourseCrawlerService.java:103`
- Description: Batch item failures are swallowed, so jobs can report success with incomplete data.
- Evidence: Writer catches `Exception` per DTO, logs it, and continues. Caller only fails when Spring Batch execution status is unsuccessful.
- Why: Silent partial success hides data ingestion failures.
- Impact: Missing or stale course rows with no operational failure signal.
- Suggested Fix: Let exceptions fail the chunk, or configure explicit retry/skip policy and surface skip counts.
- Example Code: `throw e;`
- Risk of Fix: Medium; currently hidden bad inputs will fail jobs.
- Related Issues: None found
- Priority: P1
- Status: Open

### ARCH-001
- Category: Architecture
- Severity: High
- Location: `server/src/main/java/bhoon/sugang_helper/course/application/CourseService.java:10`; `server/src/main/java/bhoon/sugang_helper/dashboard/application/DashboardService.java:4`; `server/src/main/java/bhoon/sugang_helper/admin/application/AdminService.java:6`
- Description: Application services depend on presentation DTOs.
- Evidence: Application services import request/response types from `presentation` packages across multiple modules.
- Why: Use cases become coupled to HTTP representation and API response shape.
- Impact: API DTO changes force service changes and reduce reuse/testability.
- Suggested Fix: Introduce application command/query/result types and map to presentation DTOs in controllers.
- Example Code: `CourseSearchResult result = courseService.search(command);`
- Risk of Fix: Medium; mapping and tests need migration.
- Related Issues: `ISSUE-001-API`, `ISSUE-006-API-PLAN`, `ISSUE-010-API`
- Priority: P1
- Status: Open

### ARCH-002
- Category: Architecture / Frontend
- Severity: High
- Location: `web/src/shared/api/client.ts:3`; `web/src/features/auth/store/useAuthStore.ts:3`; `web/src/features/user/api/user.api.ts:1`; `web/eslint.config.mjs:68`
- Description: Frontend dependency boundaries are violated and the lint gate is red.
- Evidence: Shared API client imports a feature store, creating a `shared -> feature -> shared` cycle; lint fails with `boundaries/dependencies`. Feature modules also import other feature modules despite configured rules.
- Why: Shared transport should not own feature state or auth behavior.
- Impact: Circular initialization risk, brittle auth refresh behavior, and architectural rules that no longer protect coupling.
- Suggested Fix: Keep `shared/api/client.ts` transport-only and register auth side effects from app/feature layer. Move cross-feature orchestration to `app` or `widgets`.
- Example Code: `registerAuthFailureHandler(() => authStore.logout())`
- Risk of Fix: Medium; auth refresh/logout regression coverage is required.
- Related Issues: `ISSUE-028`, `ISSUE-032`
- Priority: P1
- Status: Open

### API-001
- Category: API Consistency
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/common/response/CommonResponse.java:16`; `server/src/main/java/bhoon/sugang_helper/feedback/presentation/FeedbackController.java:42`; `server/src/main/java/bhoon/sugang_helper/schedule/presentation/ScheduleController.java:26`; `web/src/features/schedule/hooks/useSchedules.ts:12`
- Description: Success response envelopes are inconsistent across `/api/v1`.
- Evidence: Some controllers return `CommonResponse<T>`; others return raw DTOs, `Page`, `List`, or IDs. Frontend modules encode both shapes.
- Why: API consumers must special-case response parsing.
- Impact: Contract drift, harder client generation, and brittle shared error/response handling.
- Suggested Fix: Standardize all JSON success responses or document raw exceptions and isolate them in API adapters.
- Example Code: `return CommonResponse.ok(data, "조회했습니다.");`
- Risk of Fix: Medium; backend and frontend must migrate together.
- Related Issues: `ISSUE-001-API`, `ISSUE-006-API-PLAN`, `ISSUE-010-API`
- Priority: P2
- Status: Open

### API-002
- Category: API Consistency
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/common/response/ErrorResponse.java:18`; `server/src/main/java/bhoon/sugang_helper/common/security/exception/CustomAuthenticationEntryPoint.java:31`; `server/src/main/java/bhoon/sugang_helper/common/security/exception/CustomAccessDeniedHandler.java:31`
- Description: MVC errors and Spring Security auth errors have different JSON shapes.
- Evidence: Runtime exception handler returns `ErrorResponse` with timestamp/details; security handlers manually write a smaller map.
- Why: Auth failures should use the same error contract as application failures.
- Impact: Clients and generated schemas cannot rely on consistent error fields.
- Suggested Fix: Reuse `ErrorResponse` or a shared error writer in both security handlers.
- Example Code: `objectMapper.writeValue(response.getWriter(), ErrorResponse.of(...));`
- Risk of Fix: Low; clients reading `message` should continue working.
- Related Issues: `ISSUE-001-API`, `ISSUE-010-API`
- Priority: P2
- Status: Open

### API-003
- Category: API Contract
- Severity: Medium
- Location: `web/src/features/timetable/api/timetable.api.ts:52`; `web/src/features/wishlist/api/wishlist.api.ts:9`; `server/src/main/java/bhoon/sugang_helper/timetable/presentation/TimetableController.java:149`; `server/src/main/java/bhoon/sugang_helper/wishlist/presentation/WishlistController.java:41`
- Description: `courseKey` path encoding is inconsistent.
- Evidence: Some frontend API modules call `encodeURIComponent(courseKey)`; timetable removal and wishlist toggle interpolate `courseKey` directly into path variables.
- Why: Path parameters must be encoded consistently.
- Impact: Reserved URL characters can route incorrectly or mutate the wrong identifier. Current key format appears colon-based, so slash-containing keys need confirmation.
- Suggested Fix: Encode path variables consistently, or send arbitrary keys in body/query.
- Example Code: ``api.delete(`/api/v1/wishlist/${encodeURIComponent(courseKey)}`)``
- Risk of Fix: Low; verify Spring decoded slash behavior separately.
- Related Issues: None found
- Priority: P2
- Status: Open

### PERF-001
- Category: Performance
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/course/infra/CourseJpaRepositoryImpl.java:62`; `:91`; `server/src/main/resources/db/migration/V1__initial_schema.sql:106`
- Description: Course search lacks supporting indexes for common filters and ordering.
- Evidence: Search filters by year, semester, classification, department, disclosure, status, and sorts with offset/limit. `courses` has only primary key and `course_key` uniqueness in V1.
- Why: Table scan and filesort risk grows with course history.
- Impact: Slow search during peak registration periods.
- Suggested Fix: Capture `EXPLAIN ANALYZE` for real queries and add composite indexes for dominant paths; consider full-text/search-specific columns.
- Example Code: `CREATE INDEX idx_courses_term_disclosure_name ON courses (academic_year, semester, disclosure, name, course_key);`
- Risk of Fix: Medium; index maintenance cost and migration locks.
- Related Issues: `ISSUE-027-SEARCH`
- Priority: P2
- Status: Open

### PERF-002
- Category: Performance / Batch
- Severity: Medium
- Location: `server/src/main/resources/application.yml:117`; `server/src/main/java/bhoon/sugang_helper/crawling/config/SpringBatchConfig.java:67`; `:91`
- Description: Minute-level crawling performs per-course lookup/save work.
- Evidence: Default cron is every minute; reader loads full XML into memory; writer calls `findByCourseKey` per item then save/update.
- Why: O(N) DB reads and writes per crawl puts load on DB and heap.
- Impact: Crawling can compete with user-facing traffic and grow expensive with course count.
- Suggested Fix: Bulk-load existing courses by key per chunk, diff in memory, persist changed rows in batches, and tune cron by peak/non-peak windows.
- Example Code: `Map<String, Course> existing = repository.findByCourseKeyIn(keys)...`
- Risk of Fix: Medium; event ordering and notification regressions must be tested.
- Related Issues: `ISSUE-017`, `ISSUE-018`
- Priority: P2
- Status: Open

### PERF-003
- Category: Performance / API
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/notification/presentation/NotificationController.java:59`; `server/src/main/java/bhoon/sugang_helper/course/application/CourseService.java:60`
- Description: Notification history and course seat history APIs return unbounded lists.
- Evidence: Notification history and course history repository methods return full `List` ordered by created time without pagination.
- Why: History data grows over time.
- Impact: Large responses, memory pressure, and slow sorting without `(key, created_at)` indexes.
- Suggested Fix: Add `Slice/Page` APIs, retention policy, and composite indexes on `(user_id, created_at)` and `(course_key, created_at)`.
- Example Code: `Slice<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);`
- Risk of Fix: Medium; frontend list consumers need pagination.
- Related Issues: `ISSUE-012-NOTIFICATION`
- Priority: P2
- Status: Open

### PERF-004
- Category: Performance / Timetable
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/timetable/presentation/TimetableDetailResponse.java:33`; `server/src/main/java/bhoon/sugang_helper/timetable/presentation/CustomScheduleResponse.java:50`
- Description: Timetable detail can lazy-load custom schedule times per schedule.
- Evidence: Response mapping walks custom schedules and their `times`; no dedicated fetch join/entity graph was found for that aggregate.
- Why: Detail APIs should load bounded aggregate data predictably.
- Impact: N+1 query growth as custom schedules increase.
- Suggested Fix: Add a detail-specific repository method with fetch graph or split prefetch query for custom schedules and times.
- Example Code: `@EntityGraph(attributePaths = {"customSchedules", "customSchedules.times"})`
- Risk of Fix: Medium; multiple collection fetches can create row duplication.
- Related Issues: None found
- Priority: P2
- Status: Open

### WEB-001
- Category: Next.js / SEO / Performance
- Severity: Medium
- Location: `web/src/app/layout.tsx:58`; `web/src/app/(main)/announcements/[id]/page.tsx:1`; `web/src/app/(main)/courses/[courseKey]/page.tsx:1`
- Description: Public pages lose static rendering and detail pages miss server-rendered metadata.
- Evidence: Root layout reads cookies, making the app tree dynamic. Course and announcement detail pages are client components and no `generateMetadata` was found.
- Why: Public discovery pages should preserve App Router caching and SEO where possible.
- Impact: Weaker caching, slower first content, weaker social previews and search metadata.
- Suggested Fix: Move auth-dependent header state out of root layout or split route groups; convert SEO-visible details to server components with `generateMetadata`.
- Example Code: `export async function generateMetadata({ params }) { ... }`
- Risk of Fix: Medium; auth header hydration and interactive child components need regression coverage.
- Related Issues: None found
- Priority: P2
- Status: Open

### WEB-002
- Category: Frontend Security / PWA
- Severity: Medium
- Location: `web/public/sw.js:28`; `:53`; `:73`; `web/public/manifest.json:13`
- Description: PWA assets and notification click handling are unsafe/inaccurate.
- Evidence: Service worker opens push payload `data.url` without same-origin/path allowlisting. Manifest declares `192x192` and `512x512` icons but asset is `500x536`.
- Why: Push payloads should not control arbitrary external navigation; manifest icon sizes should match real assets.
- Impact: Compromised payload can open external URLs; PWA install/icon quality can degrade.
- Suggested Fix: Allow only same-origin relative paths and generate correct square icon assets.
- Example Code: `const safePath = url.origin === self.location.origin ? url.pathname : "/";`
- Risk of Fix: Low.
- Related Issues: None found
- Priority: P2
- Status: Open

### WEB-003
- Category: Accessibility / UX Correctness
- Severity: Medium
- Location: `web/src/app/(main)/announcements/[id]/page.tsx:83`; `web/src/features/admin/components/admin-dday-panel.tsx:344`; `web/src/features/admin/components/admin-schedule-panel.tsx:138`
- Description: Some interactive controls are inaccessible or have incorrect edit state.
- Evidence: Icon-only buttons lack accessible labels; announcement share button appears interactive without action; schedule edit path comments say custom type but sets `setIsCustomType(false)`.
- Why: Controls must be discoverable to assistive tech and edit state must match existing data.
- Impact: Screen reader users cannot identify actions; custom schedule type editing can be confusing or impossible.
- Suggested Fix: Add `aria-label`/`title`, remove or implement inert buttons, and correct custom schedule edit state.
- Example Code: `<Button aria-label="공지 공유" onClick={handleShare}>`
- Risk of Fix: Low.
- Related Issues: None found
- Priority: P2
- Status: Open

### INFRA-001
- Category: Infrastructure / Deployment
- Severity: High
- Location: `server/docker-compose.yml:17`; `server/.github/workflows/cd.yml:38`; `server/.gitignore:49`
- Description: Deployment references a Firebase key path that CD does not deliver.
- Evidence: Compose mounts `./src/main/resources/firebase/firebase-key.json`; CD copies jar, prod yml, Dockerfile, `.dockerignore`, and compose only; Firebase resource files are ignored.
- Why: Secret files should be provisioned consistently outside source tree.
- Impact: Fresh deploy can fail or boot without FCM credentials unless manually pre-provisioned.
- Suggested Fix: Mount a stable secret path such as `./secrets/firebase-key.json` and add deploy preflight checks.
- Example Code: `./secrets/firebase-key.json:/app/config/firebase-key.json:ro`
- Risk of Fix: Medium; one-time server secret migration required.
- Related Issues: `ISSUE-030`
- Priority: P1
- Status: Open

### INFRA-002
- Category: Infrastructure / Reliability
- Severity: Medium
- Location: `infra/docker-compose.yml:53`; `infra/grafana/provisioning/dashboards/dashboard.yml:10`; `infra/docker-compose.yml:36`
- Description: Observability deployment is not fully durable or reproducible.
- Evidence: Grafana has no `/var/lib/grafana` data volume while UI updates are enabled; infra images use mutable `latest` tags.
- Why: Runtime state and image versions should be reproducible.
- Impact: Grafana users/preferences/UI edits can disappear; container recreation can silently upgrade proxy/observability components.
- Suggested Fix: Add a host-backed Grafana data volume or disable UI edits; pin image versions/digests.
- Example Code: `image: grafana/grafana:12.1.1`
- Risk of Fix: Low to Medium; permissions and upgrade cadence need ownership.
- Related Issues: `ISSUE-011`, `ISSUE-026`
- Priority: P2
- Status: Open

### INFRA-003
- Category: Infrastructure / Logging
- Severity: Medium
- Location: `infra/docker-compose.yml:113`; `infra/scripts/verify-log-policy.sh:11`; `web/scripts/start-with-file-log.mjs:6`; `web/README.md:113`
- Description: Log retention policy is only partially enforced.
- Evidence: Promtail mounts `/var/log` read-write. Verification script checks DB binlog/json-file options but not Loki/Promtail host mounts or positions. Web README documents `/var/log/.../web.log`, but wrapper defaults to `./build/logs/web/web.log` and no web deploy unit exists.
- Why: Log retention is a documented product requirement.
- Impact: Promtail can modify host logs; mount regressions can pass checks; web logs may not be retained or scraped.
- Suggested Fix: Mount only required log paths read-only, expand verify script, and add/document the actual web deployment with `WEB_LOG_FILE`.
- Example Code: `/var/log/jbnu-sugang-helper:/var/log/jbnu-sugang-helper:ro`
- Risk of Fix: Low to Medium; system log scraping paths may need explicit mounts.
- Related Issues: `ISSUE-005`, `ISSUE-011`, `ISSUE-013`, `ISSUE-026`
- Priority: P2
- Status: Open

### TEST-001
- Category: Testing / CI
- Severity: High
- Location: `server/.github/workflows/cd.yml:22`; `web/package.json:5`
- Description: Deployment skips tests and no web/infra CI gate is present in the checked tree.
- Evidence: CD runs `./gradlew clean build -x test`; only server CD workflow was found. Web tests pass locally but lint fails.
- Why: Passing local tests do not protect deployment if CI bypasses them.
- Impact: Broken server tests, web lint failures, and infra config regressions can ship.
- Suggested Fix: Add required CI jobs for server test/build, web `npm ci`, test, lint, build, and infra compose/script checks before deploy.
- Example Code: `run: ./gradlew clean test --no-daemon`
- Risk of Fix: Medium; CI workdirs and secrets need setup.
- Related Issues: None found
- Priority: P1
- Status: Open

### TEST-002
- Category: Testing / Security
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/common/config/SecurityConfig.java:89`; `server/src/test/java/bhoon/sugang_helper/common/security/jwt/JwtAuthenticationFilterTest.java`
- Description: Request-level authorization and migration paths lack integration coverage.
- Evidence: Security tests are unit-level filter/provider tests; no request-level `MockMvc`/`WebMvcTest` coverage was found. Flyway is disabled in tests.
- Why: Security rules and production schema are cross-cutting behavior.
- Impact: Permit-all/admin boundary and migration regressions can pass service/unit tests.
- Suggested Fix: Add representative Spring Security request tests and Testcontainers/Flyway schema tests.
- Example Code: `mockMvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isForbidden());`
- Risk of Fix: Medium; context setup and test runtime increase.
- Related Issues: `ISSUE-033`
- Priority: P2
- Status: Open

### TEST-003
- Category: Testing / Quality Gate
- Severity: Medium
- Location: `web/src/shared/api/client.ts:3`; `web/src/widgets/header/header.test.tsx:12`; `server/src/test/java/bhoon/sugang_helper/logging/LogbackFileAppenderTest.java:35`
- Description: Current web lint is red and one server logging test uses timing sleep.
- Evidence: `npm run lint` failed on boundary and `any` errors; `LogbackFileAppenderTest` uses fixed `Thread.sleep(100)`.
- Why: Tests passing is not equivalent to a clean quality gate; timing sleeps create flaky CI risk.
- Impact: CI cannot be made required without either failing now or ignoring lint; logging test can intermittently fail.
- Suggested Fix: Fix lint errors, type test mocks, and replace sleep with deterministic flush/polling.
- Example Code: `await().atMost(Duration.ofSeconds(2)).untilAsserted(...)`
- Risk of Fix: Low.
- Related Issues: None found
- Priority: P2
- Status: Open

### MAINT-001
- Category: Maintainability
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/course/infra/CourseJpaRepositoryImpl.java:42`; `server/src/main/java/bhoon/sugang_helper/notification/application/NotificationService.java:37`; `web/src/app/(user)/settings/page.tsx:53`; `web/src/app/(user)/timetable/page.tsx:57`
- Description: Several core modules suppress complexity or combine orchestration, policy, and rendering responsibilities.
- Evidence: Search repository suppresses `TooManyMethods` and `CyclomaticComplexity`; NotificationService suppresses `TooManyMethods`; settings and timetable route pages hold many unrelated workflows.
- Why: Complexity hotspots concentrate regression risk in product-critical areas.
- Impact: Slower feature work and harder targeted tests for search, notification, and user settings.
- Suggested Fix: Extract search predicate/sort builders, channel handlers, and route-level hooks/section components behind existing behavior.
- Example Code: `CourseSearchPredicates.from(criteria)`
- Risk of Fix: Medium; behavior-preserving tests are needed.
- Related Issues: `ISSUE-012`, `ISSUE-027`, `ISSUE-029`
- Priority: P3
- Status: Open

### CONS-001
- Category: Consistency
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/timetable/presentation/CustomScheduleRequest.java:24`; `server/src/main/java/bhoon/sugang_helper/crawling/application/CourseCrawlerTargetService.java:32`
- Description: Validation and transaction conventions drift in localized paths.
- Evidence: Custom schedule list uses `@NotNull` despite requiring at least one schedule and lacks nested cascade validation. Crawler target GET-style methods create defaults and run write transactions.
- Why: Requests should fail early with validation errors, and reads should not mutate hidden state.
- Impact: Persistence errors instead of 400 responses; surprising DB writes during GET calls.
- Suggested Fix: Add `@NotEmpty` and nested `@Valid`; initialize crawler settings via migration/startup/admin PUT and keep GET read-only.
- Example Code: `private List<@Valid CustomScheduleTimeRequest> schedules;`
- Risk of Fix: Low to Medium; first-run default behavior must be preserved.
- Related Issues: None found
- Priority: P2
- Status: Open

### CONS-002
- Category: Consistency / Read Side Effects
- Severity: Medium
- Location: `server/src/main/java/bhoon/sugang_helper/crawling/application/CourseCrawlerTargetService.java:32`; `:41`; `:63`; `:112`; `server/src/main/java/bhoon/sugang_helper/course/presentation/CourseController.java:122`
- Description: GET-style crawler setting reads can create database rows.
- Evidence: `getCurrentTarget`, `getSearchDefaultSemester`, and `getCurrentTargetValue` are write transactions and call `getOrInitSetting`, which saves a default row when missing.
- Why: Read endpoints should not mutate hidden state unless explicitly documented.
- Impact: Unexpected writes during GET requests and possible first-read race behavior.
- Suggested Fix: Initialize defaults via migration/startup/admin command and keep read methods read-only.
- Example Code: `INSERT INTO crawler_settings ...` in migration or guarded startup initializer.
- Risk of Fix: Medium; first-run default behavior must be preserved.
- Related Issues: None found
- Priority: P2
- Status: Open

### DOC-001
- Category: Documentation / Repository Governance
- Severity: Medium
- Location: `docs/ARD.md:9`; `docs/PRD.md:9`; workspace root
- Description: Durable architecture docs and repository topology do not match the actual system.
- Evidence: ARD lists language/framework/package manager as unknown and PRD focuses on log retention, while the workspace contains Spring Boot, Next.js, and infra. Root is not a Git repository; `server`, `web`, and `infra` are independent nested Git repositories.
- Why: Durable docs and version control boundaries are the source of truth for cross-stack decisions.
- Impact: Cross-stack changes, docs, and issue state cannot be atomically versioned from the workspace root.
- Suggested Fix: Create project-level architecture docs and formalize the repo model as monorepo, submodules, or pinned multi-repo workspace.
- Example Code: Not applicable; governance decision required.
- Risk of Fix: Medium; history and deployment workflow migration may be needed.
- Related Issues: `ISSUE-011`, `ISSUE-013`
- Priority: P3
- Status: Open
