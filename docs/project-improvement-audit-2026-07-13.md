# 프로젝트 전반 개선 감사 보고서

감사일: 2026-07-13  
범위: 장애·보안·데이터 손실, 테스트·개발 생산성, UX·기능, 아키텍처·확장성

## 결론

2026-07-14 추가 종료: [106 (closed)](../.agents/issues/closed/ISSUE-106-SERVER-REJECT-WEBP-UPLOAD), [107 (closed)](../.agents/issues/closed/ISSUE-107-WEB-LINT-WARNINGS-CLEANUP). 107은 web lint 8개 경고와 Spring 테스트 unchecked 컴파일 경고를 제거했다.

현재 확인된 즉시 차단 수준(P0) 증거는 없지만, P1 개선이 필요한 보안·데이터·알림·운영 문제가 다수 있다. 특히 아래 두 가지는 구현 전까지 운영 리스크가 크다.

1. `DELETE /api/v1/users/devices/token/{token}`이 현재 사용자와 토큰 소유자의 일치 여부를 확인하지 않아, 토큰을 아는 요청자가 다른 사용자의 기기를 해제할 수 있다.
2. 알림 worker가 delivery와 target을 모두 직렬 처리한다. Outbox 커밋 후 5초 안에 모든 eligible delivery의 첫 시도를 시작한다는 합의된 SLA를 현재 구조만으로는 보장할 수 없다.
상태 갱신: 2026-07-13에 081, 082, 083, 084, 085, 086, 088, 090, 091, 092, 098, 100, 103, 104, 105를 종료했다. 토큰 기반 기기 해제는 현재 사용자 ID와 함께 범위를 제한하며, 새 웹 클라이언트는 토큰을 URL이 아닌 요청 본문으로 전송한다. 알림·크롤러·인증 오류는 원문 대신 마스킹·지문·안정 오류 코드와 correlation ID를 사용한다. Redis의 새 blacklist와 refresh registry는 원문 대신 SHA-256 digest·무작위 family를 쓰고, refresh 재사용은 원자적으로 family를 폐기한다. 탈퇴는 식별자·발송 대상을 즉시 제거하고 계정·비식별 이력은 soft delete로 보존하며, 인증은 불변 사용자 ID를 함께 검증한다. 삭제 피드백은 단일 관리자만 별도 projection으로 열람하며, 식별자·환경 메타 정보는 JSON에서 제외하고 첨부파일은 확인·감사 경계를 거친다. 새 피드백 환경 정보는 `os`·`language`만 작은 JSON으로 정규화하며 malformed input은 400으로 차단한다. 첨부 이미지는 실제 decoder·pixel 예산을 거쳐 metadata 없는 정적 JPEG/PNG로 정규화하고, 관리자 preview는 확인된 blob과 실제 bytes MIME만 사용한다. 웹 runtime은 temporary preview 예외와 audit/rollback 기준을 문서화하고 root lockfile을 CI 기준으로 통일했다. 운영 actuator는 별도 내부 management port와 최소 endpoint로 분리하고, Swagger/H2와 wildcard credential CORS를 차단했다. Flyway migration 검증은 기본 피드백 경로에서 분리했지만 PR·배포 gate는 유지했다. 100은 runtime·migrator·backup DB 계정을 분리하고, HMAC 검증 clean-host DR drill을 추가했다. 088은 delivery UUID key·claim token fencing·30일 DLQ 보존과 단일 관리자의 전체 DLQ 조회/선택 replay·감사 경계를 추가했다.

## 확정된 운영 기준

- 크롤러 주기: 매분.
- 알림 기준: Outbox 커밋 이후 모든 eligible delivery의 첫 발송 시도 5초 이내. 과부하 시 FIFO를 유지하고 유실하지 않으며, SLA 위반은 측정한다.
- 구독 상한: 제품 상한 없음. 용량 초과 시 지연은 허용하되 유실 금지.
- 알림 전달: at-least-once. provider가 지원하는 경우 idempotency를 사용하고, 불가피한 중복 가능성을 문서화한다.
- DLQ: 최소 30일 보존, 운영자 알림, 원인 수정 후 선택적 replay. 초기 알림 채널은 관리자 페이지, 이후 Prometheus/Grafana와 Discord webhook을 추가한다.
- replay: 동일 delivery/idempotency key 유지, 기본적으로 `SENT` replay 금지.
- 테스트: 기본/로컬 피드백 예산 3분. Flyway migration 검증은 운영 안전상 필요하므로 제거하지 않고 별도 실행 단위로 분리한다.
- 탈퇴: 즉시 접근 차단과 soft delete. 이메일·OAuth 식별자·device/refresh token은 즉시 폐기·익명화하고, 비식별 통계·필요한 감사 이력은 보존한다. 피드백 본문·첨부파일 soft delete 보존은 법률·개인정보 검토가 필요한 수용 위험이다.
- 관리자: 단일 관리자에게 전체 조회 권한을 허용한다. MFA/step-up은 필수 요구가 아니며, 계정 탈취·내부 오용 위험으로 기록한다.

