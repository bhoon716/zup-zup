# 프로젝트 전반 개선 감사 보고서

감사일: 2026-07-13  
범위: 장애·보안·데이터 손실, 테스트·개발 생산성, UX·기능, 아키텍처·확장성

## 결론

현재 확인된 즉시 차단 수준(P0) 증거는 없지만, P1 개선이 필요한 보안·데이터·알림·운영 문제가 다수 있다. 특히 아래 세 가지는 구현 전까지 운영 리스크가 크다.

1. `DELETE /api/v1/users/devices/token/{token}`이 현재 사용자와 토큰 소유자의 일치 여부를 확인하지 않아, 토큰을 아는 요청자가 다른 사용자의 기기를 해제할 수 있다.
2. 알림 worker가 delivery와 target을 모두 직렬 처리한다. Outbox 커밋 후 5초 안에 모든 eligible delivery의 첫 시도를 시작한다는 합의된 SLA를 현재 구조만으로는 보장할 수 없다.
3. 회원 탈퇴가 `User`와 연관 데이터를 hard delete한다. 합의한 “접근은 즉시 차단, 계정 row는 soft delete, 식별자·토큰은 즉시 폐기/익명화, 비식별 이력 보존” 정책이 아직 구현되지 않았다.

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
| P1 | 보안/데이터 | [081](../.agents/issues/open/ISSUE-081-SERVER-OWNERSHIP-CHECK-DEVICE-TOKEN), [082](../.agents/issues/open/ISSUE-082-SERVER-SOFT-DELETE-ACCOUNT-ANONYMIZE), [083](../.agents/issues/open/ISSUE-083-SERVER-ADMIN-DELETED-FEEDBACK-AUDIT), [086](../.agents/issues/open/ISSUE-086-WEB-AUTHENTICATED-ATTACHMENT-RENDERING), [090](../.agents/issues/open/ISSUE-090-SERVER-REDACT-OUTBOUND-SECRETS), [091](../.agents/issues/open/ISSUE-091-SERVER-HASH-REDIS-TOKENS), [092](../.agents/issues/open/ISSUE-092-SERVER-SECURITY-ENDPOINT-CORS-HARDENING) |
| P1 | 알림 | [087](../.agents/issues/open/ISSUE-087-SERVER-NOTIFICATION-FANOUT-SLA), [088](../.agents/issues/open/ISSUE-088-SERVER-NOTIFICATION-IDEMPOTENCY-DLQ-REPLAY), [089](../.agents/issues/open/ISSUE-089-SERVER-NOTIFICATION-PROVIDER-RESILIENCE) |
| P1 | API/크롤러 | [093](../.agents/issues/open/ISSUE-093-SERVER-API-PAGINATION-GUARDS), [094](../.agents/issues/open/ISSUE-094-SERVER-ANNOUNCEMENT-PAGINATION-SEARCH), [096](../.agents/issues/open/ISSUE-096-SERVER-CRAWLER-BULK-UPSERT), [097](../.agents/issues/open/ISSUE-097-SERVER-CRAWLER-FRESHNESS-UPSTREAM) |
| P1 | 인프라/운영 | [098](../.agents/issues/open/ISSUE-098-INFRA-REDIS-HEALTH-STARTUP), [099](../.agents/issues/open/ISSUE-099-INFRA-METRICS-ALERTING-DURABILITY), [100](../.agents/issues/open/ISSUE-100-INFRA-DB-LEAST-PRIVILEGE-DR) |
| P2 | UX/품질 | [084](../.agents/issues/open/ISSUE-084-SERVER-FEEDBACK-METADATA-SAFETY), [085](../.agents/issues/open/ISSUE-085-SERVER-ADMIN-ACTION-LOG-STRUCTURED), [095](../.agents/issues/open/ISSUE-095-SERVER-COURSE-SEARCH-VALIDATION), [101](../.agents/issues/open/ISSUE-101-INFRA-RESOURCE-LIMITS), [102](../.agents/issues/open/ISSUE-102-CI-E2E-CRITICAL-FLOWS), [103](../.agents/issues/open/ISSUE-103-CI-MIGRATION-TASK-ISOLATION), [104](../.agents/issues/open/ISSUE-104-WEB-PREVIEW-DEPENDENCY-STABILITY), [105](../.agents/issues/open/ISSUE-105-SERVER-UPLOAD-IMAGE-LIMITS) |

## 근거 요약

### 보안·데이터

- `UserDeviceService.unregisterDevice()`는 token으로 row를 찾은 뒤 바로 삭제하며 현재 사용자 검증이 없다. ID 기반 삭제 메서드에는 검증이 있으므로 구현 불일치가 명확하다.
- `UserService.withdraw()`는 구독·기기·리뷰·알림 이력·피드백·찜·시간표를 삭제하고 `userRepository.delete(user)`를 호출한다. `User`에는 `deletedAt`이 없다.
- `Feedback`는 `@SQLRestriction("deleted_at IS NULL")`을 사용한다. 따라서 현재 관리자 `findAll(pageable)`도 soft-deleted row를 볼 수 없고, 탈퇴 사용자 표시·마스킹·첨부파일 확인 흐름도 없다.
- `SecurityConfig`는 `/actuator/**`, Swagger, H2 console을 `permitAll`로 두고, CORS에서 `allowedOriginPatterns`와 credentials를 함께 사용한다. 운영 management endpoint는 같은 애플리케이션 경로에 노출된다.
- `JwtProvider`는 blacklist key에 access token 원문을 사용하고, refresh token을 `RT:<email>` 값으로 Redis에 저장한다.
- 알림 sender와 crawler가 email, FCM token, Web Push endpoint, session cookie를 로그에 남길 수 있고, `GlobalExceptionHandler`는 `CustomException.detail`을 응답에 그대로 넣는다.

