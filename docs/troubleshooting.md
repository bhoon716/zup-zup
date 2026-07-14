# Troubleshooting

This document merges the web, server, and operational troubleshooting notes into one continuous record.

## Evidence and Performance Notes

### Evidence Summary

Use this guide when a reviewer needs the smallest useful summary before opening a large raw artifact.

### Summary
Capture the scenario, baseline, after/result, workload or iterations, key metric, raw report path, limitations, and follow-up issue if any. Keep the summary short enough that a reviewer can decide quickly.

### Required Fields
- Scenario: TODO
- Before / baseline: TODO
- After / result: TODO
- Workload / iterations: TODO
- Key metric: TODO
- Raw report path: TODO
- Limitations: TODO
- Follow-up issue: TODO

### Review Rules
- Keep the summary compact and link the raw artifact instead of pasting a huge log.
- Large raw evidence alone must not fail review when the compact summary is present.
- Record the user-visible conclusion separately from the raw benchmark or profiling output.

### When To Use
- Use this alongside performance evidence, benchmark notes, and any review that would otherwise produce oversized raw output.
- Use it for any evidence bundle where the compact summary should be the primary review surface.

### Related Files
- [Performance Improvements](#performance-improvements)


### Performance Improvements

Use this guide when a request is slow, a benchmark is noisy, or the before/after comparison is hard to trust.

### Summary
Keep the performance workflow repeatable: capture a baseline, rerun the same workload after the change, compare the result, write a compact summary, and document any measurement limits.

### Baseline
- Record the starting metric, workload, and environment.
- Capture the exact command or benchmark that produced the baseline.

### Compact Summary
- Record the scenario, baseline, after/result, workload or iterations, key metric, raw report path, limitations, and follow-up issue if any.
- Keep the summary short enough that a reviewer can judge the result without opening the raw artifact first.

### Measurement
- Run the same workload after the change.
- Use the same metric whenever possible.
- Note any differences in machine, cache state, sample size, or input data.

### Troubleshooting
- If the result is noisy, rerun the measurement and record the noise source.
- If the metric is unclear, narrow the request to one user flow or endpoint.
- If the raw output is large, link it and keep the compact summary as the primary review surface.
- If the change is durable, record the approval in this document's evidence notes and the relevant issue or decision log.

### Evidence
- Attach the baseline output, the follow-up output, the comparison summary, and the raw report path.
- Link to the relevant source files or commands that prove the change.

### Related Files
- [Evidence Summary](#evidence-summary)

### Flyway migration task split (2026-07-13)

- Scenario: Flyway fresh schema/checksum/upgrade validation started two MySQL Testcontainers during every default server test run.
- Result: `./gradlew test` now excludes the `migration` tag (238 tests, 6.236s reported test duration); `./gradlew migrationTest` runs the two migration checks separately (18.261s reported test duration, no skips/failures).
- CI: PR and main server verification run `check migrationTest` after `docker info`; unavailable Docker is a visible failure rather than a skipped migration check.
- Evidence: `apps/server/build/reports/tests/test/index.html` and `apps/server/build/reports/tests/migrationTest/index.html` from the local verification run.
- Limitation: the two tests retain isolated MySQL containers. Static/reusable containers could save one startup but need explicit schema isolation and local Testcontainers reuse configuration, so they are deferred.

### Redis JWT opaque storage and rotation (2026-07-13)

- Scenario: Redis blacklist key and refresh registry value contained raw JWTs, and Redis-backed HTTP session also stored raw access/refresh JWT attributes.
- Result: new blacklist entries use `BL:<SHA-256>`; new refresh registry entries use `v2:<random-family>:<SHA-256>`. Redis Lua compare-and-set makes rotation single-use, and a replay in the same family revokes that family. Spring Security stores only the authenticated subject, authorities, access expiry epoch, and immutable user ID in the session; expiry or inactive-account detection clears the session and returns to the refresh path.
- Migration: no new raw data is written. To prevent a pre-deployment email-only token from becoming a rejoined account, tokens without the immutable `uid` claim and sessions without a user ID are fail-closed; users authenticate once again. Unused legacy Redis values expire within their existing TTL.
- Restart behavior: missing refresh registry state is rejected without minting a new token. Redis is now persisted by ISSUE-098; the durability and recovery boundary is recorded below.
- Evidence: `JwtProviderTest`, `AuthServiceTest`, `JwtAuthenticationFilterTest`, `OAuth2SuccessHandlerTest`, `SecurityRequestAuthorizationTest`, and `RedisServiceTest`; default `./gradlew clean test` passed.

### Redis readiness and AOF recovery (2026-07-13)

- Scenario: Compose only waited for a running Redis container, while the manually created Lettuce factory used long defaults and Redis restart discarded session, refresh, logout blacklist, rate-limit, and dedupe state.
- Readiness: Redis uses an authenticated `PING` healthcheck; app startup waits for `service_healthy`. `/actuator/health/readiness` includes `readinessState`, DB, and Redis, so Redis failure returns `DOWN/503` without exposing details. The client has 2-second connect/command limits, reconnects after recovery, and rejects commands while disconnected instead of queueing stale work.
- Persistence policy: Redis stores its full `/data` directory in `/var/lib/jbnu-sugang-helper/redis` with AOF `appendfsync everysec`. A clean Redis/container restart preserves state. A sudden host or storage failure can lose at most about one second of acknowledged writes; a missing refresh record remains fail-closed. `appendfsync always` was not selected because every Redis write would synchronously flush storage.
- Recovery: `backup-redis-state.sh` stops a running Redis instance before archiving the complete AOF directory and SHA-256 sidecar, then restores the original service. `restore-redis-state.sh` validates the checksum and archive paths, stages the replacement, rolls back to the prior directory if startup fails, and retains the prior directory after a successful restore until a root operator removes it. The isolated Docker drill writes a sentinel, simulates lost state, restores it, and verifies the sentinel.
- Alert boundary: a failed Redis preflight blocks server deployment and uses the existing deployment Discord failure channel. The readiness health signal and restart smoke are actionable now; continuous Prometheus/Alertmanager routing is deliberately completed in ISSUE-099, which owns the durable alert route.
- Evidence: `RedisConfigTest`, `RedisReadinessHealthTest`, `SecurityRequestAuthorizationTest`, `verify-compose-policy.sh`, `test-redis-state-recovery.sh`, and `test-deploy-app.sh`.

### DB least privilege and durable-state disaster recovery (2026-07-13)

- Scenario: the application had access to MySQL root credentials and only logs/Loki/Promtail had a backup path. Database rows, feedback attachments, Grafana state, and Nginx Proxy Manager certificates could not be recovered as one tested point.
- Result: runtime receives only DML privileges; a pinned Flyway 11.7.2 one-shot container owns DDL during deployment, and the backup account is local to the DB container. Deployment preflights DB health, idempotently provisions existing volumes, migrates, and only then replaces the app. The runtime app has neither root nor migrator secret.
- Recovery: the root-only archive is AES-256-CBC/PBKDF2 encrypted, SHA-256 and HMAC-SHA-256 checked, retains 14 daily recovery points, and includes a logical `--source-data=2` dump, snapshot-time row-format binlogs, uploads, Grafana, and NPM state/certificates. It intentionally pauses writers for a coherent DB/file boundary, rejects tar links/devices/FIFOs before extraction, checks the manifest database before destructive work, and leaves writers stopped if DB rollback cannot be verified.
- Evidence: `test-db-service-accounts.sh` runs all Flyway migrations under the migrator and proves runtime DDL denial; `test-dr-state-recovery.sh` destroys MySQL and all host-state sentinels before restoring identity, subscription, feedback attachment, Grafana, NPM, certificates, and archived binlogs. The drill is in infra CI and runs every Monday.
- Boundary: a database fixture can prove restored local identity state but cannot exercise Google OAuth without a dedicated external callback/client. A controlled Google login remains required after a real-host restore. The current scope is a consistent full snapshot, not PITR: post-snapshot binlogs and attachment versions are not continuously exported together. Full policy, passphrase ownership, retention, and recovery limits are in [disaster-recovery-policy.md](disaster-recovery-policy.md).

### Account withdrawal soft delete and identity binding (2026-07-13)

- Scenario: account withdrawal physically deleted user-owned rows, while a still-valid email-only token or session could be confused with a later account using the same Google email.
- Result: withdrawal immediately clears device/refresh/session credentials and direct identifiers, disables subscriptions and notification channels, soft-deletes the user and feedback, and retains non-identifying history. Access/refresh tokens and HTTP sessions now require the immutable user ID together with the active email; the former account cannot authenticate a rejoined account.
- Schema: `users.deleted_at` and immutable `users.id` already exist in V1. V17 adds a non-null `version` defaulted to `0` for existing rows so a stale profile update cannot restore a withdrawn account; the migration suite verifies the upgrade path.
- Operational policy: automatic purge is intentionally disabled until retention period and legal basis are reviewed. The compact preservation table and runbook are in [account-withdrawal-retention.md](account-withdrawal-retention.md).
- Evidence: `UserServiceTest`, `UserDataConstraintTest`, `JwtProviderTest`, `JwtAuthenticationFilterTest`, `AuthServiceTest`, `CustomOAuth2UserServiceTest`, `NotificationChannelPolicyTest`, and `SecurityRequestAuthorizationTest`.

### Structured administrator audit logs (2026-07-13)

- Scenario: reply audit metadata was hand-built JSON that could fail on quotes or newlines and could duplicate reply bodies in the database and backups.
- Result: metadata now uses the `admin-action` v1 envelope. Status changes keep only the before/after status; reply changes keep only length and an HMAC-SHA-256 fingerprint; attachment access keeps only the feedback ID. The admin-only paged endpoint never returns a `User`, email, or raw metadata.
- Migration and retention: V18 rewrites reply metadata as `LEGACY_SANITIZED`, while whitelisted legacy enum status transitions become v1 `STATUS_CHANGED`, before making metadata non-null and indexing `(created_at, id)`. A configurable 180-day scheduled cleanup applies only to this minimized audit data; feedback bodies and attachments remain under the separate no-auto-purge policy.
- Evidence: `AdminAuditServiceTest`, `AdminAuditLogRetentionSchedulerTest`, `FeedbackServiceTest`, `SecurityRequestAuthorizationTest`, and `FlywayMigrationValidationTest`; targeted server tests, static analysis, and `./gradlew migrationTest` passed.
- Connection: DLQ 선택 replay는 이제 성공한 상태 전이와 같은 transaction에서 `DELIVERY_REPLAY`를 기록한다. 원본 key·수신자·provider 예외 원문은 감사 로그에 남기지 않는다.

### Notification delivery idempotency and DLQ replay (2026-07-13)

- Scenario: outbox delivery는 retry 상태만 가져 provider 결과가 불확실한 재시도에서 같은 delivery를 식별하거나 운영자가 안전하게 DLQ를 재처리할 수 없었다.
- Result: V19는 delivery마다 stable UUID key, claim token, DLQ 전환 시각, optimistic version을 추가한다. provider 호출은 transaction 밖에서 하고 token이 같은 worker만 결과를 정산한다. provider에는 짧은 불투명 key를 전파하며, Web Push service worker는 같은 key를 notification tag로 사용한다.
- Operations: `ROLE_ADMIN`은 ownership 제한 없이 전체 DLQ 목록과 안전한 상세를 보고, DLQ 하나만 같은 key로 replay한다. `SENT`는 API의 명시 override 외에는 거부하며, 성공 replay는 최소화된 `DELIVERY_REPLAY` 감사 로그에 남는다. DLQ는 실제 전환 시각부터 최소 30일 보존한다.
- Evidence: `AdminNotificationDeliveryServiceTest`, `AdminNotificationDeliveryReplayConcurrencyTest`, `SeatNotificationDeliverySettlementServiceTest`, `NotificationDeliveryRetentionSchedulerTest`, `NotificationSenderIdempotencyContractTest`, `SecurityRequestAuthorizationTest`, `FlywayMigrationValidationTest`, 관리자 delivery UI/SW Vitest; `./gradlew check`, `./gradlew migrationTest`, web lint/build이 통과했다.
- Limitation: delivery가 여러 target으로 fan-out된 뒤 일부 target만 실패하면 이미 성공한 target도 다시 요청될 수 있다. ISSUE-089에서 provider deadline·circuit breaker·실패 분류를 적용했으며, bounded fan-out과 5초 SLA는 ISSUE-087의 잔여 범위다.

## Provider 알림 장애 대응 (ISSUE-089)

알림 provider 호출은 `APP_NOTIFICATION_PROVIDER_TIMEOUT_MS` 기본 5초의 상한을 가지며, Discord의 connect/read timeout과 SMTP의 connection/read/write timeout도 별도로 설정한다. 일시 장애·rate limit·timeout은 outbox 재시도 대상으로 분류하고, 잘못된 Web Push 구독 같은 영구 실패는 즉시 DLQ로 보낸다. provider별 회로 차단은 `APP_NOTIFICATION_PROVIDER_CIRCUIT_FAILURE_THRESHOLD`와 `APP_NOTIFICATION_PROVIDER_CIRCUIT_OPEN_SECONDS`로 제어한다. `notification.provider.latency`, `notification.provider.timeout`, `notification.provider.http.status`, `notification.provider.circuit.open`, `notification.provider.failures` metric으로 상태를 확인한다.

## 알림 fan-out SLA (ISSUE-087)

Outbox worker는 기본 8개 thread와 32개 bounded queue로 FIFO claim을 병렬 처리한다. `APP_NOTIFICATION_OUTBOX_WORKER_THREADS`와 `APP_NOTIFICATION_OUTBOX_QUEUE_CAPACITY`로 용량을 조정하며, permit이 모두 사용되면 새 row를 claim하지 않아 pending delivery가 유실되지 않는다. `notification.outbox.claim.lag`, `notification.outbox.claim_to_attempt`, `notification.outbox.queue.depth`를 channel 태그로 조회해 커밋 이후 claim 지연과 첫 시도 지연을 확인한다.

## Crawler chunk 비용 (ISSUE-096)

크롤러 writer는 chunk의 course key를 `findByCourseKeyIn`으로 한 번에 조회해 course별 N+1 select를 제거한다. seat-open event와 seat history 조건은 유지하며, `crawler.course.chunk.write` timer와 `crawler.course.chunk.items` counter로 chunk 처리 시간과 처리량을 확인한다.

## Observability durability and alert route (ISSUE-099)

Prometheus TSDB는 `/var/lib/jbnu-sugang-helper/prometheus`에 보존되며 30일 또는 20GB 중 먼저 도달하는 기준으로 정리된다. Alertmanager는 `/var/lib/jbnu-sugang-helper/alertmanager`에 상태를 보존하고 Prometheus의 SLO·DLQ·provider circuit·crawler freshness alert를 단일 webhook route로 전달한다. `ALERTMANAGER_WEBHOOK_URL`은 운영 전 검토된 관리자/Discord webhook으로 설정해야 하며, Grafana의 Notification and Crawler SLO dashboard는 관리자 페이지에서 동일 metric을 확인하는 보조 경로다. webhook 장애 시 Prometheus alert와 관리자 dashboard/log가 남고, `send_resolved`로 복구 알림 중복을 줄인다. `infra/scripts/test-observability-smoke.sh`는 compose recreate 전 설정·volume·route smoke를 검증한다.

## Single-host resource limits (ISSUE-101)

Compose는 모든 서비스에 CPU·memory·pids 상한을 둔다. 앱은 Spring graceful shutdown(30초) 후 Compose가 재시작하며, notification worker는 8개 thread/32개 queue 상한으로 앱 2 CPU·1.5GiB 예산 안에서 동작한다. 제한 변경은 `infra/scripts/verify-compose-policy.sh`에서 실패하며, 배포 전 `test-observability-smoke.sh`로 config를 재생성해 limit과 healthcheck를 함께 확인한다. 제한 초과 시 데이터는 outbox/Redis/DB persistent state에 남고 재시작 후 lease/retry가 이어진다.

## Pageable request limits (ISSUE-093)

Page size는 100, offset은 10,000까지 허용한다. course/history/review/feedback/admin audit·DLQ·notification history endpoint의 초과 요청은 `G002` 400으로 거부되며, 응답에는 내부 query나 입력 원문을 넣지 않고 `PAGEABLE_LIMIT` reason만 로그에 남긴다.

## Crawler upstream/freshness (ISSUE-097)

운영 compose는 `oasis.jbnu.ac.kr`을 고정 IP로 재정의하지 않는다. API client는 timeout·연결 오류 같은 transport 실패만 재시도하고 malformed/permanent 오류는 즉시 실패시킨다. `crawler.runs{status}`, `crawler.upstream.latency`, `crawler.data.freshness.age.seconds`, `crawler.data.stale` metric을 확인한다. 기본 stale threshold는 15분이며 `JBNU_CRAWLER_STALE_THRESHOLD_MINUTES`로 조정할 수 있다. threshold 초과 시 `alert=ADMIN` 경고가 남고 관리자 dashboard의 지연 상태와 함께 확인한다.

### Withdrawn feedback administrator access (2026-07-13)

- Scenario: user withdrawal and manual feedback deletion hid feedback through Hibernate soft-delete restrictions, but the sole administrator still needed a narrow, auditable preservation view. The old manual delete also removed attached files and hard-deleted administrator replies.
- Result: a JDBC scalar read model provides an administrator-only active/deleted filter and detail view without re-enabling deleted data in ordinary JPA paths. JSON exposes generic author labels and attachment IDs only; deleted feedback omits environment metadata. The generic attachment endpoint rejects an administrator role, while the dedicated confirmed POST download resolves the file before recording the minimized attachment-access audit event.
- Evidence: `FeedbackServiceTest`, `JdbcAdminFeedbackReadRepositoryTest`, `SecurityRequestAuthorizationTest`, and `apps/web/src/features/feedback/hooks/useFeedback.test.tsx`.
- Limitation: this is a deliberate single-administrator policy, not an MFA or step-up authentication control. The accepted residual risk and no-auto-purge rule are in [account-withdrawal-retention.md](account-withdrawal-retention.md).

### Feedback metadata validation (2026-07-13)

- Scenario: `metaInfo` was an unchecked string written directly to MySQL JSON, while the browser collected a complete URL, user agent, platform, language, and timestamp. A malformed inner JSON value could fail late and an invalid multipart JSON was previously handled as a 500.
- Result: the server now accepts only optional textual `os` (128 characters) and `language` (35 characters) in a 512 UTF-8 byte JSON object. It rejects unknown/non-text/duplicate/trailing or malformed content before user lookup, rate-limit increments, file upload, and database save; unreadable multipart JSON returns `400/G002`. The browser sends only the permitted fields, and the administrator projection remains metadata-free.
- Evidence: `FeedbackMetadataNormalizerTest`, `FeedbackServiceTest`, `FeedbackControllerTest`, `GlobalExceptionHandlerTest`, `./gradlew check --no-daemon`, 167 Vitest tests, web lint, and production build.
- Limitation: old rows are intentionally not rewritten because they are not exposed through the administrator projection and no current UI parses them. The server remains the authoritative boundary for non-browser clients.

### Feedback attachment image normalization (2026-07-13)

- Result: multipart ingress allows a 10 MiB file and 11 MiB request envelope, mapping parser rejection to `F008/413`; the application retains its 10 MiB total attachment policy. Tika sniffing is followed by actual ImageIO decoding of static JPEG/PNG only; WebP is rejected before decoding. Each accepted source is limited to 4096 pixels per side, 8,388,608 pixels (about 32 MiB for a four-byte raster before decoder overhead), and a 5 MiB sanitized output. One unqueued decoder worker gives callers a 2-second budget. GIF and APNG are rejected; accepted pixel data is re-encoded as JPEG/PNG without original metadata, and the download filename is changed to that actual output extension.
- Evidence: `LocalFileUploadServiceTest` covers WebP rejection, PNG text and JPEG EXIF markers, dimension/pixel bombs, GIF/APNG, timeout, and failed-batch cleanup. `FeedbackServiceTest`, `GlobalExceptionHandlerTest`, and `SecurityRequestAuthorizationTest` cover normalized filename, parser error response, and bound ingress limits.
- Limitation: Java interruption cannot hard-kill every decoder implementation. On timeout the request fails and the single no-queue worker is interrupted, so a non-cooperative decoder can temporarily make later image requests fail fast. A hard process sandbox is deferred unless production measurements require it.

### Authenticated administrator attachment preview (2026-07-13)

- Scenario: the administrator-only attachment POST already returned an authenticated blob, but it was always `application/octet-stream` and the UI immediately downloaded it with no preview. A delayed request could also open a preview for feedback that was no longer selected.
- Result: the server sniffs stored bytes and labels only JPEG/PNG as previewable images; the controller retains attachment disposition and returns `application/octet-stream` for legacy WebP/GIF or unknown bytes. The single administrator's confirmed blob is reused for a dialog preview and extension-safe download. feedback–attachment pair lookup, 401/403 protection, and successful-access audit logging remain intact. Selection changes and unmounts discard late blobs before creating object URLs; active URLs are revoked on close.
- Evidence: `LocalFileUploadServiceTest`, `FeedbackServiceTest`, `SecurityRequestAuthorizationTest`, and the administrator feedback page Vitest regression. `./gradlew check --no-daemon`, 171 Vitest tests, web lint, and production build passed.
- Limitation: legacy files outside JPEG/PNG remain downloadable but intentionally have no inline preview. Playwright browser smoke and scheduled/manual server contract coverage are maintained in ISSUE-102.

## Critical flow E2E (ISSUE-102)

Pull request와 수동 실행에서는 Playwright Chromium smoke가 실제 Next.js 화면에서 세션 401→refresh→재시도와 관리자 feedback attachment preview를 mock API로 검증하고 `playwright-report` artifact를 남긴다. 주간 schedule 또는 수동 provider suite는 `AuthService`, provider resilience, outbox processor, DLQ replay, feedback controller 테스트를 별도 실행해 외부 secret 없이 운영 경계를 점검한다. 브라우저 smoke가 실패하면 artifact의 trace/screenshot/video를 먼저 확인하고, provider suite가 실패하면 서버의 `build/test-results/test` artifact에서 status/timeout/retry 원인을 확인한다.

### Web runtime and workspace lockfile policy (2026-07-13)

- Scenario: the web runtime used `next@16.3.0-preview.5`, while README named an older version and `eslint-config-next` was a different release. A direct switch to current stable `16.2.10` restored the vulnerable nested PostCSS `8.4.31` package.
- Result: matching Next and ESLint preview versions are explicitly pinned as a temporary production exception because they include PostCSS `8.5.10`. The root workspace lockfile is now the only lockfile, CI installs from the root, and root-declared React/React DOM/ESLint/TypeScript preserve peer resolution after a clean workspace install.
- Evidence: clean `npm ci`, 164 Vitest tests, lint (8 pre-existing warnings, no errors), production build, and `npm audit --omit=dev` with zero vulnerabilities.
- Limitation: `@emoji-mart/react` still declares React 18-only peer metadata. Lockfile updates require a reviewed `npm install --package-lock-only --force`; its picker test, clean install, lint, and build remain required. The stable transition condition and weekly audit are in [web-dependency-policy.md](web-dependency-policy.md).

## Web Troubleshooting

이 문서는 `web` 모듈 개발 중 발생한 문제와 기술적 해결책을 기록합니다.

---

## 1. Docker 환경에서의 Radix UI 모듈 인식 오류

### 문제 상황

Docker 환경에서 `next dev` 실행 시, `package.json`에 명시된 `@radix-ui/react-alert-dialog`, `@radix-ui/react-tabs` 등의 모듈을 찾을 수 없다는 `Module not found` 에러가 발생하며 빌드가 중단되었습니다. 로컬 환경에서는 정상 작동하나 컨테이너 내부의 `node_modules`에만 특정 패키지가 누락되는 현상이 반복되었습니다.

### 원인

1. **볼륨 마운트 충돌**: 로컬의 `node_modules`와 컨테이너 내의 `node_modules`가 도커 익명 볼륨 설정에 의해 꼬일 수 있음.
2. **Lockfile 불일치**: `package-lock.json`의 변경 사항이 컨테이너 빌드 시점에 완전히 반영되지 않거나 캐시된 이미지를 사용하여 구버전 의존성이 유지됨.

### 해결책

1. **수동 모듈 주입**: 가동 중인 컨테이너 내에서 `docker exec`를 통해 누락된 패키지를 강제로 재설치하여 런타임 오류를 즉시 해결.
   - `docker exec sugang-helper-web npm install @radix-ui/react-alert-dialog @radix-ui/react-tabs`
2. **의존성 잠금 동기화**: 로컬에서 `npm install`을 새로 수행하여 `package-lock.json`을 갱신하고, 이를 Docker 빌드 시 `npm ci` 과정에서 사용하도록 구성.
3. **볼륨 대피(Exclusion)**: `docker-compose.yml`에서 `/app/node_modules`를 익명 볼륨으로 처리하여 컨테이너 내의 패키지가 로컬 환경에 덮어씌워지지 않도록 설정.

### 결과

모든 UI 컴포넌트(`Alert`, `Tabs`, `Switch` 등)가 Turbopack 환경에서 정상적으로 컴파일되고 렌더링됨을 확인했습니다.

---

## 2. API 응답 필드 불일치 (Course/Subscription)

### 문제 상황

백엔드에서 내려오는 데이터 필드명(예: `professor`)과 프론트엔트 인터페이스(`professorName`)가 일치하지 않아 데이터가 `NaN`으로 표시되거나 목록이 비어 보이는 현상이 발생했습니다.

### 해결책

- **규격 통일**: 자체 정의한 **OpenAPI v0** 사양을 기준으로 백엔드 DTO와 프론트엔드 `types/api.ts`를 전면 재매핑했습니다.
- **주요 수정**: `totalSeats`, `currentSeats`, `availableSeats`, `professorName` 등으로 모든 필드명을 표준화했습니다.

### 결과

검색 결과 및 구독 목록에서 강의명, 교수명, 수강 인원 현황이 정확하게 노출됩니다.

---

## 3. 초기 검색 상태 오인 및 로딩 피드백 부재

### 문제 상황

1. **초기 상태 오인**: 검색 페이지 진입 시 검색 조건이 비어있어(`{}`) 아무런 데이터가 뜨지 않았고, 사용자는 이를 "버튼이 동작하지 않음"으로 오인했습니다.
2. **피드백 부재**: 검색 버튼 클릭 시 로딩 표시가 없어 네트워크 지연 시 사용자가 중복 클릭하거나 멈춘 것으로 판단했습니다.

### 해결책

1. **기본값 설정**: `SearchPage`의 초기 상태를 현재 크롤링 타겟 학기로 설정하여, 강의 검색 페이지 진입 시 기본 정보 필터와 결과 목록이 같은 학기를 바라보도록 수정했습니다.
2. **스켈레톤 UI 도입**: 데이터 로딩 중 빈 화면 대신 테이블 형태의 **Skeleton Loader**를 보여주어 로딩 중임을 명확히 인지시켰습니다.
3. **버튼 상태 관리**: API 요청 중에는 검색 버튼 내부에 스피너를 표시하고 클릭을 방지(`disabled`)했습니다.

### 결과

사용자가 페이지 진입 시점부터 검색 완료 시점까지 끊김 없는(Seamless) 경험을 하게 되었으며, 기본값이 따로 노는 문제와 "고장"이라는 오인을 원천 차단했습니다.

### 4. 테이블 헤더 고정 (Sticky Header) 이슈

- **문제**: `shadcn/ui`의 `Table` 컴포넌트 사용 시, 내부적으로 감싸고 있는 `div`의 `overflow-x-auto` 속성 때문에 `sticky` 포지셔닝이 스크롤 컨테이너 기준이 아닌 내부 div 기준으로 잡혀 헤더가 고정되지 않는 현상 발생.
- **해결**: `CourseTable` 컴포넌트에서 `Table` 래퍼 컴포넌트 대신 표준 HTML `table` 태그를 직접 사용하고, 외부 스크롤 컨테이너(`max-h-[650px]`)에 맞춰 `sticky`가 동작하도록 구조를 변경함.

---

## 4. 검색 경험(UX) 최적화: 조건부 필터 및 자동 접힘

### 문제 상황

1. 상세 검색 필터의 항목이 많아져 실제 검색 결과 리스트가 화면 하단으로 밀려나 가독성이 저하되었습니다.
2. 교양 관련 검색 필터가 전공 검색 시에도 노출되어 혼란을 야기했습니다.

### 해결책

1. **조건부 렌더링**: 이수구분이 '교양'인 경우에만 교양 상세 카테고리 필터가 애니메이션과 함께 나타나도록 수정했습니다.
2. **자동 접힘(Auto-Collapse)**: 검색 버튼을 누르거나 엔터 키를 입력하여 검색이 시작되면 자동적으로 `Collapsible` 필터바가 닫히도록 로직을 추가하여 결과 화면이 즉시 상단에 노출되도록 했습니다.

### 결과

모바일 및 데스크톱 환경 모두에서 화면 공간을 효율적으로 사용하게 되었으며, 사용자의 검색 흐름(Flow)이 비약적으로 매끄러워졌습니다.

---

## 5. 서비스 워커 무한 로딩 및 타임아웃 처리

### 문제 상황

웹푸시 기기 등록 시, `navigator.serviceWorker.ready`가 promise 상태에서 영원히 대기하여 UI가 무한 로딩("등록 중...") 상태에 빠지는 현상이 간헐적으로 발생했습니다.

### 원인

서비스 워커 등록이 어떤 이유로 지연되거나 이미 등록된 워커가 비정상 상태일 때, ready 속성이 resolve 되지 않는 경우가 있었습니다.

### 해결책

- **Time-out 도입**: `Promise.race`를 사용하여 서비스 워커 준비에 5초의 타임아웃을 걸었습니다.
- **Fail-safe**: 5초 내에 응답이 없으면 명확한 에러를 발생시키고 UI 로딩을 해제하여 사용자가 재시도를 할 수 있도록 유도했습니다.

```typescript
// 웹푸시 유틸 코드 예시
const registration = await Promise.race([
  navigator.serviceWorker.ready,
  new Promise((_, reject) =>
    setTimeout(() => reject(new Error("Timeout")), 5000),
  ),
]);
```

### 결과

무한 대기 상황이 사라지고, 사용자에게 즉각적인 피드백(성공/실패)을 제공하게 되었습니다.

---

## 6. Web Push VAPID 키 환경 변수 연동

### 문제 상황

웹푸시 구독(`pushManager.subscribe`) 시점에 `applicationServerKey`가 유효하지 않다는 에러가 발생하거나 구독 객체가 `null`로 반환되었습니다.

### 원인

Next.js 환경에서 클라이언트 사이드 코드(브라우저)는 `NEXT_PUBLIC_` 접두사가 붙은 환경 변수만 접근할 수 있는데, `.env` 파일에 접두사 없이 정의하거나 `process.env` 접근이 누락되어 키 값이 `undefined`로 전달되었습니다.

### 해결책

- **변수명 변경**: `.env` 파일의 변수명을 `NEXT_PUBLIC_VAPID_KEY`로 수정했습니다.
- **유효성 검사**: `webpush.ts` 유틸리티에서 키가 존재하는지 검사하는 방어 코드를 추가하여, 키 누락 시 명확한 에러 메시지를 던지도록 수정했습니다.

### 결과

서버와 동일한 공개 키를 사용하여 VAPID 핸드셰이크가 정상적으로 이루어지게 되었습니다.

---

## 7. 포그라운드 알림 미표시 (이중 알림 구조)

### 문제 상황

브라우저 탭을 보고 있는 상태(Foreground)에서는 OS 정책이나 브라우저 설정에 따라 시스템 푸시 알림이 뜨지 않아, 사용자가 중요 알림을 놓칠 가능성이 있었습니다.

### 해결책

서비스 워커와 클라이언트 간의 **양방향 통신(Message Passing)**을 활용한 이중 알림 구조를 구축했습니다.

1. **Service Worker**: 푸시 수신 시 `showNotification`을 호출함과 동시에, 열려 있는 모든 Window Client에게 `postMessage`로 푸시 내용을 전송합니다.
2. **Client**: `navigator.serviceWorker`의 메시지 이벤트를 리스닝하고 있다가, 푸시 메시지가 오면 **`sonner` Toast**를 화면 상단에 띄웁니다.
3. **UX 강화**: `requireInteraction: true` 옵션을 추가하여 사용자가 닫기 전까지 시스템 알림이 유지되도록 설정했습니다.

### 결과

### 사용자가 다른 작업을 하거나 탭을 보고 있을 때 등 모든 시나리오에서 알림을 확실히 인지할 수 있게 되었습니다.

## 8. API 요청 중복 제거 (Request Deduplication) 및 프로미스 캐싱

### 문제 상황

로그인 후 또는 페이지 진입 시 `/api/v1/users/me` API가 짧은 간격으로 2번 이상 호출되는 현상이 발생했습니다. 이는 React Strict Mode의 영향과 더불어, Zustand의 세션 체크와 React Query의 데이터 페칭이 각각 독립적으로 발생하며 네트워크 자원을 낭비하는 결과를 초래했습니다.

### 해결책

API 레이어(`lib/api/user.ts`)에서 **Promise Caching** 패턴을 도입하여 짧은 시간 내 발생하는 동일 요청을 하나로 묶었습니다.

```typescript
let profilePromise: Promise<CommonResponse<User>> | null = null;

export const getMyProfile = async () => {
  if (profilePromise) return profilePromise; // 가고 있는 기차가 있으면 그 티켓을 같이 씀

  profilePromise = (async () => {
    try {
      const { data } = await api.get("/api/v1/users/me");
      return data;
    } finally {
      setTimeout(() => {
        profilePromise = null;
      }, 100); // 처리 완료 후 캐시 해제
    }
  })();
  return profilePromise;
};
```

### 결과

서로 다른 상태 관리 도구(Zustand, React Query)가 동일한 API를 호출하더라도 실제 네트워크 요청은 단 1회만 발생하게 되어 초기 로딩 성능이 개선되고 서버 부하를 줄였습니다.

---

---

## 9. BFF 환경에서의 자동 토큰 리프레시 인터셉터 도입

### 문제 상황

BFF 아키텍처 전환 이후 세션 쿠키를 사용함에도 불구하고, 내부적으로 관리되는 JWT 액세스 토큰이 만료되면 401 Unauthorized 에러가 발생하여 사용자가 로그아웃되는 현상이 남아있었습니다. 기존에는 단순히 로그인 페이지로 리다이렉트했으나, 이는 사용자의 작업 흐름을 끊는 요인이 되었습니다.

### 해결책

Axios 인터셉터를 고도화하여 **투명한 토큰 리프레시(Transparent Token Refresh)** 메커니즘을 구현했습니다.

1. **401 감지**: API 호출 시 401 에러가 발생하면 인터셉터가 이를 가로챕니다.
2. **자동 리프레시**: 백그라운드에서 `/api/auth/refresh`를 호출하여 서버 사이드 세션과 토큰을 갱신합니다.
3. **요청 재시도**: 리프레시 성공 시 기존의 `failedQueue`에 담겨있던 요청들을 새로운 세션 상태로 자동 재시도합니다.
4. **무한 루프 방지**: `_retry` 플래그와 `isRefreshing` 상태를 활용하여 리프레시 요청 자체가 무한 반복되는 것을 원천 차단했습니다.

### 결과

사용자는 서비스 이용 중 별도의 재로그인 과정 없이도 장시간 안정적인 인증 상태를 유지할 수 있게 되었습니다.

---

## 10. 프리미엄 대시보드 Bento Grid 도입 및 레이아웃 최적화

### 문제 상황

기존 대시보드는 여러 독립된 섹션(시간표, 관심 강의, 알림)이 단순히 나열되어 있어 시각적 집중도가 떨어지고, 특히 모바일/작은 해상도에서 정보 과다로 인한 피로도가 높았습니다.

### 해결책

1. **Bento Grid 시스템**: 현대적이고 세련된 그리드 레이아웃을 적용하여 정보의 우선순위에 따라 구획을 나눴습니다.
2. **레이아웃 간소화 (Layout Simplification)**: 사용자가 가장 핵심적인 정보인 '시간표'에 집중할 수 있도록, 기존 우측 컬럼의 '관심 강의'와 '실시간 알림' 목록을 상세 목록 형태에서 제외하고 `DashboardStats` 위젯으로 요약 정보만 상단에 노출했습니다.
3. **영역 확장**: 목록 섹션이 제거된 공간에 '대표 시간표' 섹션을 전면(Full-width)으로 확장하여 시각적 몰입감을 극대화했습니다.

### 결과

불필요한 데이터 로딩과 UI 요소를 줄임으로써 대시보드 진입 속도가 향상되었으며, 사용자는 가장 중요한 주간 일정을 더 크고 명확하게 확인할 수 있게 되었습니다.

---

## 11. 401 인터셉터 정교화 (Promise Queueing & Retry Logic)

### 문제 상황

BFF 환경에서 여러 API가 동시에 호출되는 중 액세스 토큰이 만료될 경우, 각 요청마다 독립적으로 401 에러를 받고 여러 번의 리프레시 시도가 발생하거나, 리프레시 도중 발생한 요청들이 유실되는 현상이 발견되었습니다.

### 해결책

Axios 인터셉터에 **요청 큐(Request Queue)** 메커니즘을 도입하여 리프레시 과정을 동기화했습니다.

1. **리프레시 잠금 (`isRefreshing`)**: 이미 리프레시 요청이 진행 중이면 추가적인 리프레시 요청을 막습니다.
2. **요청 대기열 (`failedQueue`)**: 리프레시가 완료될 때까지 발생하는 모든 401 에러 요청들을 큐에 저장합니다.
3. **일괄 재시도**: 리프레시 성공 시 큐에 쌓인 모든 요청을 새로운 세션 상태로 일괄 재실행하며, 실패 시 일괄 로그아웃 처리합니다.

```typescript
// 인증 인터셉터 코드 예시
if (isRefreshing) {
  return new Promise((resolve, reject) => {
    failedQueue.push({ resolve, reject });
  })
    .then(() => api(originalRequest))
    .catch((err) => Promise.reject(err));
}
```

### 결과

인증 만료 시점에도 여러 요청이 충돌 없이 안전하게 처리되며, 사용자는 인증 갱신 과정을 인지하지 못한 채 중단 없는 서비스를 이용할 수 있게 되었습니다.

---

## 12. 비로그인 무한 로딩 및 불필요한 API 호출 최적화

### 문제 상황

1. **무한 로딩 (Infinite Loading)**: 비로그인 상태로 홈페이지 접속 시, `useUser` 훅이 401(Unauthorized) 응답을 받았음에도 불구하고 API 인터셉터가 이를 '토큰 만료'로 오해하여 `/api/auth/refresh`를 호출하고, 리프레시마저 실패하면 다시 `useUser`를 재시도하는 무한 루프에 빠져 화면이 로딩 상태에서 멈춤.
2. **불필요한 API 요청**: 검색 페이지(`SearchPage`) 진입 시, 비로그인 상태임에도 `useWishlist`, `useTimetable`, `useSubscriptions` 훅이 실행되어 401 에러 로그가 콘솔에 다수 찍히고 불필요한 네트워크 트래픽 발생.

### 해결책

1. **재시도 방지 및 에러 핸들링 간소화**:
   - `useUser` 훅의 `retry` 옵션을 `false`로 설정하여 첫 실패 시 즉시 종료되도록 수정.
   - `queryFn` 내부에서 401 에러를 catch하여 `null`을 반환하도록 로직을 변경, 에러 상태가 아닌 '비로그인(Guest)' 상태로 정상 처리되도록 함.

   ```typescript
   // 사용자 조회 훅 코드 예시
   queryFn: async () => {
     try {
       const response = await userApi.getMyProfile();
       return response.data;
     } catch (error) {
       // 401은 에러가 아닌 게스트 상태로 간주
       if (isAxiosError(error) && error.response?.status === 401) {
         return null;
       }
       throw error;
     }
   };
   ```

2. **인터셉터 재귀 호출 차단**:
   - API 응답 인터셉터에서 401 에러 발생 시, 만약 요청 URL이 `/api/auth/refresh`라면 더 이상 재시도하지 않고 에러를 반환하도록 예외 처리 추가.

3. **조건부 훅 실행 (Enabled Option)**:
   - 개인화 데이터(찜, 구독, 시간표)를 가져오는 모든 Hook에 `enabled: !!user` 옵션을 추가하여, 유저 정보가 로드된 상태에서만 API를 호출하도록 변경.

### 결과

- 비로그인 사용자가 접속 시 즉각적으로 랜딩 페이지가 렌더링되며, 불필요한 에러 로그나 네트워크 요청 없이 쾌적한 탐색 경험을 제공하게 되었습니다.

---

## 13. 설정 페이지 이메일 변경 시 400 에러 (Unverified Email)

### 문제 상황

설정 페이지에서 '알림 이메일 주소'를 변경하고 저장 버튼을 누르면 `400 Bad Request` 에러(메시지: `UNVERIFIED_EMAIL`)가 발생하며 저장이 불가능한 현상이 발생했습니다. 이는 백엔드에서 보안을 위해 "변경된 이메일은 반드시 인증을 거쳐야 한다"는 제약 조건을 추가했으나, 프론트엔드 UI에는 인증 절차가 누락되어 있었기 때문입니다.

### 해결책

1. **인증 UI 구현**: 설정 페이지 내 이메일 입력 필드 옆에 '인증' 버튼을 추가하고, 인증 코드 발송 및 검증 로직(타이머, 코드 입력창)을 구현했습니다.
2. **상태 관리**: `verified`, `emailSent` 등의 상태를 추가하여, 사용자가 이메일을 변경하면 `verified`를 `false`로 초기화하고, 인증 완료 시에만 `true`로 변경하도록 했습니다.
3. **유효성 검사**: 저장(`onSubmit`) 시, 이메일이 변경되었는데 `verified`가 `false`라면 저장을 막고 "인증을 완료해주세요"라는 토스트 메시지를 띄우도록 방어 코드를 작성했습니다.

### 결과

사용자는 앱 내에서 원활하게 이메일 본인 인증을 수행할 수 있게 되었으며, 인증된 안전한 이메일로만 알림 설정을 저장할 수 있게 되었습니다.

---

## 14. 설정 페이지 네비게이션 바 누락 (Layout Mismatch)

### 문제 상황

메인 페이지(`HomePage`)에는 네비게이션 바(`Header`)가 포함되어 있었으나, 설정 페이지(`SettingsPage`)로 이동하면 헤더가 사라져 사용자가 메인으로 돌아갈 방법이 없는 갇힌(Trapped) UX 문제가 발생했습니다.

### 해결책

1. **전역 레이아웃 적용**: `Header` 컴포넌트를 특정 페이지(`Embed in Page`) 방식에서 전역 레이아웃(`layout.tsx`) 방식으로 변경하여, 설정 페이지를 포함한 모든 페이지에서 헤더가 유지되도록 수정했습니다.
2. **조건부 렌더링**: 온보딩 페이지(`/onboarding`) 등 헤더가 불필요한 경로에서는 `usePathname`을 사용하여 헤더를 숨기도록 처리했습니다.
3. **뒤로가기 버튼**: 설정 페이지 상단에 명시적인 '뒤로가기(←)' 버튼을 추가하여 모바일 환경 등에서의 네비게이션 편의성을 보강했습니다.

### 결과

어떤 페이지에 있더라도 일관된 네비게이션 경험을 제공하게 되었으며, 사용자는 자유롭게 메뉴 간 이동이 가능해졌습니다.

---

## 15. 네비게이션 바 중복 노출 (Double Header) 및 UI 간소화

### 문제 상황

1. **Double Header**: 전역 레이아웃(`layout.tsx`)에 `Header` 컴포넌트를 통합했음에도 불구하고, `SearchPage`, `NotificationPage`, `TimetablePage`, `CourseDetailPage` 등 각 페이지 내부에서도 `<Header />`를 호출하고 있어 네비게이션 바가 2개씩 표시되는 UI 버그가 발생했습니다.
2. **UI Clutter**: 모바일 환경에서 "총 N개의 강의 검색됨"이라는 텍스트가 화면 공간을 차지하여, 실제 수강신청에 필요한 버튼이나 정보가 가려지는 불편함이 있었습니다.

### 해결책

1. **중복 컴포넌트 제거**: 각 페이지(`app/*/page.tsx`) 파일에서 `<Header />` 컴포넌트 import 및 JSX 렌더링 코드를 모두 제거하여, `layout.tsx`에서 제공하는 단일 헤더만 보이도록 수정했습니다.
2. **UI 요소 제거**: 사용자 피드백을 수용하여, 검색 결과 상단의 "총 N개의 강의 검색됨" 카운터 표시 블록을 삭제하여 UI를 더 깔끔하게 만들었습니다.

### 결과

- 모든 페이지에서 일관된 단일 네비게이션 경험을 제공하게 되었습니다.

---

## 16. 시간표 렌더링 초기화 이슈 (Empty Timetable)

### 문제 상황

시간표 페이지(`/timetable`) 진입 시, `useQuery`로 데이터를 받아왔음에도 불구하고 화면에 시간표가 표시되지 않고 "아직 생성된 시간표가 없습니다" 메시지도 뜨지 않는 빈 상태가 지속되었습니다.

### 원인

React의 렌더링 사이클과 State 초기화 시점의 불일치가 원인이었습니다.
기존 코드에서는 `selectedTimetableId`를 설정하는 로직이 컴포넌트 본문(Render Body)에 조건문으로 작성되어 있었는데, 이는 React의 순수성(Purity) 원칙에 어긋나며, 데이터가 `useQuery`로부터 도착한 직후 리렌더링이 발생할 때 상태 업데이트가 누락되거나 무시되는 경우가 발생했습니다.

### 해결책

1. **Side Effect 분리**: 상태 초기화 로직을 `useEffect` 내부로 이동하여, `timetables` 데이터가 변경될 때만 명시적으로 실행되도록 수정했습니다.
2. **로딩 상태 시각화**: 상세 시간표 데이터를 가져오는 동안(`isDetailLoading`) 로딩 스피너를 표시하여 사용자에게 진행 상황을 명확히 알렸습니다.

```typescript
// 변경 전
if (timetables.length > 0 && selectedTimetableId === null) {
  setSelectedTimetableId(timetables[0].id);
}

// 변경 후
useEffect(() => {
  if (timetables.length > 0 && selectedTimetableId === null) {
    setSelectedTimetableId(timetables[0].id);
  }
}, [timetables, selectedTimetableId]);
```

### 결과

페이지 진입 시 첫 번째(또는 대표) 시간표가 즉시 선택되어 정상적으로 렌더링됩니다.

---

## 17. 모바일 환경에서의 시간표 그리드 시인성 저하

### 문제 상황

모바일 기기의 좁은 화면 폭으로 인해 7일치 시간표 열이 지나치게 좁게 렌더링되어, 수업명과 시간 등의 텍스트가 겹치거나 생략되어 읽을 수 없는 현상이 발생했습니다.

### 해결책

1. **최소 너비 강제**: 그리드 컨테이너에 `min-w-[600px]`를 부여하여 열 너비의 하한선을 확보했습니다.
2. **가로 스크롤 도입**: 고정된 너비로 인해 화면을 벗어나는 영역은 가로 스크롤(`overflow-x-auto`)을 통해 탐색할 수 있도록 UX를 개선했습니다.
3. **폰트 스케일업**: 모바일 환경을 고려하여 그리드 내부 텍스트(`text-[10px]` -> `text-xs`)의 크기를 키우고 가독성을 보완했습니다.

### 결과

사용자는 모바일에서도 텍스트 뭉개짐 없이 쾌적하게 시간표 정보를 인지할 수 있게 되었습니다.

---

## 18. 모바일 가로 스크롤 시 이미지 저장 짤림 현상

### 문제 상황

`html-to-image` 라이브러리를 사용하여 시간표를 캡처할 때, 브라우저의 가로 스크롤로 인해 화면에 보이지 않는 영역(오른쪽 요일 등)이 이미지에서 잘려서 저장되는 문제가 발생했습니다.

### 원인

라이브러리가 기본적으로 요소의 가시적인 `getBoundingClientRect` 크기를 기준으로 캡처를 시도하기 때문에, 스크롤 컨테이너 내부에 숨겨진 영역이 포함되지 않았습니다.

### 해결책

1. **실제 크기 계산**: 타겟 요소의 `scrollWidth`와 `scrollHeight`를 직접 계산하여 캡처 옵션의 `width`, `height`로 명시적으로 전달했습니다.
2. **패딩 보정**: 캡처 시 추가되는 여백(Padding) 값을 너비/높이 계산에 정확히 반영하여 패딩으로 인해 내용물이 밀려나는 현상을 방지했습니다.
3. **레이아웃 초기화**: 캡처 순간에만 `maxWidth: none`, `minWidth: none` 스타일을 강제 적용하여 브라우저의 레이아웃 제약을 완전히 해제했습니다.

### 결과

### 사용자가 현재 보고 있는 화면 상태와 상관없이, 항상 전체 요일이 포함된 깔끔하고 선명한(2x 해상도) 시간표 이미지를 저장할 수 있게 되었습니다.

## 19. 이벤트 핸들러 내 Hook 호출로 인한 Invalid Hook Call

### 문제 상황

`CourseSearchBar` 컴포넌트의 체크박스 `onClick` 이벤트 핸들러 내에서 유저 정보를 가져오기 위해 `useUser` 훅을 호출했을 때, "Invalid hook call. Hooks can only be called inside of the body of a function component" 에러가 발생하며 앱이 중단되었습니다.

### 원인

React의 **Hooks 규칙(Rules of Hooks)**에 따라 훅은 반드시 리액트 함수 컴포넌트의 최상위(Top-level)에서만 호출되어야 합니다. `onClick`과 같은 이벤트 핸들러나 일반 자바스크립트 함수 내부에서 훅을 호출하는 것은 금지되어 있습니다.

### 해결책

1. **호출 위치 변경**: 핸들러 내부에 있던 `useUser` 호출을 컴포넌트 본문 상단으로 이동시켰습니다.
2. **데이터 활용**: 상단에서 가져온 `user` 데이터를 핸들러 함수 내에서 참조하여 비로그인 여부를 판단하도록 로직을 수정했습니다.

```tsx
// 수정 후
export function CourseSearchBar(...) {
  const { data: user } = useUser(); // 최상위 호출

  const handleWishlistToggle = () => {
    if (!user) { // 데이터 참조
      toast.error("로그인이 필요합니다.");
      return;
    }
    // ... 나머지 처리 로직
  };
}
```

### 결과

### 리액트 렌더링 생명주기에 부합하는 올바른 훅 사용 방식을 통해 런타임 에러를 해결하고 안정적인 인증 체크 로직을 구현했습니다.

## 20. 인앱 브라우저 Google 로그인 차단 및 감지 이슈

### 문제 상황

에브리타임, 카카오톡, 페이스북 등의 앱 내부 브라우저에서 Google 로그인을 시도할 경우, Google의 보안 정책(`disallowed_useragent`)에 의해 로그인이 차단되는 현상이 발생했습니다.

### 원인

Google은 임베디드 웹뷰(WebView)에서의 OAuth 요청을 보안상의 이유로 허용하지 않습니다. 특히 User-Agent에 `Whale`이 포함되거나 특정 앱의 식별자가 포함된 경우 비정상적인 브라우저로 판단될 수 있습니다.

### 해결책

1. **정교한 감지 로직**: `lib/utils.ts`의 `isInAppBrowser` 함수를 강화하여 `Everytime`, `KAKAOTALK`, `FB_IAB`, `Instagram` 등을 감지하도록 개선했습니다.
2. **Whale 예외 처리**: 네이버 웨일 브라우저의 경우 정상적인 브라우저임에도 문자열 패턴에 의해 인앱으로 오판되는 경우가 있어, 명시적으로 제외(`!/Whale/i.test(ua)`) 처리를 추가했습니다.
3. **UX 개선**: 인앱 브라우저 감지 시 사용자에게 외부 브라우저(Chrome, Safari 등) 사용을 권장하는 안내 문구를 노출하고, 그럼에도 불구하고 진행을 원하는 사용자를 위해 **"그래도 계속하기"** 옵션을 제공하여 유연성을 확보했습니다.

### 결과

모바일 앱을 통해 유입되는 사용자들이 로그인 실패 원인을 명확히 인지하고, 성공적으로 로그인을 완료할 수 있는 경로를 제공하게 되었습니다.

---

## 21. 로컬 개발 환경에서의 API_URL Rewrite 설정 오류

### 문제 상황

`web/.env` 파일의 `NEXT_PUBLIC_API_URL` 설정을 잘못 구성하여 Next.js의 `rewrites` 기능이 동작하지 않거나 API 호출이 실패하는 문제가 발생했습니다.

### 원인

`next.config.ts`에서 `/api/:path*`를 백엔드로 프록시할 때, `.env`에 정의된 URL 형식이나 프로토콜이 실제 백엔드 주소와 일치하지 않아 발생한 연결 오류였습니다.

### 해결책

1. **환경 변수 가이드**: `.env` 설정 시 실제 백엔드 주소(예: `http://localhost:8080`)를 정확히 입력하도록 문서를 보강했습니다.
2. **Rewrite 규칙 명확화**: `next.config.ts`의 `destination` 설정을 환경 변수 기반으로 동적으로 구성하여 유연성을 높였습니다.

### 결과

### 로컬 및 배포 환경에서 API 요청이 안정적으로 백엔드 서버로 전달됩니다.

## 22. 시간표 선택(TimeTableSelector) 사각형 드래그 선택 기능 최적화

### 문제 상황

기존의 시간표 시간대 선택(TimeTableSelector) 방식은 단일 셀 클릭 혹은 단일 방향 드래그만 가능했습니다. 여러 요일과 교시를 한 번에 선택하거나, 기존 선택 영역의 일부만 해제하는 작업이 번거로워 사용자 경험(UX)이 저하되었습니다.

### 해결책

1. **상태 관리 고도화**: 드래그 시작 시점의 셀 좌표(`dragStart`)와 현재 마우스 위치(`dragCurrent`)를 추적하여 실시간으로 선택 영역(Bounding Box)을 계산하는 로직을 구현했습니다.
2. **드래그 모드(Drag Mode) 도입**:
   - **선택(Select)**: 비어있는 셀에서 드래그 시작 시, 영역 내 모든 셀을 선택 상태로 전환.
   - **해제(Deselect)**: 이미 선택된 셀에서 드래그 시작 시, 영역 내 셀들을 선택 취소 상태로 전환.
3. **포커스 유실 방지**: `mouseup` 이벤트가 시간표 영역 밖에서 발생하더라도 드래그가 정상적으로 종료되도록 `window.addEventListener`를 사용하여 전역 마우스 이벤트를 제어했습니다.
4. **시각적 프리뷰**: 드래그 중인 영역을 `ring` 테두리(선택 모드) 혹은 `destructive` 배경색(해제 모드)으로 실시간 렌더링하여 사용자에게 직관적인 피드백을 제공했습니다.

### 결과

### 여러 교시가 겹친 복잡한 시간대 설정이 단 한 번의 드래그로 가능해졌으며, 선택 취소 작업 역시 매우 직관적으로 개선되어 시간표 관리 효율성을 높였습니다.

## 23. 강의 상세 정보 UI 중복 및 관리 효율성 저하

### 문제 상황

강의 상세 정보를 보여주는 모달이 `CourseDetailDialog`(검색 페이지)와 `TimetableGrid`(시간표 페이지) 두 곳에서 각각 독립되게 구현되어 있었습니다. 이로 인해 디자인 수정이나 필드 추가 시 두 곳을 모두 수정해야 했으며, 데이터 매핑 로직도 파편화되어 유지보수 효율이 떨어졌습니다.

### 해결책

1.  **공통 컴포넌트 추출 (`CourseDetailContent`)**: 실제 데이터 렌더링을 담당하는 로직을 별도의 순수 UI 컴포넌트로 분리했습니다.
2.  **관심사 분리**: 다이얼로그 제어(Open/Close) 로직은 각 페이지에 남겨두고, 내용물은 `CourseDetailContent`를 재사용하도록 구조를 변경했습니다.
3.  **데이터 매핑 추상화**: `selectedCourse`(UI 전용)와 `detailedCourse`(API 응답) 데이터를 병합하여 공통 API 규격인 `Course` 타입으로 변환 후 전달하도록 개선했습니다.

### 결과

- 한 번의 수정으로 모든 화면의 강의 상세 UI를 동시에 업데이트할 수 있게 되었습니다.
- 코드 중복이 제거되어 `TimetableGrid` 파일 크기가 약 200라인 가량 감소했습니다.

---

## 24. 시간표 내 강의 블록 렌더링 로직의 비대화

### 문제 상황

`TimetableGrid` 컴포넌트가 시간표 그리드 레이아웃 생성뿐만 아니라 개별 강의 블록(`div`)의 스타일 계산(배경색 혼합, 투명도 등)까지 담당하면서 파일이 500라인 이상으로 비대해졌습니다. 이로 인해 코드 가독성이 떨어지고 특정 기능을 찾기 어려워졌습니다.

### 해결책

1.  **컴포넌트 분리 (`TimetableBlock`)**: 개별 강의 블록을 렌더링하는 전용 컴포넌트를 분리했습니다.
2.  **유틸 함수 이동**: 색상 계산(`mixWithWhite`, `hexToRgb` 등) 로직을 해당 컴포넌트로 이동시켜 관련성이 높은 코드를 하나로 묶었습니다.
3.  **컴포넌트 단위 관리**: 강의 클릭 핸들러와 데이터 전달 방식을 Props 인터페이스로 정의하여 코드를 정형화했습니다.

### 결과

- `TimetableGrid`는 전체적인 그리드 구조와 데이터 제어에만 집중하고, `TimetableBlock`은 개별 요소의 표현에만 집중하는 명확한 역할 분담(SOC)이 이루어졌습니다.
- 코드의 가독성이 크게 향상되었으며, 새로운 기능(예: 블록 내 특수 아이콘 추가) 도입 시 수정 범위가 명확해졌습니다.

---

## 25. 카카오맵 SDK 통합 및 동적 위치 렌더링

### 문제 상황

강의실 위치 정보를 시각적으로 보여주기 위해 카카오맵을 도입했으나 다음과 같은 기술적 과제가 발생했습니다:

1. **SDK 로딩 시점**: Next.js App Router 환경에서 외부 스크립트(`sdk.js`)의 로드 완료 시점과 지도 렌더링 시점을 동기화해야 함.
2. **도메인 보안**: 카카오 개발자 콘솔에 등록된 도메인이 일치하지 않으면 지도가 표시되지 않는 문제.
3. **키워드 검색 정규화**: "공과대학 4호관 401호"와 같은 텍스트를 "전북대학교 공과대학 4호관" 정도로 정규화하여 검색 정확도를 높여야 함.

### 해결책

1. **SDK 싱글톤 로더 (`kakao-map.ts`)**: `Promise`와 `script` 엘리먼트 동적 생성을 결합하여 SDK를 단 한 번만 로드하고, `window.kakao.maps.load` 콜백을 통해 초기화가 완료된 후 API 객체를 반환하는 구조를 구축했습니다.
2. **키워드 정규화 (`map-links.ts`)**: 정규표현식을 통해 "호수/실" 정보를 제거하고 "전북대학교" 접두사를 붙여 검색 엔진이 캠퍼스 내 건물을 정확히 찾을 수 있도록 가공했습니다.
3. **Embed 컴포넌트 (`KakaoMapEmbed.tsx`)**: `useEffect`를 사용하여 컴포넌트 마운트 시점에 지도를 렌더링하며, 검색 결과가 없을 경우 사용자에게 도메인 등록 확인 가이드를 UI로 노출하도록 설계했습니다.

### 결과

### 사용자는 강의 상세 페이지에서 텍스트로 보던 강의실 위치를 지도 마커로 즉시 확인할 수 있게 되어 캠퍼스 내 위치 파악 편의성이 비약적으로 향상되었습니다.

## 26. 강의 메타데이터(학과명) 중복 정보 제거 및 정규화

### 문제 상황

원천 데이터(OASIS)의 특정 학과명 뒤에 학년 숫자가 붙어 나오는 경우가 있어, UI상에서 "컴퓨터공학부 3 3학년"과 같이 정보가 중복 노출되는 현상이 발생했습니다. 이는 사용자에게 혼란을 주고 레이아웃이 지저분해 보이는 원인이 되었습니다.

### 해결책

1. **학과명 정규화 함수 (`formatDepartmentLabel`)**: 정규표현식을 사용하여 학과명 뒤의 불필요한 숫자 패턴(`3`, `3학년`, `3학년 등`)을 동적으로 감지하여 제거하는 로직을 구현했습니다.
2. **조건부 렌더링 최적화**: 대상 학년이 "전체"인 경우 학과명 옆의 학년 라벨을 숨겨 정보의 군더더기를 최소화했습니다.
3. **콤마 기반 다중 학과 처리**: 여러 학과가 섞여 있는 경우(컴마 구분) 각 항목에 대해 개별적으로 정규화를 적용한 후 결합하도록 설계했습니다.

### 결과

### 모든 강의의 메타데이터가 일정한 형식으로 표시되며, 중복된 학년 정보가 사라져 시인성이 대폭 향상되었습니다.

## 27. 강의 리뷰(Review) UX 고도화 및 용어 정립

### 문제 상황

1. **용어 혼선**: 기획 단계에서 "한줄평"이라는 용어를 사용했으나, 시스템이 별점 기능을 포함하고 있어 더 포괄적이고 전문적인 **"리뷰"**라는 명칭으로의 통일이 필요했습니다.
2. **비정상 요청 방지**: 이미 리뷰를 작성했음에도 불구하고 작성 폼이 계속 노출되어 409 Conflict 응답을 유발하거나, 사용자들이 불필요한 입력을 시도하는 불편함이 있었습니다.

### 해결책

1. **리소스 명칭 통일**: 컴포넌트(`CourseReviewSection`), API 타입, 그리고 UI상의 모든 텍스트(알럿, 메시지 등)를 "리뷰"로 전면 교체했습니다.
2. **조건부 UI 렌더링**:
   - `isReviewed` 플래그를 백엔드로부터 전달받아, 이미 작성한 사용자에게는 "이미 리뷰를 마친 강의입니다"와 같은 피드백을 즉각적으로 제공하고 작성 폼을 숨겼습니다.
   - 비인증 사용자에게는 단순 비활성화 대신 "로그인이 필요한 서비스입니다"와 함께 로그인 버튼을 유도하여 이탈률을 최소화했습니다.

### 결과

사용자는 자신의 리뷰 작성 상태를 명확히 인지할 수 있게 되었으며, 서비스 용어 통일로 인해 앱의 전체적인 완성도가 향상되었습니다.

---

## 28. 브라우저 기본 알렛 지양 및 프리미엄 토스트(Sonner) 도입

### 문제 상황

기존의 `window.alert()` 방식은 브라우저의 실행 흐름을 일시 정지(Blocking)시키고, 투박한 시스템 UI로 인해 서비스의 전체적인 프리미엄 톤앤매너를 저해했습니다. 특히 모바일 환경에서 알럿 창이 뜨면 상호작용이 단절되는 문제가 있었습니다.

### 해결책

현대적인 리액트 기반 알림 라이브러리인 **`sonner`**를 전역적으로 도입했습니다.

- **Non-blocking UX**: 알림이 뜨더라도 사용자가 다른 작업을 지속할 수 있도록 비동기 토스트 방식을 적용했습니다.
- **디자인 통합**: `sonner`의 커스텀 스타일 기능을 활용하여 Tailwind CSS 기반의 다크 모드 및 라이트 모드 디자인을 완벽히 매칭했습니다.
- **타입 안정성**: `toast.success`, `toast.error` 등 의미론적인 유틸리티를 사용하여 코드 가독성을 높였습니다.

### 결과

사용자는 서비스 이용 중 흐름을 끊지 않고도 세련된 피드백을 받을 수 있게 되었으며, 앱의 전반적인 완성도가 비약적으로 향상되었습니다.

---

## 29. 리뷰 작성 완료 인지성 개선 (Hiding Form vs Message)

### 문제 상황

이미 리뷰를 작성한 사용자에게 "이미 리뷰를 마친 강의입니다"라는 안내 메시지를 강조하거나 배너를 띄울 경우, 사용자는 이를 '경고'나 '오류'로 오해하는 경향이 있었습니다. 또한, 본인 리뷰가 성공적으로 등록되었음을 한눈에 파악하기 어려운 배치가 문제였습니다.

### 해결책

안내 메시지를 제거하고 **자연스러운 UI 전환(Natural UI Transition)** 방식을 선택했습니다.

1.  **폼 자동 은닉**: 리뷰가 이미 존재할 경우, '작성 폼'을 아예 렌더링하지 않음으로써 불필요한 입력을 유도하지 않도록 했습니다.
2.  **본인 리뷰 최상단 배치**: 백엔드에서 나의 리뷰를 항상 리스트 1번으로 내려줌으로써, 사용자가 작성 폼이 사라진 자리에 본인의 리뷰가 위치하게 되어 "아, 내가 작성한 리뷰가 여기 있구나"라고 자연스럽게 인지하게 유도했습니다.

### 결과

불필요한 텍스트 노이즈를 제거하여 화면을 더 깔끔하게 구성하고, 별도의 설명 없이도 사용자가 시스템의 상태를 정확히 파악할 수 있는 직관적인 UX를 완성했습니다.

---

## 30. 문의 및 건의사항 이미지 로딩 실패 (Spring Security & Rewrite)

### 문제 상황

문의 및 건의사항 게시판에서 이미지를 첨부하여 게시글을 생성할 경우, 본인 상세 내역이나 관리자 대시보드에서 이미지가 엑스박스로 표시되며 불러오기에 실패하는 현상이 발생했습니다. 브라우저 콘솔 확인 결과 `/uploads/xxx.webp` 경로에 대해 `401 Unauthorized` 또는 `404 Not Found` 에러가 기록되었습니다.

### 원인

1.  **Spring Security 인증 차단**: 백엔드 서버의 `/uploads/**` 경로가 보안 설정(`SecurityConfig`)의 `permitAll` 목록에 포함되어 있지 않아, 이미지 정적 리소스 접근 시 세션/토큰 인증을 요구하여 차단됨.
2.  **프론트엔드 리라이트(Rewrite) 누락**: Next.js 환경에서 `/uploads/**`로 들어오는 요청을 백엔드 서버(`http://localhost:8080`)로 전달해주는 프록시 설정이 `next.config.ts`에 존재하지 않아 브라우저가 Next.js 자체 경로에서 파일을 찾으려 함 (404 발생).

### 해결책

1.  **보안 정책 수정 (`SecurityConfig.java`)**:
    - `PERMIT_ALL_ENDPOINTS` 상수에 `/uploads/**` 경로를 추가하여 이미지 리소스에 대한 익명 접근을 허용했습니다.
2.  **Next.js 프록시 설정 추가 (`next.config.ts`)**:
    - `rewrites` 함수 내부에 `/uploads/:path*`를 백엔드 서버 주소로 연결하는 규칙을 추가하여 클라이언트가 `/uploads` 경로를 호출할 때 백엔드로 자동 위임되도록 설정했습니다.

### 결과

사용자와 관리자 모두 첨부된 이미지를 지연 없이 정상적으로 확인할 수 있게 되었으며, 보안 레이어와 프록시 레이어 간의 정적 리소스 서빙 구조가 명확해졌습니다.
---

## 31. 관리자 대시보드 강제 새로고침(데이터 갱신) 현상

### 문제 상황

관리자 대시보드 페이지에서 약 1분(실제로는 30초) 주기로 페이지가 강제로 새로고침되는 것 같은 현상이 발생하여 사용자 경험을 저해함.

### 원인

`useAdminOverview`와 `useHealth` 훅에 설정된 `refetchInterval: 30000` 옵션으로 인해 30초마다 자동으로 데이터를 다시 불러옴. 이때 데이터가 갱신되면서 컴포넌트가 리렌더링되고 Framer Motion 애니메이션이 다시 실행되어 사용자가 '새로고침'으로 오인함.

### 해결책

두 훅에서 `refetchInterval` 옵션을 제거하여 자동 갱신 기능을 끄고, 사용자가 필요할 때만 수동으로(새로고침 버튼 클릭 등) 데이터를 갱신하도록 수정함.

### 결과

의도치 않은 자동 갱신 현상이 사라져 대시보드 이용 안정성이 향상됨.
---

## 32. 강의실 위치에 불필요한 콜론(:) 노출 및 모호한 문구 개선

### 문제 상황

- 강의실 위치 정보가 배정되지 않은 경우 원본 데이터(Oasis)에서 `:` 문자가 포함되어 화면에 그대로 노출됨.
- 강의실이 없는 경우 "장소 미정"이라는 문구를 사용했으나, 온라인 강의 등 장소의 개념이 모호한 경우에 부적절하다는 피드백이 있음.

### 원인

- 백엔드 크롤러가 오아시스 시스템의 `VILROOMNOCTNT` 컬럼 값을 별도 가공 없이 저장함.
- 프론트엔드에서 데이터가 존재(`:`)할 경우 그대로 렌더링함.

### 해결책

- **백엔드**: `JbnuCourseParser`에서 강의실 값이 `:`인 경우 `null`로 변환하여 저장하도록 수정.
- **프론트엔드**:
  - `formatClassroom` 포맷터를 추가하여 문자열 내의 콜론 제거 및 공백 정규화.
  - 강의실 정보가 없을 때의 문구를 더욱 적절한 **"강의실 미배정"**으로 통일하여 수정.

### 결과

- 지저분하게 보이던 `:` 문자가 제거됨.
- 강의실 미배정 상태와 온라인 강의 상황을 포괄할 수 있는 명확한 문구로 개선됨.

---

## 33. Next.js 하이드레이션 불일치(Hydration Mismatch) 오류 해결

### 문제 상황

- 헤더 영역에서 `A tree hydrated but some attributes of the server rendered HTML didn't match the client properties` 오류 발생.
- 특히 Radix UI 기반의 `DropdownMenu` 등에서 ID(`aria-controls` 등)가 일치하지 않는 문제 발생.

### 원인

- 서버 사이드 렌더링(SSR) 시점의 사용자 상태(로그아웃)와 클라이언트 하이드레이션 시점의 사용자 상태(로그인 완료)가 불일치함.
- 이에 따라 렌더링되는 컴포넌트의 순서나 개수가 달라지며 Radix UI의 `useId` 생성 순서가 어긋남.

### 해결책

- `useHasMounted` 훅을 도입하여 컴포넌트 마운트 이후에만 클라이언트 전용 상태를 반영하도록 제어.
- 초기 렌더링(SSR 및 하이드레이션 단계) 시에는 서버와 동일한 상태를 유지하고, 마운트 직후 실제 사용자 정보를 리렌더링하여 안정성을 확보.

### 결과

- 하이드레이션 오류가 완벽히 해결됨.
- 레이아웃이 급격하게 변하지 않으면서도 안정적인 인증 UI 제공 가능.

---

## 34. 이수구분 '전공(대학원)' 프론트엔드 API 타입 추가

### 문제 상황

* JBNU 수강신청 API 응답 중 대학원 전공 강의의 이수구분 텍스트 규격(`"전공(대학원)"` 및 `"전공(대)"`)이 프론트엔드 타입 명세에서 빠져 있어 정적 분석 단계에서 타입 안정성이 저해됨.

### 해결책

* `web/src/shared/types/api.ts` 파일의 `CourseClassification` 유니온 타입에 `'전공(대학원)'` 및 `'전공(대)'`를 수동으로 매핑하여 추가함.

### 결과

* 타입 안전성을 확보하고, 추후 관련 도메인 필터링 필드 확장 시의 런타임 잠재적 경고 문제를 예방했습니다.

---

## 35. 프론트엔드 학기(Semester) 바인딩 고정 버그

### 문제 상황

* 사용자가 강의 검색 화면의 학기 필터를 '1학기'(`U211600010`) 등으로 명시 선택했음에도, 실제 API 요청 및 쿼리 파라미터에는 서버의 기본 학기(예: 하기 계절학기 `U211600015`)로 고정 바인딩되어 데이터가 잘못 조회되던 문제.

### 원인 분석

* `web/src/app/(main)/search/page.tsx` 내부에서 사용자가 클릭한 조건인 `userCondition`과 서버 기본값(`defaultSemester`)을 취합하여 호출하는 `searchCondition` 파생 `useMemo` 블록이 존재함.
* 해당 `useMemo` 내부에서 사용자가 수동 선택한 `userCondition.semester` 상태를 우선시하지 않고, 항상 `defaultSemester?.semester`를 최우선 순위로 고정 덮어쓰도록 작성되어 변경 사항이 소실됨.

### 해결책

* 사용자의 선택이 존재하면 그 값을 우선 채택하고, 누락된 경우에만 서버 기본값 및 FALLBACK 디폴트 값으로 순차 대입되도록 바인딩 규칙을 수정함.
  ```typescript
  // 수정 후
  semester: userCondition.semester ?? defaultSemester?.semester ?? FALLBACK_DEFAULT_CONDITION.semester,
  ```

### 결과

* 사용자가 드롭다운에서 선택한 학기 쿼리 파라미터가 유실 없이 백엔드로 올바르게 도달하며 데이터 조회가 정상 동기화됨을 확인했습니다.

---

## 36. 강력 새로고침 후 검색 페이지 인증 UI 누락

### 문제 상황

* 강력 새로고침 후 `'/search'` 페이지에서 로그인 버튼이 보이지 않고, 로그인 상태에서만 보여야 하는 헤더 내비게이션 버튼도 함께 사라지는 현상이 발생했습니다.

### 원인 분석

* `web/src/app/providers.tsx`의 `AuthProvider`가 `'/search'` 경로에서 `checkSession()`을 건너뛰고 있었습니다.
* `web/src/app/(main)/search/page.tsx`는 `useUser()`로 현재 사용자 데이터를 조회하지만, 그 결과를 `useAuthStore`에 다시 주입하지 않기 때문에 헤더의 인증 상태가 초기화되지 않았습니다.

### 해결책

* `AuthProvider`가 `'/search'`에서도 세션을 확인하도록 수정했습니다.
* 이로 인해 강력 새로고침 후에도 `useAuthStore`의 `isLoading`과 `user`가 정상적으로 초기화되고, 헤더가 로그인 CTA 또는 인증 상태에 맞는 메뉴를 다시 렌더링합니다.

### 결과

* 검색 페이지에서 강력 새로고침해도 로그인 버튼이 정상 표시됩니다.
* 로그인 상태에서는 인증 전용 네비게이션이 다시 노출됩니다.

---

## 37. 검색 페이지 진입 시 개인화 API 중복 호출

### 문제 상황

검색 페이지(`/search`)에 진입하면 로그인 여부와 관계없이 `useUser`, `useSubscriptions`, `useWishlist`, `useTimetables`가 각자 독립적으로 실행되어 `/api/v1/users/me`와 개인화 API가 중복 호출되었습니다. 비로그인 상태에서는 401 응답과 불필요한 refresh 시도까지 겹쳐 네트워크 요청이 과해졌습니다.

### 해결책

1. **검색 페이지 전용 사용자 조회 분리**: `/search` 진입 시에는 `skipAuthRefresh` 옵션으로 사용자 조회를 수행해 guest 상태를 빠르게 판별하고, 이후 개인화 훅들은 이 결과를 재사용하도록 했습니다.
2. **초기 사용자 주입**: `CourseSearchBar`, `CourseTable`, `CourseSmartFilters`에 `initialUser`와 `skipPersonalFetch`를 전달해, 이미 확인된 사용자 정보를 하위 훅에서 다시 조회하지 않도록 정리했습니다.
3. **개인화 훅 조건부 실행**: `useSubscriptions`, `useWishlist`, `useTimetables`는 초기 사용자 정보가 있으면 `useUser`를 다시 부르지 않고 그 값을 그대로 사용하도록 바꿨습니다.

### 결과

* 검색 페이지 진입 시 `/api/v1/users/me` 호출 수가 1회로 줄었습니다.
* 로그인 상태에서도 찜/구독/시간표 관련 API가 불필요하게 중복 호출되지 않게 되었습니다.
* 비로그인 상태에서는 개인화 API가 아예 실행되지 않아 콘솔 401 노이즈와 낭비 트래픽이 줄었습니다.

---

## 38. 모바일 Lighthouse에서 폰트 로딩이 렌더링을 지연

### 문제 상황

모바일 Lighthouse에서 `/search` 페이지의 초기 렌더링이 늦게 끝났고, 원인 중 하나로 Google Fonts 요청과 과도한 폰트 weight 로딩이 잡혔습니다.

### 해결책

1. **`next/font` 전환**: 외부 `fonts.googleapis.com` CSS import를 제거하고 `next/font/google`로 폰트를 로딩하도록 변경했습니다.
2. **폰트 weight 축소**: `Noto Sans KR`는 실제 사용에 필요한 `400`, `700`, `900`만 로드하도록 줄였습니다.
3. **불필요한 sans fallback 제거**: 전역 sans stack에서 추가 fallback을 제거해 폰트 경로를 단순화했습니다.

### 결과

* 외부 Google Fonts render-blocking 요청이 줄었습니다.
* 모바일 Lighthouse에서 폰트 요청이 LCP 경로를 붙잡는 시간이 감소했습니다.
* 실제 UI 스타일은 유지하면서 초기 로딩 비용만 낮췄습니다.

---

## 39. 비로그인 대시보드 D-day 위젯 위치 및 모바일 반응형 버그

### 문제 상황

1. **모바일 미노출**: 수강신청 D-day 잔여일을 나타내는 카운트다운 위젯이 모바일 화면(너비 768px 미만) 환경에서 보이지 않고 완전히 사라졌습니다.
2. **비로그인 레이아웃 어색함**: 비로그인 사용자가 메인 랜딩 페이지([home-landing.tsx](/Users/bhoon/Project/jbnu-sugang-helper/apps/web/src/widgets/home/home-landing.tsx))에 진입할 경우, 카운트다운 위젯이 화면 최하단 중앙에 덩그러니 놓여 있어 디자인상 통일성과 집중도가 떨어지는 형태였습니다.

### 원인 분석

1. **CSS 미디어 쿼리 제한**: [dashboard-countdown.tsx](/Users/bhoon/Project/jbnu-sugang-helper/apps/web/src/widgets/home/dashboard-countdown.tsx) 위젯의 기저 CSS 스타일 클래스에 `hidden md:flex`가 선언되어 모바일 해상도에서 컴포넌트가 숨김(`display: none`) 처리되었습니다.
2. **컴포넌트 배치 오설정**: [home-landing.tsx](/Users/bhoon/Project/jbnu-sugang-helper/apps/web/src/widgets/home/home-landing.tsx) 파일 내부 Hero 소개 텍스트 및 시작 버튼 그룹과 거리를 둔 하단 영역인 `<div className="mt-12 w-full flex justify-center">`에 마운트되도록 정적으로 하드코딩 되어 있었습니다.

### 해결책

1. **반응형 CSS 적용**:
   - [dashboard-countdown.tsx](/Users/bhoon/Project/jbnu-sugang-helper/apps/web/src/widgets/home/dashboard-countdown.tsx)의 `hidden md:flex`를 걷어내고 `flex`를 기본으로 부여하였습니다.
   - 모바일 해상도의 좁은 뷰포트에서도 영역이 깨지거나 가로 넘침이 발생하지 않도록, `text-[10px] sm:text-xs`, `px-2.5 sm:px-3`, `h-8 sm:h-9` 등의 최적화된 모바일 전용 폰트 및 패딩 스타일을 적용했습니다.
2. **렌더링 레이아웃 재배치**:
   - [home-landing.tsx](/Users/bhoon/Project/jbnu-sugang-helper/apps/web/src/widgets/home/home-landing.tsx)에서 기존 Hero 하단의 어색했던 카운트다운 마운트 위치를 삭제했습니다.
   - 랜딩 페이지의 메인 슬로건 설명글 바로 아래이자 액션 버튼 컨테이너("지금 시작하기") 바로 위의 중앙 상단 영역으로 D-day 카운트다운 노출 위치를 상향 재배치했습니다.

### 결과

- 모바일 디바이스로 메인 및 대시보드 접근 시에도 D-day 카운트다운 정보가 레이아웃 깨짐 없이 올바르게 노출됩니다.
- 로그인 전 첫 화면 진입 시 핵심 버튼과 D-day 일정이 유기적으로 조화를 이루며 사용자의 첫 방문 인지도가 향상되었습니다.


## Server Troubleshooting

이 문서는 `zup-zup` 개발 과정에서 직면한 주요 기술적 문제와 그 해결 과정을 기록합니다.

---

## 40. 알림 발송 시 N+1 쿼리 문제 해결

### 문제 상황

특정 과목에 여석이 발생했을 때, 해당 과목의 수천 명의 구독자에게 알림을 발송하는 과정에서 성능 저하가 발생했습니다. 로그 확인 결과, 각 구독자마다 `User` 정보와 `UserDevice` 정보를 개별적으로 조회하는 N+1 쿼리 문제가 발견되었습니다.

### 가설 및 실험

- **가설**: 구독자 ID 리스트를 미리 추출하여 `IN` 절을 사용하는 배치 조회를 수행하면 DB 왕복 횟수를 획기적으로 줄일 수 있을 것이다.
- **실험**: `userRepository.findById()`와 리스트 조회를 비교 테스트.

### 해결책

`subscriptionRepository.findByCourseKeyAndIsActiveTrue()`로 얻은 구독자 리스트에서 `userId`를 추출하여 다음과 같이 배치 처리로 변경했습니다.

```java
// 개선된 로직
List<Long> userIds = subscriptions.stream().map(Subscription::getUserId).distinct().toList();
Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
Map<Long, List<UserDevice>> deviceMap = userDeviceRepository.findByUserIdIn(userIds).stream()
        .collect(Collectors.groupingBy(UserDevice::getUserId));
```

### 결과

수천 명의 구독자가 있는 경우에도 DB 조회 횟수를 단 3회로 고정하여 알림 발송 지연을 약 80% 단축했습니다.

---

## 41. Redis를 이용한 중복 알림 방지 (Dedup)

### 문제 상황

크롤링 주기가 짧고, 학교 서버의 데이터 갱신 시점이 불분명하여 짧은 시간 내에 동일한 과목에 대한 알림이 여러 번 발송되는 UX 저성 문제가 발생했습니다.

### 해결책

**Redis**를 분산 락 및 캐시로 활용하여 동일 과목-동일 상태에 대한 알림 발송 이력을 관리합니다.

- **로직**: 알림 발송 전 `ALERT:{courseKey}` 키가 Redis에 존재하는지 확인.
- **만료 시간**: 10분(DEDUP_TTL)으로 설정하여 적절한 간격을 유지.

```java
if (redisService.hasKey(redisKey)) {
    log.info("[Dedup] Notification already sent. Skipping.");
    return;
}
// 알림 발송 후...
redisService.setValues(redisKey, "SENT", DEDUP_TTL);
```

---

## 42. 통합 테스트에서의 비동기(@Async) 처리 제어

### 문제 상황

`NotificationService`가 `@Async`로 동작하여, 통합 테스트 코드에서 알림 발송 메서드가 호출되었는지 검증할 때 `verify()`가 실행 완료 전 테스트가 종료되는 비결정성(Non-determinism) 문제가 발생했습니다.

### 가설 및 실험

- **가설 1**: `Thread.sleep()`을 사용한다. (대기 시간이 길어지고 테스트 속도가 저하됨)
- **가설 2**: 테스트 환경에서만 `TaskExecutor`를 동기식으로 교체한다.

### 해결책

`@TestConfiguration`을 사용하여 테스트 환경에서만 `SyncTaskExecutor`를 `@Primary` 빈으로 등록했습니다.

```java
@TestConfiguration
public class TestAsyncConfig {
    @Bean @Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
```

### 결과

`@Async` 로직이 테스트 코드와 동일한 스레드에서 실행되도록 강제하여 `Thread.sleep()` 없이도 100% 신뢰할 수 있는 통합 테스트를 구현했습니다.

---

## 43. 민감한 외부 API URL의 안전한 관리

### 문제 상황

학교(JBNU)의 실제 API URL이 소스 코드나 `application.properties`에 하드코딩되어 노출될 경우, 보안상 취약점이 발생할 수 있고 외부 공개 시 문제가 될 소지가 있었습니다.

### 해결책

**환경 변수(Environment Variables)**를 활용하여 민감한 정보를 소스 코드와 완전히 분리했습니다.

- **방법**: `application.properties`에는 `${JBNU_API_URL}`과 같은 플레이스홀더만 남기고, 실제 값은 `.env` 파일을 통해 주입받습니다.
- **구조 최적화**: 설정의 성격에 따라 `infra/.env`(인프라)와 `server/.env`(비즈니스 로직)로 분리하여 관리 효율성을 높였습니다. `docker-compose` 실행 시에는 두 파일을 병합하여 사용하고, 로컬 개발 시에는 `server/.env`만으로도 구동이 가능하도록 구성했습니다.
- **테스트 환경 대응**: 수동 테스트 진행 시에도 하드코딩된 URL 대신 시스템 프로퍼티(`-Djbnu.api.url`) 또는 환경 변수에서 값을 읽어오도록 리팩토링하여 보안성을 높였습니다.

---

## 44. 무거운 통합 테스트 분리 (Manual Tag 적용)

### 문제 상황

프로젝트 규모가 커짐에 따라 `@SpringBootTest`를 포함한 통합 테스트의 실행 시간이 길어져(약 18~20초), 전체 테스트 주기가 느려지는 현상이 발생했습니다.

### 해결책

JUnit5의 **`@Tag("manual")`** 기능을 사용하여 무거운 통합 테스트와 실제 네트워크 호출이 포함된 테스트를 분리했습니다.

- **기본 테스트 (`./gradlew test`)**: 컨테이너 구동 없이 빠르게 실행되는 단위 테스트 위주로 수행 (약 6초).
- **수동 테스트 (`./gradlew manualTest`)**: 명시적으로 필요한 경우에만 스프링 컨테이너를 구동하여 전체 연동 과정을 검증.

### 결과

개발 과정에서 빈번하게 실행하는 기본 테스트 속도를 3배 이상 개선하여 개발 생산성을 확보했습니다.

---

## 45. DB 초기화 시 세션 불일치 및 401(Unauthorized) 처리

### 문제 상황

Docker 환경에서 DB만 초기화된 경우, 브라우저에는 유효한 JWT(세션)가 남아있으나 서버 DB에는 해당 유저가 존재하지 않는 상태가 발생했습니다. 이 경우 기존 로직은 `USER_NOT_FOUND` (404)를 반환했으나, 프론트엔드에서는 이를 단순 데이터 부재로 판단하여 세션을 유지하려 시도하고, 이후 모든 요청이 실패하는 현상이 발생했습니다.

### 해결책

세션 정보(Email/JWT)는 존재하지만 DB에 유저가 없는 경우를 명확한 **인증 실패(401 Unauthorized)** 상태로 정의하고, 에러 코드를 통일했습니다.

- **변경 내역**: `AuthService.reissue`, `SubscriptionService.getCurrentUser` 등에서 유저 미발견 시 `ErrorCode.USER_UNAUTHORIZED` (401)를 던지도록 전면 수정.
- **프론트엔드 연동**: Axios 인터셉터가 401 에러를 감지하면 즉시 로컬 세션을 정리(`logout()`)하고 로그인 페이지로 리다이렉트 시키도록 연동.

### 결과

DB 초기화나 세션 만료 시 사용자가 의도치 않게 깨진 상태(Broken State)에 머무는 것을 방지하고, 자연스러운 재로그인을 유도하여 UX 안정성을 확보했습니다.

---

## 46. 구독 토글 시 목록에서 사라지는 문제 해결

### 문제 상황

대시보드에서 알림 스위치(토글)를 꺼서 비활성화하면, 구독 내역이 삭제된 것처럼 목록에서 사라지는 현상이 발생했습니다. 실제 데이터는 남아있으나 사용자 인터페이스에서 조회되지 않아 혼란을 초래했습니다.

### 원인 분석

`SubscriptionService.getMySubscriptions()` 메서드가 내부적으로 `findByUserIdAndIsActiveTrue(userId)`를 호출하고 있었습니다. 즉, **활성화된(`isActive=true`) 구독만 조회**하도록 구현되어 있어, 사용자가 알림을 끄는 순간 조회 대상에서 제외되었습니다.

### 해결책

Repository에 `findByUserId(userId)` 메서드를 추가하고, 서비스 로직이 이를 사용하여 **활성 상태와 관계없이 모든 구독**을 반환하도록 수정했습니다.

```java
// 변경 전: 활성 구독만 조회
return subscriptionRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()...

// 변경 후: 모든 구독 조회
return subscriptionRepository.findByUserId(user.getId()).stream()...
```

---

## 47. Docker 빌드 속도 최적화 (Layer Caching)

### 문제 상황

소스 코드 변경 후 Docker 이미지를 빌드할 때마다 의존성(Dependencies)을 포함한 전체 빌드 과정이 반복되어, 단순 수정에도 2분 이상의 시간이 소요되었습니다.

### 원인 분석

`Dockerfile`에서 `COPY . .` 명령어가 의존성 설치(`gradle build`)보다 먼저 실행되게 작성되어 있었습니다. 이로 인해 소스 코드 변경 시 모든 레이어가 캐시 무효화(Cache Miss)되었습니다.

### 해결책

Docker의 **Layer Caching** 메커니즘을 활용하기 위해 `Dockerfile`의 순서를 재구성했습니다.

1. `build.gradle`, `settings.gradle`만 먼저 복사
2. `gradle dependencies` 실행 (이 레이어는 소스 코드 변경 시에도 캐싱 유지)
3. 소스 코드 복사 및 빌드 실행

```dockerfile
# 1. 의존성 정의 파일 복사
COPY build.gradle settings.gradle ./
# 2. 의존성 다운로드 (캐싱됨)
RUN ./gradlew dependencies --no-daemon
# 3. 소스 복사 및 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon
```

### 결과

빌드 시간이 **약 130초에서 60초대로 50% 이상 단축**되었습니다.

---

## 48. CourseKey 데이터 절삭(Truncation) 및 크롤러 초기화 이슈

### 문제 상황

기존 `과목코드-분반` 형식의 키를 `연도-학기-과목코드-분반`의 Composite Key로 변경하는 과정에서, DB 컬럼 길이가 `varchar(20)`으로 고정되어 있어 `Data truncation` 에러가 발생했습니다.

### 해결책

- 모든 관련 엔티티의 컬럼 길이를 `varchar(64)`로 확장했습니다.
- 스프링 부트 `ApplicationReadyEvent`를 통해 서버 시작 시 즉시 크롤러가 구동되도록 하여 초기 데이터 공백 현상을 제거했습니다.

---

## 49. JbnuCourseParser XML 구조 대응 및 교양 영역 추출 보완

### 문제 상황

학교 API 응답에서 교양 영역(`FLDFGNM`) 컬럼이 비어 있는 경우가 많아 교양 영역별 필터링이 불가능했습니다.

### 해결책

`FLDCONVINFO` 필드(`"영역,상세구분"`)를 파싱하여 값을 추출하는 로직을 추가함으로써 데이터 정합성을 확보했습니다.

---

## 50. Web Push 초기화 에러 (Property Key Mismatch)

### 문제 상황

서버 구동 시 `PushService is not initialized` 에러와 함께 알림 발송이 실패하는 현상이 발생했습니다.

### 원인

`WebPushNotificationSender.java` 코드에서는 `@Value("${WEBPUSH_PUBLIC_KEY}")`와 같이 환경변수 스타일로 키를 참조했으나, 실제 `application.yml` 설정은 `app.webpush.public-key`와 같은 계층형 구조로 되어 있어 값을 주입받지 못했습니다.

### 해결책

코드의 `@Value` 어노테이션 값을 YAML 설정 파일 구조에 맞게 수정했습니다.

```java
// 수정 전
@Value("${WEBPUSH_PUBLIC_KEY}")

// 수정 후
@Value("${app.webpush.public-key:}")
```

### 결과

설정 값을 정상적으로 로드하여 `PushService`가 성공적으로 초기화되었습니다.

---

## 51. 알림 테스트 예외 처리 (Device Not Found)

### 문제 상황

관리자 알림 테스트 시, 대상 유저의 기기 토큰이 DB에 없음에도 불구하고 요청이 성공(200 OK)으로 반환되어 실제 발송 실패 원인을 파악하기 어려웠습니다.

### 해결책

`NotificationService`의 `sendTestNotification` 메서드에서 기기 존재 여부를 먼저 조회하고, 기기가 없을 경우 `ErrorCode.DEVICE_NOT_FOUND` 예외를 명시적으로 발생시키도록 로직을 추가했습니다. 이로써 프론트엔드에서 "기기가 등록되지 않았습니다"라는 정확한 피드백을 줄 수 있게 되었습니다.

## 52. BFF(Backend For Frontend) 아키텍처 전환 및 토큰 격리

### 문제 상황

기존의 브라우저 기반 JWT 저장 방식(LocalStorage/Memory)은 다음과 같은 문제점이 있었습니다:

1. **보안 취약점**: XSS 공격 시 자바스크립트를 통해 토큰이 탈취될 위험이 있음.
2. **401 Race Condition**: Access Token 만료 시 여러 API가 동시에 Refresh를 시도하면서 서버 부하 및 인증 로직 충돌 발생.
3. **URL 노출**: OAuth2 로그인 후 토큰 전달 과정에서 URL 파라미터로 토큰이 노출되는 보안 리스크.

### 해결책

브라우저가 토큰을 직접 다루지 않는 **BFF + 세션 쿠키** 패턴으로 전면 개편했습니다.

- **토큰 저장소 이전**: 생성된 JWT(Access/Refresh)를 브라우저로 보내지 않고, **Spring Session Redis**를 도입하여 서버 측 세션에 저장합니다.
- **인증 매커니즘**: 브라우저는 오직 `HttpOnly`, `SameSite=Lax` 설정이 된 `SESSION` 쿠키만 보유하며, 모든 인증은 서버 세션을 통해 수행됩니다.
- **필터 확장**: `JwtAuthenticationFilter`가 HTTP 헤더뿐만 아니라 서버 세션의 속성을 확인하여 인증을 수행하도록 로직을 확장했습니다.

### 결과

- **보안 극대화**: 브라우저 자바스크립트 엔진에서 토큰 접근이 원천 차단되어 XSS로부터 안전해졌습니다.
- **안정성 향상**: 클라이언트 측의 복잡한 리프레시 동기화 로직이 제거되고, 서버 세션 생명주기에 따른 일관된 인증 관리가 가능해졌습니다.

---

## 53. JWT 검증 실패 로그 상세화 (디버깅 능력 향상)

### 문제 상황

기존에는 JWT 검증 실패 시 단순히 `log.error("Invalid JWT token")`과 같이 단일 메시지만 출력되어, 토큰이 만료된 것인지, 서명이 잘못된 것인지, 혹은 형식이 틀린 것인지 구분하기 어려웠습니다.

### 해결책

`JwtProvider`의 `validateToken` 메서드 내부에 예외 타입별 상세 로깅을 추가했습니다.

```java
try {
    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    return true;
} catch (SecurityException | MalformedJwtException e) {
    log.info("잘못된 JWT 서명입니다.");
} catch (ExpiredJwtException e) {
    log.info("만료된 JWT 토큰입니다.");
} // ...기타 예외 처리
```

### 결과

### 서버 로그만으로도 사용자의 접속 이슈 원인을 즉시 파악할 수 있게 되어 트러블슈팅 속도가 비약적으로 향상되었습니다.

## 54. 401 Unauthorized 세션 안정화 (Timeout Align & Session Sync)

### 문제 상황

BFF 아키텍처 도입 이후에도 사용자가 30분 정도 서비스를 이용하지 않으면 401 Unauthorized 에러가 발생하며 로그아웃되는 현상이 빈번했습니다. 이는 JWT 액세스 토큰의 짧은 만료 시간(30분)과 서버 사이드 세션 타임아웃 간의 불일치, 그리고 토큰 재발급 시 세션 정보가 갱신되지 않는 문제로 파악되었습니다.

### 해결책

1. **만료 시간 연장 및 일치**: `application.yml` 설정을 통해 JWT 액세스 토큰 만료 시간과 Spring Session(Redis) 타임아웃을 모두 **2시간**으로 연장하여 생명주기를 일치시켰습니다.
2. **세션 속성 즉시 갱신**: `AuthService.reissue` 메서드에서 새로운 액세스 토큰이 발급될 때, 이를 서버 세션의 `ACCESS_TOKEN` 속성에도 즉시 반영하도록 로직을 수정했습니다.

```java
// AuthService.java
session.setAttribute("ACCESS_TOKEN", newAccessToken);
log.info("[Auth] Session tokens updated for email: {}", email);
```

### 결과

세션 유지 능력이 비약적으로 향상되었으며, 토큰 재발급 후에도 서버 세션과 인증 정보가 항상 일치하게 되어 끊김 없는 서비스 이용이 가능해졌습니다.

---

## 55. 대표 시간표 삭제 시 자동 승계 로직 구현 (SUX 개선)

### 문제 상황

사용자가 '대표 시간표'를 삭제할 경우, 홈 화면(Dashboard)의 핵심 위젯인 시간표 영역이 비어 보이게 되어 사용자 경험(UX)이 저하되는 문제가 있었습니다. 사용자가 수동으로 다른 시간표를 대표로 지정하기 전까지는 대시보드에서 시간표를 확인할 수 없었습니다.

### 해결책

`TimetableService.deleteTimetable` 로직을 개선하여 **자동 승계(Automatic Succession)** 메커니즘을 도입했습니다.

1. **상태 확인**: 삭제하려는 시간표가 현재 '대표(`isPrimary=true`)' 상태인지 확인합니다.
2. **조건부 승계**: 대표 시간표를 삭제하는 경우, 해당 유저가 보유한 남은 시간표 목록을 조회합니다.
3. **자동 지정**: 남은 시간표 중 하나라도 존재하면, 즉시 해당 시간표를 새로운 대표로 지정(`setPrimary(true)`)합니다.

### 결과

사용자가 여러 개의 시간표를 관리할 때 대표 시간표를 삭제하더라도 시각적 공백 없이 자연스럽게 다른 시간표가 대시보드에 노출되어 끊김 없는 사용자 경험을 제공하게 되었습니다.

---

## 56. 토큰 재발급 시 세션 유실 방지 (Force Session Creation)

### 문제 상황

BFF 아키텍처에서 JWT 액세스 토큰은 만료되었으나 리프레시 토큰이 유효한 경우(`/api/auth/refresh`), 간헐적으로 서버 세션 자체가 유실되어 `ACCESS_TOKEN` 속성을 저장하지 못하고 인증이 풀리는 현상이 발생했습니다.

### 원인 분석

`AuthService.reissue` 시점에 `request.getSession(false)`를 사용하거나 세션이 이미 만료된 경우, 새로운 속성을 저장할 공간(세션 객체)이 존재하지 않아 발생하는 문제였습니다.

### 해결책

토큰 재발급 로직에서 세션 존재 여부와 관계없이 **강제로 세션을 생성**하도록 보장했습니다.

```java
// AuthService.java
HttpSession session = request.getSession(true); // 기존 세션이 없으면 새로 생성
session.setAttribute("ACCESS_TOKEN", newAccessToken);
```

### 결과

### 액세스 토큰 만료 후 재발급 시에도 서버 세션 컨텍스트가 안정적으로 유지되어, 프론트엔드에서 401 발생 후 재시도 시 인증 상태가 완벽하게 복구되도록 개선되었습니다.

## 57. Querydsl 서브쿼리를 이용한 고성능 찜 필터링 구현

### 문제 상황

사용자가 찜한 강의만 검색 결과에 노출해야 하는 요구사항이 있었습니다. 초기 계획은 프론트엔드에서 모든 찜 목록을 가져와 필터링하는 방식이었으나, 검색 엔진(Pagination/Sorting)과의 연동 및 데이터 정합성 유지를 위해 백엔드 통합 필터링이 필요했습니다.

### 해결책

`CourseRepositoryImpl`의 `searchCourses` 로직에 Querydsl의 **`JPAExpressions`**를 활용한 `exists` 서브쿼리 조건을 통합했습니다.

- **로직**: `Wishlist` 엔티티와 `Course` 엔티티를 `courseKey`로 조인하되, 실제 조인 대신 `exists()` 체크를 사용하여 성능을 최적화했습니다.
- **인증 연동**: 서비스 레이어에서 `SecurityUtil`을 통해 현재 로그인 유저의 ID를 주입받아 서브쿼리의 조건으로 사용합니다.

```java
private BooleanExpression inWishlist(Boolean isWishedOnly, Long userId) {
    if (isWishedOnly == null || !isWishedOnly || userId == null) {
        return null;
    }

    QWishlist wishlist = QWishlist.wishlist;
    return JPAExpressions.selectOne()
            .from(wishlist)
            .where(wishlist.courseKey.eq(course.courseKey)
                    .and(wishlist.userId.eq(userId)))
            .exists();
}
```

### 결과

### 수만 건의 강의 데이터와 복잡한 검색 조건(연도, 학기, 요일 등) 속에서도 사용자의 찜 상태를 단일 쿼리로 정확하고 빠르게 필터링할 수 있게 되었습니다.

## 58. k6 통합 테스트 시 한글 검색어 인코딩 문제

### 문제 상황

`integrated-user-flow.js` 시나리오에서 한글이 포함된 검색 쿼리(`keyword=컴퓨터`)를 서버로 전송할 때, k6에서 URL 인코딩 처리를 누락하여 서버로부터 `400 Bad Request` 응답을 받거나 검색 결과가 0건으로 나오는 현상이 발생했습니다.

### 원인 분석

k6의 `http.get()`은 URL에 특수문자나 한글이 포함된 경우 자동으로 인코딩하지 않습니다. 특히 쿼리 파라미터 조합을 문자열로 관리할 때 이 문제가 두드러졌습니다.

### 해결책

JavaScript의 `encodeURIComponent()`를 사용하여 쿼리 파라미터의 값 부분을 명시적으로 인코딩하도록 보정했습니다.

```javascript
// 보정 전
const url = `${BASE_URL}/api/v1/courses?${query}&page=0&size=20`;

// 보정 후 (안전한 인코딩 적용)
const encodedQuery = query
  .split("&")
  .map((pair) => {
    const [key, value] = pair.split("=");
    return `${key}=${encodeURIComponent(value)}`;
  })
  .join("&");
const url = `${BASE_URL}/api/v1/courses?${encodedQuery}&page=0&size=20`;
```

### 결과

한글 키워드를 포함한 모든 검색 시나리오에서 **성공률 100%**를 달성했으며, 실제 유저와 동일한 검색 부하를 생성할 수 있게 되었습니다.

---

## 59. 고부하 Admin API 성능 테스트 시 인증(Authorization) 장벽

### 문제 상황

크롤링 트리거(`POST /api/v1/admin/courses/crawl`)나 전역 통계 조회와 같은 고부하 어드민 API를 k6로 테스트할 때, 유효한 `ADMIN_TOKEN`을 지속적으로 공급하고 인증 필터 체인의 오버헤드를 측정하는 과정에서 어려움이 있었습니다.

### 해결책

1. **환경 변수 기반 인증**: k6 실행 시 `-e ADMIN_TOKEN=<TOKEN>` 옵션을 통해 보안 시크릿을 소스 코드와 분리하여 주입받도록 설계했습니다.
2. **Spike Test 전용 시나리오**: 인증 토큰 재발급(`refresh`) 과정 자체에 고부하를 가하는 `auth-spike-test.js`를 별도로 구성하여, Redis 세션 저장소의 동시성 처리 능력을 단독으로 검증했습니다.

### 결과

보안 가이드라인을 준수하면서도 시스템에서 가장 무거운 어드민 기능 및 인증 레이어에 대한 정밀 부하 측정이 가능해졌습니다.

---

## 60. 디스코드 봇 DM 발송 실패 (403 Forbidden - 50007)

### 문제 상황

디스코드 연동 후 테스트 알림 발송 시, `403 Forbidden` 에러와 함께 `Cannot send messages to this user (Code: 50007)` 응답이 반환되었습니다.

### 원인 분석

디스코드 봇이 사용자와 DM을 하려면 두 가지 조건 중 하나를 만족해야 합니다.

1.  사용자와 봇이 동일한 서버(Guild)에 있어야 함.
2.  봇이 **"App"**으로서 사용자 계정에 직접 설치되어야 함(User Install).

기존 OAuth2 설정에는 `scope=identify`만 포함되어 있어 단순 로그인만 수행되었고, 봇이 설치되지 않아 DM 발송 권한이 없었습니다.

### 해결책

OAuth2 인증 URL에 `integration_type=1`(User Install) 파라미터와 `applications.commands` 스코프를 추가했습니다.

```javascript
// 변경된 인증 URL 파라미터
const params = {
  client_id: DISCORD_CLIENT_ID,
  redirect_uri: DISCORD_REDIRECT_URI,
  response_type: "code",
  scope: "identify applications.commands", // 필수 스코프 추가
  integration_type: "1", // User Install 모드 활성화
};
```

### 결과

사용자가 재연동 시 "내 계정에 추가" 옵션을 통해 봇을 설치하게 되었고, 서버 가입 여부와 관계없이 DM 발송이 가능해졌습니다.

---

## 61. NotificationChannel DB 컬럼 데이터 잘림 (Data Truncation)

### 문제 상황

디스코드 알림 발송은 성공했으나, 알림 이력을 저장하는 과정에서 `500 Internal Server Error`가 발생했습니다. 로그 확인 결과 `Data truncated for column 'channel'` 에러였습니다.

### 원인 분석

기존 `NotificationChannel` 엔티티의 `channel` 컬럼이 `varchar(255)`가 아닌 기본값(혹은 짧은 길이)으로 설정되어 있었거나, 기존 데이터 길이에 맞춰져 있었습니다. `DISCORD`라는 값(7자)을 저장하려다 길이 제한에 걸린 것입니다.

### 해결책

1.  **엔티티 수정**: `@Column(length = 20)`으로 길이를 명시했습니다.
2.  **Flyway 도입**: 운영 중인 DB 스키마를 안전하게 변경하기 위해 Flyway를 적용했습니다.
3.  **마이그레이션 스크립트 작성**: `V2__expand_channel_column.sql`을 통해 기존 테이블의 컬럼 길이를 확장했습니다.

```sql
ALTER TABLE notification_histories MODIFY COLUMN channel VARCHAR(20) NOT NULL;
```

### 결과

### 데이터베이스 스키마가 안전하게 마이그레이션되었고, 모든 채널(FCM, WEB, EMAIL, DISCORD)의 알림 이력이 정상적으로 저장되었습니다.

## 62. 대용량 XML 응답 수신 중 데이터 누락 (수업명 '헌법 1' 등)

### 문제 상황

주기적인 크롤링 작업 중, 특정 과목(예: `헌법 1`)이 데이터베이스에서 검색되지 않거나 업데이트되지 않는 현상이 발생했습니다.

### 원인 분석

1. **Jsoup 응답 크기 제한**: 학교 서버로부터 전달되는 전체 강의 데이터 XML은 약 **7.2MB**에 달하지만, `Jsoup`의 기본 응답 수신 제한(maxBodySize)이 **2MB**로 설정되어 있어 데이터가 중간에 절단되었습니다.
2. **데이터 배치**: `헌법 1`과 같이 파일 하단(약 96% 지점)에 위치한 과목들이 이 제한에 걸려 파싱 대상에서 제외되었습니다.

### 해결책

- **수신 제한 해제**: `JbnuCourseApiClient` 클래스에서 `Jsoup.connect()` 호출 시 `.maxBodySize(0)` 옵션을 추가하여 크기 제한 없이 전체 응답을 수신하도록 수정했습니다.

### 결과

7MB가 넘는 대용량 XML 데이터를 온전히 처리할 수 있게 되어, "헌법 1"을 포함한 전체 5,500여 개의 강의 데이터가 누락 없이 정확하게 크롤링됩니다.

---

## 63. 불규칙한 강의 시간표(DAYTMCTNT) 파싱 실패

### 문제 상황

일부 과목의 강의 시간 데이터에 다중 공백이나 예기치 않은 엔터티(`&#32;`)가 포함되어 있어, 시간표 객체로 변환하는 과정에서 `Enum` 매핑 에러가 발생하거나 데이터가 누락되었습니다.

### 해결책

1. **정규식 기반 공백 분리**: 기존 `split(" ")` 대신 다중 공백을 감지하는 `split("\\s+")`를 사용하여 파싱 안정성을 높였습니다.
2. **데이터 전처리**: XML 태그 내의 텍스트를 추출할 때 즉시 `trim()`을 적용하여 불필요한 공백을 제거하고, Jsoup의 엔티티 디코딩 기능을 활용하여 `&#32;` 등을 정상적인 공백으로 변환했습니다.

### 결과

---

## 64. 대규모 데이터 업데이트 시 MySQL Lock Wait Timeout 문제 해결

### 문제 상황

대용량 XML 파싱 및 전체 과목 크롤링(`CourseCrawlerService`) 작업 중, `Lock wait timeout exceeded` 에러가 발생하며 트랜잭션이 롤백되는 현상이 발생했습니다. 특히 H2 데이터베이스 환경에서 `Concurrent update` 오류가 빈번하게 나타났습니다.

### 원인 분석

1. **지나치게 큰 트랜잭션**: 5,500건의 강의 데이터를 단일 트랜잭션(`@Transactional`)으로 처리하여 DB 커넥션과 테이블 락을 너무 오래 점유했습니다.
2. **네트워크 I/O 포함**: 트랜잭션 내부에서 외부 API 호출(XML 수신)이 수행되어 대기 시간이 길어졌습니다.
3. **중복 실행 충돌**: 스케줄러와 수동 실행이 겹칠 경우 동일 레코드에 대한 락 경합이 심화되었습니다.

### 해결책

1. **트랜잭션 세분화**: 메서드 레벨의 `@Transactional`을 제거하고, `TransactionTemplate`을 사용하여 **개별 강의 단위**로 트랜잭션을 분리했습니다.
2. **I/O 외부화**: 외부 API 호출 로직을 트랜잭션 밖으로 이동시켰습니다.
3. **중복 실행 방지**: `AtomicBoolean`을 이용해 현재 크롤링이 진행 중인 경우 추가 요청을 무시(Skip)하도록 구현했습니다.

### 결과

테이블 락 점유 시간이 밀리초(ms) 단위로 단축되었으며, 동시 실행 시에도 충돌 없이 안정적으로 데이터를 갱신할 수 있게 되었습니다.

---

## 65. 대상학년(TargetGrade) 문자열 기반 필터링의 가독성 및 안전성 개선

### 문제 상황

강의 검색 기능에서 `대상학년`은 "1학년", "전학년" 등 문자열로 처리되었습니다. 이로 인해 코드 전반에 매직 리터럴이 산재하게 되었고, 오타로 인한 검색 오류 및 학년별 논리적 순서 처리에 어려움이 있었습니다.

### 해결책

1. **Enum 도입**: `TargetGrade` 에넘(GRADE_1 ~ GRADE_6, ALL, NONE 등)을 정의하고, 파서에서 한글 명칭을 에넘으로 즉시 변환하도록 개선했습니다.
2. **JPA 컨버터 적용**: `@Converter`를 통해 DB에는 가독성 좋은 코드(예: `G1`)로 저장하고, 애플리케이션에서는 에넘 객체로 다루도록 격리했습니다.
3. **QueryDSL 연동**: 문자열 비교 대신 에넘 비교를 통해 타입 안정성을 확보했습니다.

### 결과

코드 가독성이 향상되었으며, 학년 조건이 포함된 검색 쿼리의 신뢰성이 높아졌습니다.

---

## 66. 시간표 선택(TimeTableSelector) 드래그 선택 로직 최적화

### 문제 상황

기존 시간표 시간대 선택 로직은 단순 클릭 혹은 단일 방향 드래그만 지원하여, 복잡한 시간대를 직관적으로 선택하기 불편했습니다.

### 해결책

1. **사각형 드래그(Rectangular Drag) 도입**: 시작점과 끝점을 기준으로 사각형 영역 내의 모든 셀을 일괄 선택/해제하는 기능을 구현했습니다.
2. **모드 자동 전환**: 선택되지 않은 셀에서 드래그 시작 시 '선택' 모드, 이미 선택된 셀에서 시작 시 '해제' 모드로 동작하도록 컨텍스트를 부여했습니다.
3. **실시간 프리뷰**: 드래그 중인 영역을 시각적으로 즉시 반영하여 사용자 피드백을 강화했습니다.

### 결과

### 사용자가 여러 교시를 한 번에 선택하거나 해제할 수 있어 시간표 설정 편의성이 대폭 개선되었습니다.

## 67. API 응답 데이터의 Enum 명칭 노출 (Localization Mismatch)

### 문제 상황

시간표 조회 API(`TimetableCourseResponse`)에서 이수구분 필드가 `MAJOR_REQUIRED`와 같은 Enum 명칭으로 반환되어, 프론트엔드에서 이를 매번 한글로 변환하는 오버헤드가 발생했습니다. 프론트엔드의 포맷터 가중치를 줄이고 데이터 일관성을 확보할 필요가 있었습니다.

### 해결책

응답 DTO(`TimetableCourseResponse`)의 팩토리 메서드(`of()`)에서 Enum의 `name()` 대신 사전에 정의된 `getDescription()` 메서드를 사용하도록 변경했습니다.

```java
// 변경 전
.classification(course.getClassification() != null ? course.getClassification().name() : null)

// 변경 후
.classification(course.getClassification() != null ? course.getClassification().getDescription() : null)
```

### 결과

서버로부터 "전공필수", "교양" 등 사용자 친화적인 명칭이 직접 전달되어 프론트엔드 로직이 간소화되었으며, 데이터의 시각적 정합성이 향상되었습니다.

---

## 68. 프로젝트 전반의 코드 스타일 불일치 및 유지보수성 저하

### 문제 상황

여러 차례의 기능 추가와 리팩토링 과정에서 파일별로 들여쓰기(Indentation), 사용하지 않는 Import, 일관성 없는 주석 등이 누적되었습니다. 특히 IDE 설정 차이로 인해 2칸과 4칸 들여쓰기가 혼재되어 코드 리뷰 시 불필요한 변경 사항(Noise)이 다수 발생했습니다.

### 해결책

1.  **스타일 표준화**: 모든 Java 소스 코드의 들여쓰기를 **4개 공백**으로 통일했습니다.
2.  **클린 코드 정례화**: `/clean` 워크플로우를 활용하여 사용하지 않는 Import를 제거하고, 메서드별 1~3줄의 핵심 주석을 추가하여 SOLID 원칙을 재점검했습니다.
3.  **검증**: 스타일 수정 후 전체 테스트(`./gradlew test`)를 수행하여 로직에 영향이 없음을 확인했습니다.

### 결과

코드 베이스의 시각적 일관성이 확보되어 가독성이 높아졌으며, 향후 git diff 확인 시 코드 본연의 변경 사항에만 집중할 수 있는 환경이 조성되었습니다.

---

## 69. 커스텀 마크다운 파서의 한계와 라이브러리 교체 (`react-markdown`)

### 문제 상황

공지사항 본문을 렌더링하기 위해 직접 구현한 정규식 기반 `MarkdownViewer`가 표(Table), 복잡한 중첩 목록, 링크 내 특수문자 등을 제대로 처리하지 못하거나 스타일이 깨지는 현상이 발생했습니다. 문법이 추가될 때마다 정규식이 복잡해져 유지보수가 불가능한 수준에 도달했습니다.

### 해결책

직접 구현 방식을 폐기하고 업계 표준인 **`react-markdown`**과 **`remark-gfm`** 라이브러리를 도입했습니다.

- **보안**: `dangerouslySetInnerHTML` 없이 가상 DOM 기반으로 안전하게 렌더링하도록 구성.
- **스타일링**: 라이브러리가 생성하는 HTML 태그들에 Tailwind CSS 클래스를 직접 매핑하여 기존의 프리미엄 디자인 톤을 완벽하게 유지.

### 결과

마크다운 표준(CommonMark + GFM)을 100% 지원하게 되어, 복잡한 공지사항도 깨짐 없이 예쁘게 표현할 수 있게 되었습니다.

---

## 70. 관리자 패널의 참조 오류 및 구문 오류 해결

### 문제 상황

리팩토링 과정에서 `AdminAnnouncementPanel` 컴포넌트 내 `cn` 유틸리티 함수 임포트가 누락되어 런타임 에러가 발생했으며, TypeScript 환경에서 지원하지 않는 `switch` 표현식(Expression)을 사용하여 빌드 오류가 발생했습니다.

### 해결책

1. **임포트 보완**: 누락된 `@/shared/lib/utils` 경로로부터 `cn` 함수를 명시적으로 임포트했습니다.
2. **구문 수정**: TypeScript/CommonJS 환경 호환성을 위해 `switch` 식(Expression)을 표준 `switch` 문(Statement)으로 변경하여 안전하게 로직을 분기했습니다.

### 결과

관리자 기능의 안정성이 확보되었으며, 코드 정적 분석(Lint) 가이드라인을 준수하게 되었습니다.

---

## 71. Oracle Cloud ARM 환경에서의 네트워크 MTU 불일치로 인한 크롤링 타임아웃

### 문제 상황

학교 서버(Oasis)에서 대용량 강의 데이터(XML, 약 7MB 이상)를 수집하는 과정에서 `java.net.SocketTimeoutException: Read timed out` 에러가 지속적으로 발생하며 크롤링이 실패했습니다.

### 원인 분석

1. **MTU(Maximum Transmission Unit) 불일치**: 호스트 서버(Oracle Cloud)의 네트워크 인터페이스는 9000(Jumbo Frames)으로 설정되어 있으나, Docker의 기본 브리지 네트워크는 1500으로 고정되어 있었습니다.
2. **패킷 유실**: 대용량 XML 데이터를 전송받을 때 패킷 조각화(Fragmentation)가 제대로 이루어지지 않거나 유실되어 세션이 끊기는 현상이 발생했습니다.

### 해결책

`infra/docker-compose.yml`의 네트워크 정의 섹션에 드라이버 옵션을 사용하여 MTU를 호스트와 동일하게 9000으로 설정했습니다.

```yaml
networks:
  sugang-helper-network-server:
    name: sugang-helper-network-server
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 9000
```

### 결과

네트워크 도로 폭을 호스트와 일치시킴으로써 7MB 이상의 대용량 응답도 끊김 없이 안정적으로 수신하게 되어 크롤링 타임아웃 문제를 완전히 해결했습니다.

---

## 72. 서버 환경(ARM64)과 이미지 플랫폼(AMD64) 불일치 문제

### 문제 상황

서버 실행 시 `The requested image's platform (linux/amd64) does not match the detected host platform (linux/arm64/v8)` 경고가 발생하며, 컨테이너 성능이 극도로 저하되거나 헬스체크 실패로 인해 무한 재시작되는 현상이 발생했습니다.

### 해결책

서버(ARM64) 환경에서 네이티브 이미지를 직접 생성할 수 있도록 빌드 구조를 개선했습니다.

1.  **Dockerfile 개선**: CI에서 빌드된 JAR에 의존하던 방식에서, Docker 내부에서 직접 Gradle 빌드를 수행하는 **Multi-stage Build** 방식으로 전환했습니다.
2.  **Compose 설정 변경**: 외부 이미지를 Pull 받는 대신 현재 환경에서 즉시 빌드하여 실행하도록 `build: context` 설정을 추가했습니다.

### 결과

ARM64 환경에 최적화된 네이티브 이미지가 생성되어 에뮬레이션 오버헤드가 사라졌으며, 서버 구동 속도와 안정성이 비약적으로 향상되었습니다.

---

## 73. Flyway 마이그레이션 유효성 검증 실패 (Missing Migration)

### 문제 상황

서버 구동 시 `FlywayValidateException: Validate failed: Detected applied migration not resolved locally: 2` 에러가 발생하며 애플리케이션 시작이 중단되었습니다.

### 원인 분석

과거 유저의 DB에는 `V2` 마이그레이션이 이미 적용되어 기록(`flyway_schema_history`)되어 있었으나, 현재 소스 코드에는 `V2`가 삭제되거나 `V3`로 통합되어 로컬 파일과 DB 기록 간의 불일치가 발생했습니다.

### 해결책

기존 마이그레이션 이력을 유지하면서도 유효성 검증을 통과할 수 있도록 설정을 조정했습니다.

- **설정 추가**: `SPRING_FLYWAY_VALIDATE_ON_MIGRATE: "false"`를 통해 시작 시 엄격한 검증을 끄고, `SPRING_FLYWAY_IGNORE_MIGRATION_PATTERNS: "*:missing" extinction`을 적용하여 코드에 없는 과거 이력을 무시하도록 했습니다.

### 결과

### DB의 기존 데이터를 유지하면서도 새로운 코드 배포 시 중단 없이 서버가 정상적으로 구동되도록 조치되었습니다.

## 74. Mockito Stubbing 에러 및 SecurityContext 모킹 최적화

### 문제 상황

`CourseReviewServiceTest` 등 보안 컨텍스트가 필요한 단위 테스트에서 `Strictness.STRICT_STUBS` 설정으로 인해 미사용 스텁(Unused stub) 에러가 빈번하게 발생하거나, `SecurityUtil` 클래스가 모킹되었음에도 실제 `SecurityContextHolder`와의 상호작용으로 인해 `NullPointerException`이 발생하는 현상이 나타났습니다.

### 해결책

1. **Lenient 모킹 적용**: 모든 테스트 케이스에서 매번 호출되지 않을 수 있는 인증 관련 스텁에 `lenient()` 설정을 적용하여 불필요한 테스트 실패를 방지했습니다.
2. **SecurityContext 직접 제어**: `SecurityUtil`만 모킹하는 대신, 테스트 셋업(`@BeforeEach`)에서 `SecurityContextHolder`에 가짜 `Authentication` 객체를 직접 주입하고 정리(`@AfterEach`)하는 방식을 병행하여 실제 서비스 코드와 동일한 인증 환경을 구축했습니다.

### 결과

보안 로직이 포함된 서비스 코드에 대해 100% 신뢰할 수 있는 단위 테스트 환경을 확보했으며, `Strictness` 설정과 충돌 없이 유연한 테스트 작성이 가능해졌습니다.

---

## 75. 중복 리뷰 작성 방지 및 409 Conflict 처리 정책

### 문제 상황

동일 사용자가 동일 강의에 대해 여러 번 리뷰를 작성할 수 있는 기술적 허점이 발견되었습니다. 이는 단순 데이터 오염을 넘어 특정 강의의 평점을 인위적으로 조작할 수 있는 리스크가 있었습니다.

### 해결책

1. **비즈니스 제약 추가**: `CourseReviewService`에서 리뷰 작성 전 `existsByCourseIdAndUserId` 체크를 수행하도록 로직을 강화했습니다.
2. **전용 에러 코드 정의**: 기존의 `INVALID_INPUT` 대신 상황을 더 명확히 설명하는 **`REVIEW_ALREADY_EXISTS (E003, 409 Conflict)`** 에러 코드를 도입했습니다.
3. **HTTP 상태 코드 최적화**: 리소스의 상태 충돌임을 나타내기 위해 `409 Conflict` 상태 코드를 반환하도록 설정하여 RESTful 원칙을 준수했습니다.

### 결과

데이터 정합성을 완벽히 보장하게 되었으며, 클라이언트(Web)는 409 응답을 통해 이미 작성된 상태임을 정확히 인지하고 그에 맞는 UI 대응이 가능해졌습니다.

---

## 76. H2 데이터베이스 영속성 상실 (Memory vs File Mode)

### 문제 상황

로컬 개발 환경에서 스프링 서버를 재시작할 때마다 H2 데이터베이스(In-Memory 모드)의 모든 데이터(크롤링된 강의, 테스트 유저 등)가 삭제되어 매번 초기화 작업을 반복해야 하는 번거로움이 발생했습니다.

### 해결책

H2 데이터베이스 설정을 **In-Memory**에서 **File Persistence** 모드로 전환했습니다.

- **설정 변경**: `jdbc:h2:mem:testdb` 대신 `jdbc:h2:file:./data/jbnu-sugang` 경로를 사용하도록 `application.yml`을 수정했습니다.
- **보안/관리**: 생성된 데이터 파일이 Git에 포함되지 않도록 `.gitignore`에 `server/data/` 경로를 추가했습니다.

### 결과

서버 재시작 후에도 이전 크롤링 데이터와 리뷰 내역이 그대로 유지되어 로컬 개발 및 테스트 효율이 비약적으로 향상되었습니다.

---

## 77. 사용자 중심의 리뷰 정렬 알고리즘 도입 (My Review First)

### 문제 상황

강의 상세 페이지에서 수백 개의 리뷰가 최신순으로만 나열될 경우, 사용자가 본인이 작성한 리뷰를 확인하거나 수정(향후 지원)하기 위해 스크롤을 한참 내려야 하는 불편함이 있었습니다. 또한, 내가 작성한 리뷰가 성공적으로 등록되었는지 즉각적으로 인지하기 어려웠습니다.

### 해결책

리뷰 조회 쿼리에 **조건부 정렬 로직(Conditional Sorting)**을 도입했습니다.

- **쿼리 구현**: `ORDER BY CASE WHEN r.userId = :userId THEN 0 ELSE 1 END ASC, r.createdAt DESC` 형식을 사용하여, 현재 로그인한 사용자의 ID와 일치하는 리뷰를 최상단(`0`)에 배치하고 나머지는 최신순으로 배치했습니다.
- **백엔드 처리**: `CourseReviewRepository`에 커스텀 `@Query`를 적용하여 페이징 처리 시에도 이 규칙이 일관되게 적용되도록 구현했습니다.

### 결과

사용자는 본인의 리뷰를 항상 리스트의 가장 처음에 보게 되어 작성 완료 상태를 즉시 확인할 수 있으며, 본인의 의견을 타인의 의견과 쉽게 대조할 수 있는 직관적인 UX를 확보했습니다.

---

## 78. 이수구분 '전공(대학원)' 필터링 누락 버그 해결

### 문제 상황

* 사용자가 강의 검색 페이지의 상세 필터에서 **'전공(대학원)'**을 선택하고 검색할 때 대학원 전공 과목이 조회되지 않고 결과가 비어 있음.
* 실제 데이터베이스에는 대학원 전공 강의의 `classification`이 `MAJOR`("전공")로 저장되어 있으나, 이를 파싱 및 매핑하는 단계에서 누락이 발생함.

### 원인 분석

* **백엔드 도메인 매핑 누락**:
  - JBNU 수강신청 원본 API 응답에서 대학원 과목의 이수구분 텍스트 값은 `"전공(대학원)"` 또는 `"전공(대)"` 등으로 전송됨.
  - 백엔드 내부의 `CourseClassification` Enum 클래스 및 `from()` 매핑 메서드에서 해당 텍스트를 `MAJOR` Enum 값으로 변환해주는 분기 처리가 누락되어 null 또는 기본값으로 매핑됨.

### 해결책

1. **백엔드 Enum 매핑 보완**:
   - `CourseClassification.java`의 `from()` 정적 팩토리 메서드에 `"전공(대학원)"` 및 `"전공(대)"` 수신 시 `MAJOR`로 정규화하는 분기 분별 규칙 추가.
2. **단위 테스트 구축**:
   - `CourseClassificationTest.java`를 신규 생성하여 `"전공(대학원)"` 및 `"전공(대)"` 파싱 시 `CourseClassification.MAJOR`로 올바르게 전환되는지 검증 팩터 구현.

### 결과

"전공(대학원)" 및 "전공(대)" 텍스트가 정상적으로 `MAJOR`로 분류되어 검색 필터링 시 대학원 과목이 누락 없이 반환됨을 확인했습니다.

### 학습 및 방어 지침

* **외부 API 변동성 대응**: 외부 수강 데이터의 이수구분명이나 학과명 포맷은 기습적으로 변할 수 있으므로, Enum 파싱 시 예외 처리와 정규화 처리를 항상 동반해야 합니다.

---

## 79. CourseSchedule 업데이트 시 발생하는 불필요한 DELETE 및 INSERT 쿼리 성능 Baseline 측정

### 문제 상황

크롤러가 외부 API로부터 수강신청 데이터를 받아와 `Course.updateMetadata()`를 통해 강의 정보를 갱신할 때, 기존 시간표 목록과 갱신할 시간표 목록이 완전히 동일함에도 불구하고 Hibernate 컬렉션 관리 메커니즘으로 인해 매번 시간표 데이터가 전체 삭제(`DELETE`)된 후 다시 삽입(`INSERT`)되는 비효율적인 쿼리 발생 현상이 관찰되었습니다.

### 가설 및 실험

* **실험 대상**:
  1. **청크 크기**: 100개 과목 (300개 시간표) - 1회 Spring Batch 트랜잭션 범위
  2. **전체 학기 크기**: 5,395개 과목 (16,185개 시간표) - 실제 1학기 전체 크롤링 데이터 규모
* **측정 방법**: Hibernate Statistics 기능을 활용하여 완전히 동일한 시간표로 엔티티 메타데이터를 업데이트할 때 발생하는 소요 시간과 `DELETE` 및 `INSERT` 쿼리 횟수를 측정하는 `CourseSchedulePerformanceTest`를 작성하고 수동 실행 (`./gradlew performanceTest`).

### 결과 (개선 전 Baseline)

* **1. 청크 크기 (100개 과목, 300개 시간표)**
  - **소요 시간**: 412.61 ms
  - **Entity Delete 횟수**: 300회
  - **Entity Insert 횟수**: 300회
* **2. 전체 학기 크기 (5,395개 과목, 16,185개 시간표)**
  - **소요 시간**: 51,325.98 ms (~51.3초)
  - **Entity Delete 횟수**: 16,185회
  - **Entity Insert 횟수**: 16,185회

동일한 데이터를 업데이트함에도 불구하고 매번 하위 시간표 테이블에 대해 총 32,370번의 I/O 쿼리가 강제 발생하며, 이로 인해 약 51.3초의 막대한 성능 지연이 유발됨을 Baseline 수치로 확정지었습니다.

### 해결책 및 최적화 방향

* **도메인 레벨 최적화**: `Course.updateMetadata()` 실행 시 새로운 시간표 리스트와 기존 시간표 리스트를 비교하여 변경 사항이 없는 경우 엔티티의 Schedule 컬렉션을 비우고 새로 채우는 오퍼레이션을 방지하고, 리스트의 변화가 감지된 경우에만 부분 변경을 수행하도록 로직을 개선했습니다.

### 개선 후 결과

* **1. 청크 크기 (100개 과목, 300개 시간표)**
  - **소요 시간**: 334.93 ms (약 **18.8% 단축**)
  - **Entity Delete 횟수**: **0회**
  - **Entity Insert 횟수**: **0회**
* **2. 전체 학기 크기 (5,395개 과목, 16,185개 시간표)**
  - **소요 시간**: 51,416.72 ms (유사 수준, 단 DB 디스크 I/O 트랜잭션 부하 완전 제거)
  - **Entity Delete 횟수**: **0회**
  - **Entity Insert 횟수**: **0회**

### 결과 분석 및 기대 효과

* **쿼리 수 대폭 절감**: 동일 시간표 기준 업데이트 시 발생하는 DELETE 및 INSERT 쿼리가 **32,370회에서 0회로 100% 제거**되었습니다.

---

## 80. GitHub Actions CI/CD 검증 파이프라인 중단 오류 해결

### 문제 상황

새로운 PR을 올리거나 배포를 진행할 때, GitHub Actions의 `verify-infra` 및 `verify-server` 두 잡에서 모두 에러가 발생하며 파이프라인이 중단되는 현상이 발생했습니다.
- `verify-infra`: `env file .../apps/server/.env not found` 발생.
- `verify-server`: `SecurityRequestAuthorizationTest` 등 13개 테스트 클래스에서 `FlywayMigrateException` 및 AWS SDK 관련 `SdkClientException` 발생.

### 원인 분석

1. **Docker Compose 환경 변수 참조 실패**:
   - `infra/docker-compose.yml`이 백엔드 시크릿 관리를 위해 `../apps/server/.env`를 참조하도록 수정되었습니다. 그러나 해당 파일은 `.gitignore` 대상이어서 CI 러너의 체크아웃 시점에는 존재하지 않아 `docker compose config` 파싱 시 에러가 났습니다.
2. **Flyway H2 SQL 문법 호환성 문제**:
   - 새로 추가된 `V12__add_user_data_constraints_and_cleanup.sql` 마이그레이션 파일 내 `ALTER TABLE timetables ADD COLUMN ... GENERATED ALWAYS AS (...) STORED` 구문에서 `STORED` 키워드가 H2 데이터베이스(테스트 환경)에서 인식되지 않아 문법 오류를 일으켰습니다.
   - 또한 H2 2.x에서는 `BOOLEAN` 타입과 숫자형(`0`/`1`)의 비교를 금지하여 `V12` 마이그레이션 내 `is_primary = 1` 구문 또한 타입 에러를 야기했습니다.
   - 나아가, 테스트 시 Flyway 자동설정이 돌면서 H2 환경과 호환되지 않는 MySQL 전용 구문(예: `SET FOREIGN_KEY_CHECKS=0`)이 들어있는 `V1__initial_schema.sql`를 파싱하려다 신택스 에러를 유발했습니다.
3. **AWS 자격 증명 누락**:
   - CI/CD 검증 러너 환경에는 실제 AWS credentials가 없기 때문에, 통합 테스트를 실행할 때 Spring context 로드 중 AWS SES 관련 빈이 생성되면서 default credential provider가 자격 증명을 찾지 못해 `SdkClientException`을 발생시켰습니다.

### 해결책

1. **인프라 검증 스크립트 수정 (`verify-compose-policy.sh`)**:
   - `.env`와 마찬가지로 `../apps/server/.env` 파일도 존재하지 않는 경우 `../apps/server/.env.example` 복사본을 생성해 임시 fallback을 제공하도록 보완하여 파싱 오류를 제거했습니다.
2. **Flyway 마이그레이션 쿼리 및 타입 비교 수정 (`V12__add_user_data_constraints_and_cleanup.sql`)**:
   - `STORED` 키워드를 삭제하여 H2와 MySQL 모두에서 호환 가능한 표준 `GENERATED ALWAYS AS (expression)` 구문으로 변경했습니다 (MySQL에서는 기본값인 `VIRTUAL`로 렌더링되나 인덱스 생성에 영향 없음).
   - H2 2.x와의 호환을 위해 `is_primary`에 대한 numeric 비교(`= 0`, `= 1`)를 `false` 및 `true` boolean 리터럴 비교로 치환했습니다.
3. **CI 워크플로 환경 변수 및 자동설정 제외 주입 (`cd.yml`)**:
   - `verify-server` 잡의 gradle check 실행 시 가짜 AWS credentials(`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`)를 주입하여 테스트 시 SDK 예외가 터지지 않고 context가 정상적으로 로드되도록 처리했습니다.
   - `SPRING_AUTOCONFIGURE_EXCLUDE: org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration` 환경변수를 주입하여, 테스트 환경 상에서 Flyway 자동구성을 원천 차단해 `V1__initial_schema.sql` 등 MySQL 전용 구문으로 인한 테스트 구동 에러를 해결했습니다.

### 결과

로컬 및 GitHub Actions 가상환경 양쪽 모두에서 인프라 보안 정책 검증과 스프링 테스트/코드 정적 검사(`check`)가 실패나 중단 없이 `BUILD SUCCESSFUL`로 완전히 통과함을 확인했습니다.

---

## 81. 모노레포 전환 후 CD 배포 중 Dockerfile 미검출 및 docker compose 서비스 누락 오류 해결

### 문제 상황

모노레포 마이그레이션 이후 GitHub Actions CD 워크플로(`cd.yml`)가 동작하는 과정에서 `deploy-server` 작업 중 두 가지 오류가 발생하며 배포에 실패했습니다.
1. `Dockerfile` 미검출 오류: `unable to prepare context: unable to evaluate symlinks in Dockerfile path: lstat /home/***/jbnu-sugang-helper/apps/server/Dockerfile: no such file or directory`
2. `app` 서비스 누락 오류: `err: no such service: app`

### 원인 분석

1. **상대 파일 경로 및 컴포넌트 스트립(strip_components) 누락**:
   - `appleboy/scp-action`은 기본적으로 원본 경로 구조를 대상 디렉토리 하위에 그대로 생성합니다.
   - 워크플로에서 `source`가 `apps/server/Dockerfile` 등으로 지정되었고, `strip_components` 옵션이 주어지지 않아 원격 서버의 `~/jbnu-sugang-helper/apps/server/apps/server/Dockerfile` 경로로 파일들이 복사되었습니다.
   - 이로 인해 `cd ~/jbnu-sugang-helper/apps/server` 후 `docker build` 실행 시 Dockerfile을 찾을 수 없게 되었습니다.
2. **인프라 설정 미동기화**:
   - 기존의 분리된 리포지토리 구성과 달리, 모노레포에서는 백엔드 앱을 띄우는 `app` 서비스 정의가 `infra/docker-compose.yml` 내에 포함되어 있습니다.
   - 하지만 CD 파이프라인에서 원격 서버의 `infra` 폴더 내 `docker-compose.yml` 등 최신 인프라 설정 파일을 업데이트해주는 단계가 누락되어 있었습니다.
   - 결과적으로 원격 서버는 모노레포 이전의 구버전 `docker-compose.yml`을 유지하고 있었고, `app` 서비스가 정의되어 있지 않아 `no such service: app` 오류가 발생했습니다.

### 해결책

1. **빌드 경로 플래닝 설정 수정 (`cd.yml`)**:
   - `deploy-server` 작업의 서버 파일 전송(`scp-action`) 단계에 `strip_components: 2` 속성을 명시적으로 부여하여, `apps/server/` 경로를 제외하고 빌드 결과물(`build/libs/app.jar`), `Dockerfile`, `.env` 등의 파일들이 `~/jbnu-sugang-helper/apps/server/` 디렉토리 바로 밑으로 복사되도록 경로 변환을 수행했습니다.
2. **인프라 설정 자동 전송 단계 신설 (`cd.yml`)**:
   - 서버로 소스 코드를 빌드해 배포하기 전, 최신 `infra/docker-compose.yml` 및 하위 모니터링/로깅 설정 파일들을 원격 서버의 `~/jbnu-sugang-helper/infra/`에 전송하는 단계를 추가했습니다 (`strip_components: 1`).
   - 이와 동시에 기존에 수동 또는 구버전으로 유지되고 있던 `docker-compose.yml`이 런타임에 동기화되도록 보강하여 `app` 서비스 참조 문제를 근본적으로 해결했습니다.

### 결과

- `apps/server/Dockerfile`을 비롯한 파일들이 원격 서버의 목적 경로에 올바르게 복사되어 `docker build` 단계가 정상 구동됩니다.
- `infra/docker-compose.yml`이 배포 프로세스 상에서 동기화되므로, `docker compose up -d --no-deps app` 명령 수행 시 `app` 서비스를 성공적으로 식별하고 무중단 재시작 프로세스가 정상 완료됩니다.

## Announcement pagination and search limits (ISSUE-094)

공지사항 public/admin 목록은 기본 20건, 최대 100건의 `Page`로 반환된다. `page`·`size`가 공통 pageable 상한을 넘으면 `400/G002`가 반환된다. 검색어는 trim 후 100자까지 허용하며, 빈 검색어도 동일한 페이지 상한을 적용한다. 제목/내용 `LIKE` 검색은 고정 공지 우선·최신순 정렬을 유지하고 `announcement.search.latency` metric에서 검색 타입과 키워드 유무별 지연을 확인한다. 현재 검색은 `%keyword%` 특성상 대용량 데이터에서 full scan이 될 수 있으므로 운영 데이터 증가 시 DB full-text/trigram 인덱스 도입 전후의 query plan을 비교한다.