## 우선순위 요약

| 우선순위 | 영역 | 이슈 |
| --- | --- | --- |
| P1 | 보안/데이터 | [081 (closed)](../.agents/issues/closed/ISSUE-081-SERVER-OWNERSHIP-CHECK-DEVICE-TOKEN), [082 (closed)](../.agents/issues/closed/ISSUE-082-SERVER-SOFT-DELETE-ACCOUNT-ANONYMIZE), [083 (closed)](../.agents/issues/closed/ISSUE-083-SERVER-ADMIN-DELETED-FEEDBACK-AUDIT), [086 (closed)](../.agents/issues/closed/ISSUE-086-WEB-AUTHENTICATED-ATTACHMENT-RENDERING), [090 (closed)](../.agents/issues/closed/ISSUE-090-SERVER-REDACT-OUTBOUND-SECRETS), [091 (closed)](../.agents/issues/closed/ISSUE-091-SERVER-HASH-REDIS-TOKENS), [092 (closed)](../.agents/issues/closed/ISSUE-092-SERVER-SECURITY-ENDPOINT-CORS-HARDENING) |
| P1 | 알림 | [087 (closed)](../.agents/issues/closed/ISSUE-087-SERVER-NOTIFICATION-FANOUT-SLA), [088 (closed)](../.agents/issues/closed/ISSUE-088-SERVER-NOTIFICATION-IDEMPOTENCY-DLQ-REPLAY), [089 (closed)](../.agents/issues/closed/ISSUE-089-SERVER-NOTIFICATION-PROVIDER-RESILIENCE) |
| P1 | API/크롤러 | [093 (closed)](../.agents/issues/closed/ISSUE-093-SERVER-API-PAGINATION-GUARDS), [094 (closed)](../.agents/issues/closed/ISSUE-094-SERVER-ANNOUNCEMENT-PAGINATION-SEARCH), [096 (closed)](../.agents/issues/closed/ISSUE-096-SERVER-CRAWLER-BULK-UPSERT), [097 (closed)](../.agents/issues/closed/ISSUE-097-SERVER-CRAWLER-FRESHNESS-UPSTREAM) |
| P1 | 인프라/운영 | [098 (closed)](../.agents/issues/closed/ISSUE-098-INFRA-REDIS-HEALTH-STARTUP), [099 (closed)](../.agents/issues/closed/ISSUE-099-INFRA-METRICS-ALERTING-DURABILITY), [100 (closed)](../.agents/issues/closed/ISSUE-100-INFRA-DB-LEAST-PRIVILEGE-DR) |
| P2 | UX/품질 | [084 (closed)](../.agents/issues/closed/ISSUE-084-SERVER-FEEDBACK-METADATA-SAFETY), [085 (closed)](../.agents/issues/closed/ISSUE-085-SERVER-ADMIN-ACTION-LOG-STRUCTURED), [095 (closed)](../.agents/issues/closed/ISSUE-095-SERVER-COURSE-SEARCH-VALIDATION), [101 (closed)](../.agents/issues/closed/ISSUE-101-INFRA-RESOURCE-LIMITS), [102 (closed)](../.agents/issues/closed/ISSUE-102-CI-E2E-CRITICAL-FLOWS), [103 (closed)](../.agents/issues/closed/ISSUE-103-CI-MIGRATION-TASK-ISOLATION), [104 (closed)](../.agents/issues/closed/ISSUE-104-WEB-PREVIEW-DEPENDENCY-STABILITY), [105 (closed)](../.agents/issues/closed/ISSUE-105-SERVER-UPLOAD-IMAGE-LIMITS), [106 (closed)](../.agents/issues/closed/ISSUE-106-SERVER-REJECT-WEBP-UPLOAD) |