### 알림·확장성

- worker는 5초 fixed delay 뒤 `claimReadyDeliveryIds()` 결과를 `for` 루프로 하나씩 처리한다.
- `NotificationService.deliverSeatOpening()`도 channel target을 순차 호출하고, provider timeout/circuit breaker가 없다.
- delivery에는 retry 상태는 있으나 provider idempotency key, replay API/UI, 접근 감사가 없다.
- `materializeDeliveries()`는 user/channel마다 `existsByOutboxIdAndUserIdAndChannel()`을 호출해 fan-out 때 N+1 쿼리가 생긴다.

### API·크롤러

- `CourseController`의 search는 외부 `Pageable`을 그대로 `CourseService.searchCourses()`에 전달한다. history만 최대 100으로 제한한다.
- 공지사항 public/admin API와 검색 repository가 `List`를 반환하며, service가 이미 있는 pageable overload를 사용하지 않는다.
- `CourseSearchCondition`에는 `@Valid`, 문자열 길이, list 크기, 선택 시간표 개수 제한이 없다.
- `SpringBatchConfig.processCourse()`는 chunk 내부 각 DTO마다 `findByCourseKey()`를 호출한다.
- compose가 `oasis.jbnu.ac.kr`을 고정 IP로 `extra_hosts`에 매핑하고, crawler freshness/lag metric이 없다.

### 인프라·품질

- Redis healthcheck가 없고 app은 `service_started`만 기다린다. Redis 상태 저장 volume도 없다.
- Prometheus에 영속 volume이 없고, Loki ruler는 compose에 없는 `localhost:9093` Alertmanager를 가리킨다.
- DB compose는 root password만 사용하며, `backup-log-state.sh`는 MySQL·Grafana·Nginx/cert 상태를 백업하지 않는다.
- 서비스별 CPU/memory/pid limit이 없다.
- web/server 테스트는 핵심 단위 테스트 중심이며 OAuth, refresh, 인증 첨부파일, 실제 push/fan-out의 browser/contract coverage가 없다.
- Flyway 검증은 MySQL Testcontainer 2개를 각각 시작한다. 기존 측정에서 약 28초가 걸렸고, 전체 server suite는 약 36초였다. 3분 예산 안에서는 불필요하지 않지만 별도 migration task로 분리하면 로컬 피드백이 빨라진다.
- `apps/web/package.json`은 Next `16.3.0-preview.5`를 사용하고 README는 16.1.6을 가리킨다.

## 이미 해결되어 중복 생성하지 않은 항목

기존 closed issue 071~080에서 Outbox 영속화, 부분 알림 실패 격리, 첨부파일 인증/수명주기, XML streaming, static analysis blocking, JWT 로그 원문 제거, non-root container, subscription N+1, PostCSS 취약점이 처리되었다. 이번 이슈는 그 위의 잔여 정책·운영·확장성 문제만 다룬다.

## 실행 순서 제안

각 이슈는 아래 순서대로 구현·검증·종료·커밋한다. 독립 항목도 커밋은 이슈별로 분리한다.

1. 081 → 090 → 092: 즉시 악용 가능한 기기 삭제, 비밀값 노출, 외부 관리 경로를 먼저 차단한다.
2. 103 → 091 → 082: migration 검증은 유지한 채 실행 단위를 분리하고, Redis 원문 토큰을 제거한 뒤 탈퇴 soft delete를 적용한다.
3. 085 → 083 → 104 → 084 → 105 → 086: 관리자 감사 기반을 만든 뒤 삭제 피드백·첨부파일 UX와 입력 안전성을 순서대로 보강한다.
4. 098 → 100: Redis readiness/저장 상태를 먼저 안전하게 만든 뒤 DB 최소 권한·백업·restore drill을 처리한다. 098은 091 이후에만 Redis 영속화를 도입한다.
5. 088 → 089 → 087: idempotency·DLQ replay 상태를 먼저 정의하고 provider timeout/실패 분류를 붙인 뒤 bounded fan-out과 5초 SLA를 구현한다. 기존 087~089의 순환 의존은 이 순서로 해소한다.
6. 096 → 097 → 099 → 101: 매분 크롤러 부하를 줄이고 freshness metric을 만든 다음, 공통 alert route·Prometheus 영속화·자원 한계를 확정한다. 097·098의 실제 알림 연결은 099에서 닫는다.
7. 093 → 094 → 095 → 102: API pagination/input 제한을 정리한 뒤 핵심 HTTP/browser/provider 흐름을 CI smoke와 nightly E2E로 검증한다.

## 검증 상태

이번 단계에서는 사용자가 요청한 대로 제품 코드 구현은 하지 않았다. 파일·설정·테스트 구조를 정적으로 대조해 이슈 초안을 만들었고, 현재 작업 트리는 감사 문서와 ignored grill log를 제외하면 clean이었다. 각 이슈의 구현 단계에서 acceptance criteria에 맞는 회귀·통합·부하 테스트를 추가해야 한다.