## 근거 요약

### 보안·데이터

- `UserDeviceService.unregisterDevice()`는 token으로 row를 찾은 뒤 바로 삭제하며 현재 사용자 검증이 없다. ID 기반 삭제 메서드에는 검증이 있으므로 구현 불일치가 명확하다.
- 082에서 `User`가 상속한 `deletedAt`을 사용해 account row를 soft delete한다. 이름·이메일·알림 이메일·Discord 식별자는 제거 또는 익명화하고, refresh registry·현재 세션·기기 token·구독을 즉시 끊는다. 시간표·찜·리뷰·이력은 비식별 `user_id`로 보존하며 feedback과 첨부파일은 논리 삭제한다.
- `Feedback`는 `@SQLRestriction("deleted_at IS NULL")`을 유지한다. 083은 일반 JPA 경로를 다시 열지 않고 관리자 전용 native scalar projection으로 soft-deleted row를 읽는다. 응답은 generic author label·삭제 상태·첨부 ID만 노출하며, 첨부는 명시 확인 POST와 감사 로그를 거친다.
- `SecurityConfig`는 `/actuator/**`, Swagger, H2 console을 `permitAll`로 두고, CORS에서 `allowedOriginPatterns`와 credentials를 함께 사용한다. 운영 management endpoint는 같은 애플리케이션 경로에 노출된다.
- 091 전 `JwtProvider`는 blacklist key에 access token 원문을 사용하고, refresh token을 `RT:<email>` 값으로 저장했다. 이제 새 blacklist는 SHA-256 key, 새 refresh registry는 `v2:<family>:<SHA-256>` record를 사용하며 Lua CAS로 회전한다. 082에서 access/refresh token과 session에 불변 `userId`를 추가해 탈퇴 전 인증이 같은 이메일의 재가입 계정으로 해석되지 않도록 했다.
- 알림 sender와 crawler가 email, FCM token, Web Push endpoint, session cookie를 로그에 남길 수 있고, `GlobalExceptionHandler`는 `CustomException.detail`을 응답에 그대로 넣는다.

### 알림·확장성

- worker는 bounded thread/queue와 FIFO claim을 사용해 delivery를 병렬 처리한다. permit이 없으면 claim을 보류해 유실을 막고, claim lag·claim→first attempt·provider latency·channel queue depth를 계측한다. stale worker의 완료/실패 반영은 token이 다르면 무시한다.
- `NotificationService.deliverSeatOpening()`도 channel target을 순차 호출하고, provider timeout/circuit breaker가 없다.
- 088은 delivery별 UUID idempotency key를 retry/replay에도 유지하고 provider별 key 전파, 관리자 전용 DLQ 목록·상세·선택 replay, `DELIVERY_REPLAY` 감사를 추가했다. 089는 모든 provider 호출에 유한한 deadline을 적용하고 retryable/permanent 분류, 회로 차단, latency·timeout·HTTP 상태 metric을 추가했다. bounded fan-out/5초 SLA는 087의 잔여 범위다.
- `materializeDeliveries()`는 user/channel마다 `existsByOutboxIdAndUserIdAndChannel()`을 호출해 fan-out 때 N+1 쿼리가 생긴다.

### API·크롤러

- 공통 `PageableGuard`가 course/history/review/feedback/admin audit·DLQ·notification history의 page size를 최대 100, offset을 최대 10,000으로 제한하고 초과 요청은 G002 4xx로 거부한다. 거부 사유는 원문 입력 없이 reason 로그만 남긴다.
- 공지사항 public/admin API와 검색 repository가 `List`를 반환하며, service가 이미 있는 pageable overload를 사용하지 않는다. (094에서 bounded `Page`, 검색어 상한, latency metric으로 종료)
- `CourseSearchCondition`에는 `@Valid`, 문자열 길이, list 크기, 선택 시간표 개수 제한이 없다.
- `SpringBatchConfig`는 chunk의 course key를 한 번에 조회하고, schedule/seat history 처리는 기존 동작을 유지한다. `crawler.course.chunk.write` timer와 `crawler.course.chunk.items` counter로 chunk 비용을 확인한다.
- compose는 `oasis.jbnu.ac.kr`을 고정 IP로 덮어쓰지 않고 DNS를 사용한다. crawler는 run 상태, upstream latency, freshness age/stale, overlap skip을 metric으로 남기며 stale threshold 초과 시 관리자 경고를 기록한다.

### 인프라·품질

- 098에서 Redis authenticated `PING` healthcheck·`service_healthy` startup·AOF host bind·backup/restore drill을 완료했다. 앱 readiness에는 DB/Redis가 포함되고, Redis preflight 실패는 기존 deployment failure channel로 전달한다. 지속 alert route는 099에서 완성한다.
- Prometheus는 30일/20GB retention과 host bind volume을 사용하고, Alertmanager는 별도 persistent state volume·webhook route로 배포된다. Loki ruler와 Prometheus 모두 실제 `alertmanager:9093` 경로를 사용한다. SLO/DLQ/provider circuit/crawler freshness rule과 Grafana dashboard를 함께 provision한다.
- DB compose는 root password만 사용하며, `backup-log-state.sh`는 MySQL·Grafana·Nginx/cert 상태를 백업하지 않는다.
- 모든 Compose 서비스에 CPU/memory/pid limit과 restart 정책을 명시했다. 앱은 graceful shutdown 30초, notification worker는 8 thread/32 queue로 예산과 concurrency를 연결하고 compose policy smoke가 상한을 검증한다.
- web/server 테스트는 핵심 단위 테스트 중심이며 OAuth, refresh, 인증 첨부파일, 실제 push/fan-out의 browser/contract coverage가 없다.
- Flyway 검증은 MySQL Testcontainer 2개를 각각 시작한다. 103에서 이를 `migrationTest`로 분리했고, 최신 local report 기준 기본 suite는 238개/6.236초, migration suite는 2개/18.261초다. PR·main CI는 둘 다 실행하며 Docker 미가용은 skip이 아니라 명시 실패로 남긴다. static container 재사용은 schema isolation이 필요한 후속 최적화다.
- 104는 Next와 `eslint-config-next` preview version을 일치시키고 README·root workspace lockfile·Node 22 CI를 맞췄다. stable `16.2.10`이 PostCSS `8.4.31`을 직접 고정해 현재 preview는 audit-verified temporary exception이며, stable에 보안 수정이 직접 반영되면 전환한다.

107의 정적 검사 보강은 [107 (closed)](../.agents/issues/closed/ISSUE-107-WEB-LINT-WARNINGS-CLEANUP)에서 web lint 0건, Checkstyle/PMD 0건, Java unchecked 컴파일 경고 0건으로 검증했다.

## 이미 해결되어 중복 생성하지 않은 항목

기존 closed issue 071~080에서 Outbox 영속화, 부분 알림 실패 격리, 첨부파일 인증/수명주기, XML streaming, static analysis blocking, JWT 로그 원문 제거, non-root container, subscription N+1, PostCSS 취약점이 처리되었다. 이번 이슈는 그 위의 잔여 정책·운영·확장성 문제만 다룬다.

## 실행 순서 제안

각 이슈는 아래 순서대로 구현·검증·종료·커밋한다. 독립 항목도 커밋은 이슈별로 분리한다.

1. 081 (closed) → 090 (closed) → 092 (closed): 즉시 악용 가능한 기기 삭제, 비밀값 노출, 외부 관리 경로를 먼저 차단했다.
2. 103 (closed) → 091 (closed) → 082 (closed): migration 검증은 유지한 채 실행 단위를 분리했고, Redis 원문 토큰을 제거한 뒤 탈퇴 soft delete와 identity binding을 적용했다.
3. 085 (closed) → 083 (closed) → 104 (closed) → 084 (closed) → 105 (closed) → 086 (closed): 관리자 감사 기반을 만든 뒤 삭제 피드백·첨부파일 UX와 입력 안전성을 순서대로 보강했다.
4. 098 (closed) → 100 (closed): Redis readiness/저장 상태를 먼저 안전하게 만든 뒤 DB 최소 권한·백업·restore drill을 처리했다. 098은 091 이후에만 Redis 영속화를 도입했다.
5. 088 (closed) → 089 (closed) → 087 (closed): idempotency·DLQ replay 상태를 먼저 정의한 뒤 provider timeout/실패 분류와 bounded fan-out·5초 SLA를 구현했다. 기존 087~089의 순환 의존은 이 순서로 해소했다.
6. 096 (closed) → 097 (closed) → 099 (closed) → 101 (closed): 매분 크롤러 부하와 upstream 고정 의존을 줄이고 freshness metric/Alertmanager·Prometheus 영속화와 resource limit을 확정했다.
7. 093 (closed) → 094 (closed) → 095 (closed) → 102 (closed): 공통 API pagination/input 제한을 정리한 뒤 핵심 HTTP/browser/provider 흐름을 CI smoke와 nightly E2E로 검증한다.

## 검증 상태

087은 bounded FIFO worker·channel별 SLA/queue metric과 부하 회귀 테스트를, 089는 provider deadline·회로 차단·실패 분류와 관련 회귀 테스트를, 093은 공통 pageable 상한과 oversized boundary 테스트를, 094는 공지사항 public/admin `Page`·검색어 100자 상한·빈 검색 bounded query·`announcement.search.latency` metric과 경계 테스트를, 095는 course search DTO의 Bean Validation·enum whitelist·시간/학점 범위와 oversized/invalid API 400 테스트를, 096은 chunk bulk lookup·비용 metric과 query-count 회귀 테스트를, 097은 DNS-safe upstream·retry 분류·freshness/skip metric과 stale 관리자 경고 테스트를, 099는 Prometheus/Alertmanager 영속화·실제 route·SLO rule/dashboard smoke를, 101은 서비스별 resource budget·graceful shutdown·concurrency 정책과 compose smoke를, 102는 Playwright session refresh·관리자 첨부 preview browser smoke와 provider/outbox/auth/feedback contract suite를, 106은 WebP 업로드 400 거부·JPEG/PNG 프론트 압축·TwelveMonkeys 제거를 별도 커밋으로 종료했다.

081, 082, 083, 084, 085, 086, 088, 090, 091, 092, 098, 100, 103, 104, 105는 이슈별 회귀 테스트와 별도 커밋으로 종료했다. 082는 soft delete·식별자 익명화·device/refresh 폐기·구독 비활성화·feedback soft delete, 탈퇴 전 token/session의 재가입 계정 차단, 탈퇴 사용자 알림 제외, V17의 기존 계정 optimistic-lock version backfill을 검증했다. 083은 삭제 피드백/답변의 관리자 전용 read model, 일반 첨부 경로의 관리자 우회 차단, 명시 확인·감사 다운로드, 삭제 부모의 답변 mutation 차단, 실제 attachment 보존을 검증했다. 086은 actual JPEG/PNG MIME blob preview, legacy download fallback, object URL lifecycle, role/feedback–attachment pair MVC 경계를 검증했다. 088은 V19 backfill·unique key와 DLQ retention, 동일 key replay/SENT override, concurrent replay·stale worker fencing, provider key contract, 관리자 role 경계와 DLQ UI/service worker notification tag를 검증했다. 098은 Redis authenticated healthcheck·DB/Redis readiness·2초 bounded fail-fast reconnect·AOF persistence·checksum restore drill·deploy preflight를 검증했다. 100은 runtime DML·one-shot migration·local backup 계정을 실제 MySQL/Flyway로 검증하고, HMAC·특수 tar 항목 거부·checksum·clean-host DB/uploads/Grafana/NPM recovery drill을 검증했다. 104는 root workspace clean install 뒤 lint/test가 깨지지 않도록 hoisted peer를 root에 선언하고, version mismatch·dual lockfile을 정리했으며, full web suite/build/audit을 통과했다. 091은 raw Redis key/value와 raw JWT session attribute를 새로 쓰지 않는지, 동일 family replay, Redis state 유실 fail-closed, Spring Security session 인증과 access 만료 시각 강제를 검증했다. 기본 server suite와 정적 분석도 통과했다.

082의 identity binding rollout에서는 `uid` claim 또는 session user ID가 없는 배포 전 인증을 fail-closed한다. 기존 access/refresh token과 session은 한 번 재로그인이 필요하며, 남은 raw Redis value는 기존 TTL 안에 자연 만료된다. 탈퇴 보존표와 purge 금지 runbook은 [account-withdrawal-retention.md](account-withdrawal-retention.md)에 기록했다. Redis는 AOF host volume으로 restart state를 보존하며, abrupt host loss의 마지막 약 1초 write는 098의 명시적 잔여 위험이다.
