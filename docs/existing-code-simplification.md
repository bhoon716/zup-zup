# 기존 코드 단순화 작업

## 사용자 요청과 경계

2026-10-26 요청: 전면 백엔드 재작성은 중단하고 기존 코드를 새 브랜치에서 최대한 단순하게 만든다. 기능·공개 API·인증/권한·데이터 정합성과 수집·검색·시간표·구독·알림·취소·재시도를 보존한다. 새 기술, 스키마 변경, 운영 작업을 전제로 삼지 않는다.

원본 작업 폴더의 미커밋 변경을 보존한다. push, PR, merge, 배포, 운영 DB 변경, 실사용자 발송, 자격 증명·권한 변경은 실행하지 않는다.

## 기준 확인

- 원본: `/Users/bhoon/Project/jbnu-sugang-helper`.
- 원격 HEAD와 main: `8efb5c4e544a9cd861fb6cbfec2b6cd229c05c7a`.
- 원격 dev: `9e81c0a0adea8ad2c7c8587c92869da55209874f`.
- 로컬 dev: `aade57db93f213c98b035f979362075b6f2bf425`.
- 중단된 재작성 브랜치: `codex/backend-rewrite` (`3515b941`). 작업 기준으로 사용하지 않는다.
- 시작 시 원본에는 수정 파일 6개와 미추적 문서 1개가 있고, 등록된 worktree는 원본 하나였다.
- 분석용 격리 worktree: `/Users/bhoon/Documents/Codex/2026-10-26/task/zup-zup-simplify`, 브랜치 `codex/simplify-existing-20261026`. 현재 로컬 dev에서 생성했다.

**확정 기준:** 사용자 요청 `dev 기준으로 잡아`에 따라 로컬 dev `aade57db`를 기준으로 구현했다. MVC·저장소 통합과 RestClient 전환을 유지한다. 계절학기 수정은 **dev HEAD에는 없지만 원본 dev 작업 폴더의 미커밋 수정에는 이미 있었다**. main `5f73a8a4`/PR #34의 수정과 대조해 격리 브랜치에 보존했으며, 초기 보존 시 파서 및 두 회귀 테스트가 원본 미커밋 파일과 바이트 단위로 동일함을 확인했다. 후속 공유 타입 분리에서는 타입명·import만 갱신했다. 원본 파일은 수정하지 않았고 중복 적용도 하지 않았다.

`git diff --quiet aade57db origin/main -- apps/web`는 종료 코드 0이다. 웹 코드는 두 선택지에서 동일하므로 웹 중복 정리와 기존 테스트 확인은 기준 선택과 독립적으로 진행할 수 있다.

## 단순화 설계

기준은 읽고 따라갈 경로와 같은 업무 규칙의 중복을 줄이는 것이다. 파일 수 자체는 성공 지표로 삼지 않는다.

| 범위 | 실제 근거 | 선택 |
| --- | --- | --- |
| 웹 일정 API | `useSchedules`와 `useAdminSchedules`에 같은 목록/생성/수정/삭제 HTTP 처리 반복 | 기존 기능별 API 모듈 관례대로 일정 API를 한 곳에 정의한다. 기존 훅 이름·query key·staleTime·enabled·mutation 반환값·실패 전파·성공 시 무효화 범위는 보존한다. |
| 강의 반영 → outbox | 이벤트 publisher의 실제 수신자가 BEFORE_COMMIT outbox 저장 한 곳 | 같은 트랜잭션에서 직접 enqueue한다. MANDATORY로 바깥 트랜잭션 참여를 강제하며 강의·시간표·좌석 이력·성공 상태·outbox 전체 롤백을 유지한다. |
| 구독 조회 전달 서비스 | 수신자/통계 조회 서비스가 각각 repository 호출 하나와 단일 구현 인터페이스 하나로 구성 | 4개 파일을 읽기 전용 SubscriptionQueryService 하나로 통합한다. worker에 구독 명령 서비스의 사용자·강의·수집 의존성을 유입하지 않는다. |
| 강의 서비스 계약 | CourseCatalogSynchronizer, CourseCatalogQuery, CourseReviewCatalog가 단일 구현의 메서드 선언만 중복 | 계약 3개를 제거하고 같은 기존 서비스를 주입한다. 서비스 본문·DTO·잠금·트랜잭션은 보존한다. |
| 발송·정산 경계 | claim token, lease, provider 호출 전후의 짧은 트랜잭션과 재시도/DLQ 책임 | 유지한다. 역할 있는 실패·동시성 처리 계층을 파일 수 때문에 합치지 않는다. |

API 경로·응답 형태·채널 정책·여석 발생 조건·기존 구독 취소 효과를 바꾸지 않는다. 이미 만들어진 delivery에 구독 취소를 새로 적용하는 등 기능 정책 변경은 별도 사용자 판단 사항이다.

## 보존할 기능 계약

| 기능 | 계약과 근거 테스트 |
| --- | --- |
| 인증/권한 | 공개 검색·조회 유지, 관리자 경로 ADMIN, 나머지 인증 및 소유권 검사. `SecurityRequestAuthorizationTest`, OAuth·디바이스 테스트. |
| 수집 | 전체 fetch/parse/검증 후 DB 반영. 성공 반영 원자성, 실패 별도 트랜잭션, 중복 실행 방지. `CourseCrawlCompletionTransactionTest`, `CrawlerRunStateServiceTest`. |
| 검색 | 필터·페이지 검증, 찜 사용자 식별, 기존 검색·정렬·공강 의미. `CourseSearchReadRepositoryTest`, `CourseServiceTest`. |
| 시간표 | 사용자당 10개, 강의 10개, 소유권·중복 강의 검사, 대표 승계, 가입 기본 시간표. `TimetableServiceTest`. |
| 구독/취소 | 강의·학기·한도·중복 검사, 본인 개별/전체 삭제, 탈퇴 비활성화, bulk 조회. `SubscriptionServiceTest`, `SubscriptionServiceQueryCountTest`. |
| 알림/재시도 | 활성 수신자 materialize, delivery 중복 방지, lease/claim fencing, provider 실패 격리, 재시도/DLQ/재처리, 성공 이력. 기존 outbox·delivery·provider 계약 및 동시성 테스트. |
| 기타 | 찜, 리뷰/이모지/reaction, 대시보드, D-day, 공지/일정, 문의/첨부/감사 API와 테스트를 보존한다. |

## 검증 계획 및 수정 전 결과

1. Java 21에서 `./gradlew --offline check`로 기존 회귀와 정적 검사 상태를 확인한다.
2. Node 22에서 잠금 파일 기준 `npm ci --legacy-peer-deps --offline`, 전체 Vitest, ESLint, TypeScript와 build를 실행한다.
3. 실제 변경 경로의 HTTP 입력/출력·cache 갱신·실패 동작을 검증한다. build 후 localhost의 기존 Playwright 두 시나리오를 실행한다.
4. 백엔드 관련 단위 테스트와 기존 H2 원자성·동시성 회귀 테스트를 실행하고 전체 check와 bootJar를 검증한다.
5. 변경 전후 원본 git 상태를 확인하여 기존 변경을 건드리지 않았음을 확인한다.

수정 전 서버: **117개 클래스, 494개 테스트 통과, 실패/오류/skip 0**. Checkstyle은 통과했으나 PMD main 6개/test 13개 위반으로 전체 check는 실패했다. 위반은 기존 unused import/parameter/assignment와 중복 문자열이며 수정으로 생긴 실패와 구분한다. 검사·테스트를 비활성화하지 않는다.

수정 전 웹: 잠금 파일 기준 오프라인 설치 성공. **79개 파일, 276개 테스트 통과**.

선택 대기 중 원격 main(`8efb5c4e`)도 별도 소스 스냅샷 `/Users/bhoon/Documents/Codex/2026-10-26/task/baseline-main`에서 확인했다. **108개 클래스, 491개 테스트 통과, 실패/오류/skip 0**이며 PMD main/test 위반 0, Checkstyle 포함 **`./gradlew --offline check` 전체 통과**다. 위의 PMD 19개 실패는 로컬 dev 기준선의 문제이며 원격 main에는 없다. 스냅샷 생성·테스트는 원본 checkout이나 기준 브랜치를 변경하지 않는다.

서버 기본 suite는 H2와 mock/축소 Spring 컨텍스트를 사용한다. 필요한 통합 검증을 요청한 사용자 지시에 따라 기존 원자성·동시성 테스트도 보존한다. `manualTest`는 prod 프로필 수집·실메일 발송을 포함하므로 실행하지 않는다. 일반 bootRun이나 실학교 API, 운영 DB/Redis, 실발송으로 검증하지 않는다. Playwright는 API mock을 쓰므로 전체 백엔드 E2E로 해석하지 않는다.

## 1차 진행 결과 (`34bfb20ea`)

### 기준 선택과 독립적인 웹 수정

일정 화면과 관리자 패널의 중복 HTTP 처리를 `features/schedule/api/schedule.api.ts`로 모았다. 새로운 프레임워크·factory·범용 CRUD 계층 없이 기존 기능별 API 관례를 적용했다. 두 훅의 서로 다른 cache 정책은 그대로 유지했다.

두 관리자 진입점의 생성/수정 결과, 삭제 실패 전파와 성공 시 무효화, 공개 조회 enabled 및 관리자 cache key 보존을 확인하는 회귀 테스트 7개를 추가했다. 기존 테스트는 삭제·비활성화하지 않았다. 독립 읽기 전용 검토에서도 구체적인 회귀가 발견되지 않았다.

| 검증 | 결과 |
| --- | --- |
| 변경 집중 Vitest | 2개 파일, 8개 테스트 통과 |
| 전체 Vitest | 80개 파일, 283개 테스트 통과 |
| ESLint | 통과 |
| TypeScript `tsc --noEmit` | 통과 |
| Next production build | 통과. 최초 sandbox 실행은 기존 Google Fonts 다운로드 제한으로 실패했고 공개 폰트 다운로드 허용 후 통과. API 주소는 localhost로 제한. |
| 기존 Playwright | localhost:3100, API mock, Chromium 2개 시나리오 통과 |
| `git diff --check` | 통과 |

### 백엔드 구현 및 최종 검증

- 수집 완료 → 강의 동기화 → outbox가 코드상 직접 연결된다. 이벤트 publisher와 listener 추적을 없애고 기존 단일 트랜잭션에 outbox가 반드시 참여하도록 했다. 외부 fetch/parse는 계속 DB 트랜잭션 밖에 있고 실패 기록의 REQUIRES_NEW는 유지한다.
- 단일 구현 강의 계약 3개와 구독 조회 계약·구현 4개를 정리했다. 대체 구독 조회 서비스는 하나이며 조회 DTO와 읽기/쓰기 트랜잭션·리뷰 잠금은 그대로다.
- 실제 구독 조회 테스트에서 기존 dev의 `findUserIdByCourseKeyAndIsActiveTrue`가 엔티티를 Long으로 반환하려는 오류를 발견했다. userId projection과 활성/과목 조건을 명시한 JPQL로 수정했다. 새 테스트는 취소된 구독 및 다른 과목 제외, 없는 과목 빈 결과, 전체 활성 집계를 확인한다.
- 여석 최초 발생만 outbox를 기록하는지, outbox 저장 실패가 전파되는지, 다음 청크의 outbox 실패가 앞선 100개 강의·좌석 이력까지 롤백하는지, 트랜잭션 없는 outbox 호출이 거부되는지 검증을 추가했다. 기존 commit/rollback·재시도·동시성 테스트는 유지했다.
- 기존 PMD 위반은 미사용 import/인자 제거와 테스트 문자열 상수화로 정리했다. CourseSummary의 본문은 원래 compact constructor 그대로 유지한다. PMD 7.5의 record 암묵적 필드 대입 오탐만 생성자 단위 `SuppressWarnings`로 제한했고 전역 규칙은 바꾸지 않았다. 별도 javac 비교로 null 정규화·방어적 복사·불변성·null 원소 거부·record 필드 의미가 같음을 확인했다.
- 두 독립 읽기 검토에서 구체적인 도입 회귀나 테스트 삭제/약화를 발견하지 못했다. 실제 구현이 여러 개인 채널 발송, 사용자 조회의 서로 다른 DTO/필터, 발송과 정산·claim/lease·재시도/DLQ 경계는 유지했다.

1차 `./gradlew --offline check bootJar`: **118개 클래스, 515개 테스트 통과, 실패/오류/skip 0**. PMD main/test **0건**, Checkstyle main/test **0건**, 실행 JAR 패키징 통과. 초기 집중 검증에서 발생했던 구독 조회 오류와 새 테스트의 잔여 데이터 격리 문제를 수정한 뒤 전체 검증이 통과했다. 실패를 숨기거나 테스트를 비활성화하지 않았다.

변경하지 않은 공개 컨트롤러·보안 설정·DB migration·알림 채널 정책과 기존 구독 취소 효과를 유지한다. MySQL의 실제 잠금/flush 차이, 실제 외부 provider, 서버 전체 HTTP E2E·운영 부하는 검증하지 않았다. H2 및 mock 기반 검증을 그 범위까지 확대 해석하지 않는다. 스키마 변경이 없으므로 migrationTest는 실행하지 않았다.

최종 서버 로그는 `server-check-final.log`, 중간 집중 검증은 `backend-simplification-focused.log`, 계절학기 보존 검증은 `seasonal-regression.log`다. 검증 로그는 작업 디렉터리 `/Users/bhoon/Documents/Codex/2026-10-26/task/`의 `baseline-server-check.log`, `baseline-main-server-check.log`, `baseline-web-test.log`, `web-schedule-test.log`, `web-test-final.log`, `web-lint.log`, `web-typecheck.log`, `web-build-network.log`, `web-e2e.log`에 보관했다. 서버의 JUnit/PMD/Checkstyle 보고서는 격리 worktree와 main 스냅샷의 `apps/server/build/` 아래에 있다.

원본의 git status 및 수정·미추적 파일 7개의 SHA-256을 비교하여 격리 작업 중 원본 내용이 보존됐음을 확인했다. 변경은 로컬 브랜치에서 기능별 커밋으로 기록한다. 원격 쓰기·병합·배포·운영 데이터/자격 증명 변경·실발송은 실행하지 않았다.

## 로컬 변경 단위

| 커밋 | 범위 |
| --- | --- |
| `a56eae5f1` | 기존 계절학기 수정과 회귀 검증 보존 |
| `d36483d15` | 구독 조회 중복 통합과 활성 수신자 ID 조회 수정 |
| `e5c34c806` | 강의 반영 → outbox 직접 연결과 원자성 회귀 |
| `860c0386b` | 강의 조회 중복 계약과 기존 정적 분석 문제 정리 |
| `cea15f00d` | 웹 일정 HTTP 처리 통합과 기존 캐시 계약 회귀 |

모든 변경은 기능 보존 범위다. 취소된 구독이 이미 생성된 delivery를 취소하도록 바꾸거나 알림 보장을 축소하지 않았다. 필요한 사용자 정책 판단이나 승인 대기 작업은 남아 있지 않다.


## 추가 구현: 애플리케이션 작업을 순차 실행

| 이전 구조 | 변경 결과 |
| --- | --- |
| outbox worker 8개, 대기열 32개, semaphore, executor 제출·거부·종료 처리 | batch 상한(기본 50) 안에서 한 건 선점 → 처리·정산 → 다음 건 선점. 별도 발송 worker/대기열/semaphore 제거 |
| Slack `@Async`, 전용 `applicationTaskExecutor`, `@EnableAsync` | 공개 alert를 호출 스레드에서 직접 실행. 중복 억제·민감정보 가림·실패 격리·MDC 유지 |
| 환경의 virtual-thread 설정에 따라 달라지는 예약 실행 기반 | Spring이 관리하는 `ThreadPoolTaskScheduler` 하나, pool size 1로 고정. HTTP 요청·DB pool은 직렬화하지 않음 |
| 단일 구현 `NotificationProviderExecutor` 인터페이스 | 기존 `NotificationProviderResilience` 직접 사용. 구현의 timeout·회로·실패 분류는 동일 |

같은 worker의 polling이 겹치면 CAS로 건너뛴다. `fixedDelay`는 이전 처리가 완료된 뒤부터 계산되며, 주기보다 처리가 오래 걸려도 같은 작업을 병렬로 쌓지 않는다. 수집 시작·관리자 HTTP 요청은 scheduler 밖일 수 있으므로 `CourseCrawlerService.isCrawling` CAS도 유지했다.

한 번에 여러 delivery를 선점해 순서대로 기다리게 하면 뒤 순번의 lease가 발송 전에 만료될 수 있다. 그래서 `claimReadyDeliveries(1)`만 호출한다. 한 건의 RuntimeException은 기록하고 다음 건을 처리한다. 정산 실패로 PROCESSING에 남은 건은 기존 lease 만료 복구가 처리한다. 컨텍스트 종료 이벤트에서 새 선점을 막고 현재 건 뒤에는 다음 건을 가져오지 않는다. scheduler 종료는 진행 중 호출을 interrupt할 수 있으므로 현재 건의 완료를 보장하지는 않는다. 강제 중단·프로세스 장애 시에도 DB에 남은 작업은 기존 복구 경로를 따른다.

필수로 유지한 별도 실행 경계는 두 곳이다. 제공자 Future 결과 대기는 기본 5초, 이미지 decode 결과 대기는 기본 2초다. Firebase의 재시도, WebPush의 무제한 Future 대기, timeout 없는 ImageIO read/encode를 같은 호출 스레드에서 실행하면 동등한 대기시간 제한을 유지할 수 없다. 이미지 worker는 이미 1개·대기열 0으로 이미지끼리 병렬 실행하지 않는다. 두 경계의 취소는 best effort이며 원격 발송·codec 연산의 강제 종료를 보장하지 않는다. servlet·DB·SDK의 필수 실행 기반도 제거하지 않았다.

처리량·지연은 운영에서 측정하지 않았다. 발송 동시성은 8→1이므로 느린 건이 뒤의 건과 다른 예약 작업을 지연시킨다. 각 delivery의 대상이 1개이고 기본 batch 50건이 모두 5초 대기 제한을 소진하면 발송 대기만 이론상 약 250초이고 DB 시간이 추가된다. delivery 하나에 여러 단말이 있으면 더 길어질 수 있다. 기존 polling 5초·batch 상한·재시도 간격은 바꾸지 않았으며, obsolete worker-threads/queue-capacity 설정만 제거했다.

### 중복 방지의 실제 근거와 한계

- `SeatNotificationDelivery`와 migration V16의 `(outbox_id, user_id, channel)` unique 제약이 같은 delivery 생성을 막는다. V19에는 idempotency_key 고유 제약도 있다.
- `SeatNotificationDeliveryRepository.findReadyForUpdate`는 비관적 쓰기 락으로 due PENDING 또는 lease가 만료된 PROCESSING을 선택한다. `claim()`은 새 UUID claimToken과 lockedUntil을 기록한다.
- `SeatNotificationDeliverySettlementService`는 같은 claimToken인지 다시 검사해 stale worker 정산을 거부한다. SENT·retry/backoff·DLQ 전이와 관리자 replay의 기존 락을 유지했다.
- 재시도는 delivery에 저장된 같은 UUID idempotencyKey를 재사용한다. `NotificationDeliveryIdempotencyKey.providerKey`는 SHA-256 → URL-safe Base64 앞 22자로 변환한다. Discord nonce/enforce_nonce, WebPush topic/payload 및 service worker notification tag, FCM data, email X-Idempotency-Key 전달이 유지된다. 이 값 전달만으로 모든 제공자의 exactly-once를 보장하지 않는다.
- 실제 outbox 경로의 중복 방지는 위 DB/claim 장치다. `NotificationService.handleSeatOpenedEvent`와 `ALERT:<courseKey>` Redis 경로는 runtime 호출·이벤트 애너테이션이 없고 테스트만 호출하던 옛 경로여서 제거했다. 실제 outbox 중복 방지와 사용자 테스트의 `ALERT:USER_TEST:` 10초 쿨타임은 유지했다.
- 외부 발송 성공 후 SENT 정산 커밋 전 장애가 나면 재시도가 다시 발송할 수 있다. 이 기존 한계는 순차 실행만으로 사라지지 않는다. 이미 materialize된 delivery에 구독 취소를 새로 소급 적용하는 정책 변경도 하지 않았다.

## 외부 공유 중첩 타입 전수 분리

기준 코드의 중첩 선언 31개를 확인하여 소유 클래스 밖에서 사용하는 19개를 단독 최상위 파일로 분리했다. 필드 순서·생성자·factory·Lombok·validation·JSON 이름을 보존했고 JPA entity·영속 enum·migration은 바꾸지 않았다. 클래스명 기반 Jackson type ID 설정은 사용하지 않는다.

| 이전 타입 | 최상위 타입 및 위치 |
| --- | --- |
| ParsedCourseDto.ScheduleDto | academic/course/dto/ParsedCourseScheduleDto |
| CourseSummary.CourseScheduleSummary | academic/course/dto/CourseScheduleSummary |
| CourseSearchCriteria.SelectedSchedule | academic/course/dto/CourseSelectedSchedule |
| PrimaryTimetableQuery.PrimaryTimetableSnapshot | planning/timetable/dto/PrimaryTimetableSnapshot |
| PrimaryTimetableQuery.CourseSnapshot | planning/timetable/dto/TimetableCourseSnapshot |
| PrimaryTimetableQuery.CourseScheduleSnapshot | planning/timetable/dto/TimetableCourseScheduleSnapshot |
| PrimaryTimetableQuery.CustomScheduleSnapshot | planning/timetable/dto/TimetableCustomScheduleSnapshot |
| PrimaryTimetableQuery.CustomScheduleTimeSnapshot | planning/timetable/dto/TimetableCustomScheduleTimeSnapshot |
| CustomScheduleRequest.CustomScheduleTimeRequest | planning/timetable/dto/request/CustomScheduleTimeRequest |
| CustomScheduleResponse.CustomScheduleTimeResponse | planning/timetable/dto/response/CustomScheduleTimeResponse |
| TimetableCourseResponse.ScheduleResponse | planning/timetable/dto/response/TimetableCourseScheduleResponse |
| AdminFeedbackReadRepository.FeedbackSummary | support/feedback/dto/AdminFeedbackSummary |
| AdminFeedbackReadRepository.FeedbackDetail | support/feedback/dto/AdminFeedbackDetail |
| AdminFeedbackReadRepository.FeedbackReplySummary | support/feedback/dto/AdminFeedbackReplySummary |
| AdminFeedbackReadRepository.FeedbackAttachmentSummary | support/feedback/dto/AdminFeedbackAttachmentSummary |
| AdminFeedbackReadRepository.FeedbackAttachmentAccess | support/feedback/dto/AdminFeedbackAttachmentAccess |
| LocalFileUploadService.LoadedFile | common/dto/LoadedFile |
| ImageUploadSanitizer.SanitizedImage | common/util/SanitizedImage（기존 package-private 접근성 유지） |
| NotificationDevice.Type | identity/user/enums/NotificationDeviceType |

미사용 NotificationContext는 제거했고, 소유자 내부에서만 쓰는 private 타입 11개는 유지했다: Circuit, WebPushPayload, NotificationMessage, 세 JWT 클래스의 RefreshTokenRecord, 이미지 출력 제한의 BoundedOutputStream/ImageOutputLimitExceededException, parser TimeRange, 검색 ValidScheduleCondition, 이메일 SendCooldownLease. 테스트 전용 보조 타입도 무차별 분리하지 않았다. 독립 검토에서 외부 공유 누락, 옛 중첩 참조, 선언 본문·애너테이션·factory 변경은 발견되지 않았다. DashboardSnapshotResponseContractTest의 실제 Jackson JSON 검사와 CustomScheduleRequestValidationTest의 cascade validation 등 기존 계약 테스트로 확인했다.

## 테스트를 핵심 계약으로 통합

사용자의 추가 요청에 따라 기존의 테스트 전부 보존 원칙을 중복·구현 세부사항 검사의 선별 통합으로 변경했다. 실패를 숨기기 위한 삭제·skip·assert 완화는 하지 않았다. 삭제 전 순차화된 전체 suite 525건은 이미 통과한 상태였다.

| 정리 대상 | 이유와 남은 보장 |
| --- | --- |
| CourseCrawlCompletionServiceTest | mock 호출 순서·전달 2건을 실제 CourseCrawlCompletionTransactionTest의 성공 상태·롤백·저장 실패 검사로 대체 |
| SeatNotificationOutboxServiceTest | save(any) 호출 1건을 실제 DB outbox 필드·commit/rollback 검사로 대체 |
| NotificationServiceTransactionTest | 직접 outbox 호출에 무관한 notification mock 8개를 붙인 중복 2건 제거 |
| SeatNotificationOutboxTransactionTest | commit/rollback 2건은 수집 전체 DB 경로와 중복. 고유한 MANDATORY 거부 검증은 CourseCrawlCompletionTransactionTest로 이동 |
| NotificationService의 옛 이벤트 경로 4건 | 실제 호출 없는 Redis 발송 경로와 함께 제거. 현재 durable delivery의 4개 채널 provider key·이력, persisted outbox의 채널별 1건 생성·SENT/COMPLETED·빈 후속 claim을 검증하도록 기존 테스트 강화. 사용자 제공자 실패 전파·쿨타임 해제 1건 추가 |
| timetable/page.test.tsx | hook·화면을 모두 mock한 prop 전달 검사만 있어 제거. 실제 hook·grid 기능 검사는 유지 |
| nav-links skeleton 개수 | 장식 개수 4라는 내부 구현 고정만 제거. 로그인·관리자·SSR·접근성 검사는 유지 |
| header cookie/store 중복 | 같은 입력의 loading/login 기대를 하나의 렌더에서 함께 확인 |
| course guide·timetable grid | 같은 fixture의 초기 표시 assertion을 닫기/재노출 또는 실제 블록 표시 흐름으로 통합 |

서버는 4개 중복 테스트 클래스를 통합해 8건을 제거하고 고유 검증 1건을 옮겼다. 이어 미사용 알림 경로 4건을 제거하고 현재 사용자 실패 계약 1건을 추가해 **525→515건**이다. 현재 outbox 경로의 네 채널 검증은 기존 테스트에 통합했다. 웹은 mock 전달·장식 2건 삭제와 중복 3건 통합으로 **283→278건**, 테스트 파일은 **80→79개**다. 수 자체를 줄이기 위해 다른 기능의 보호를 제거하지 않았다. SubscriptionServiceQueryCountTest는 실제 H2/Hibernate SQL 3회로 N+1을 검증하므로 유지했다. MdcTaskDecoratorTest는 같은 worker 재사용 때의 context 누수를 별도로 검증하므로 유지했다. service worker VM 검사와 파일·첨부·권한·알림 취소·재시도·동시성 검증도 유지했다.

## 기존 원본 미커밋 변경의 선택 보존

추가 권한을 받은 뒤 원본 상태와 7개 파일을 다시 확인했다. 수집 파서·두 회귀 테스트의 기능 수정은 이미 포함돼 있었으므로 다시 중복 적용하지 않았다. 초기 관측 뒤 원본 parser 설명 주석 1줄이 제거된 추가 변경도 확인하고 반영했으며, 원본에는 읽기만 수행했다.

- AGENTS.md의 작업 흐름·조기 반환·검증/기록 지침은 현재 저장소 개발 기준이므로 보존했다.
- course-collection-policy의 2026-10-23 추가 결정과 course-collection-seasonal-diagnostics의 재현·검증 근거는 포함한 수집 수정의 근거이므로 보존했다. 문서의 10월 17일·23일 운영 기록은 이전 작업 이력이며 이번 작업에서 운영 접속·배포·발송을 한 기록이 아니다.
- backend-redesign-checklist의 10월 17일 운영 준비·조사 메모는 현재 코드 수정이 아니고 최신 진단 기록에 후속 결과가 있으므로 이번 단순화 브랜치에는 복사하지 않았다. 원본의 해당 변경은 그대로 남겨 두었다.
- 중단된 backend-rewrite 브랜치의 코드와 민감한 설정·자격 증명은 가져오지 않았다. 원본에 reset/clean/stash를 실행하지 않았다.

## 단순화 통합 검증 (`29dfb568f`)

- Java 21 `./gradlew --offline clean check bootJar`: **115개 클래스, 515건 통과, 실패/오류/skip 0**, PMD main/test 0, Checkstyle main/test 0, 실행 JAR 생성 성공. 삭제한 테스트의 옛 class 파일이 섞이지 않도록 clean부터 실행했다.
- Node 22 Vitest: **79개 파일, 278건 통과**. ESLint·TypeScript·production build를 통과했다. localhost API mock Playwright **2개 시나리오는 재시도 0으로 통과**했다.
- 순차 호출 스레드, 선점 순서, 한 건 실패 뒤 다른 채널 계속 처리, 중복 poll 거부, 종료/중단 뒤 추가 선점 금지, 실패한 poll의 다음 실행 복구, platform/virtual 모드의 scheduler 작업 비겹침과 종료를 검증했다. 기존 provider 5초·이미지 2초 대기 제한, 회로·429/backoff·DLQ·취소·claim fencing·DB 원자성 테스트도 통과했다.
- MySQL 실제 잠금/flush, 외부 제공자와 단말 수신, 서버 전체 HTTP E2E·운영 부하를 검증했다고 확대 해석하지 않는다. 실제 학교·발송 제공자 호출·운영 변경·원격 push·병합·배포는 하지 않았다.

최종 서버 로그는 simplification-clean-resumed.log, 웹은 web-test-resumed.log / web-lint-resumed.log / web-typecheck-resumed.log / web-build-resumed.log / web-e2e-resumed-build.log에 저장했다. 첫 clean 실행의 PMD 중복 문자열 1건은 공통 상수로 정리한 뒤 전체 clean 검증을 다시 통과했다.


### 재연결 후 알림 정리와 브라우저 검증

중단 시 마지막 legacy 수정은 저장되지 않았음을 diff로 먼저 확인했다. runtime 전체 검색에서 호출자와 이벤트 등록이 없는 `handleSeatOpenedEvent`, 3인자 delivery 전달 메서드, 구독자 bulk 조회·Redis course TTL 발송 경로를 삭제했다. 실제 outbox worker는 기존 4인자 delivery만 사용한다. 관리자/사용자 테스트 컨텍스트의 forceSend=true·saveHistory=false는 고정값이므로 삭제하고, 서로 달랐던 대상 없음의 엄격성만 boolean으로 전달한다. 관리자 NOT_FOUND, 사용자 대상 없는 채널 건너뛰기/모두 없으면 INVALID_INPUT, 제공자 실패 전파·쿨타임 해제는 그대로다.

기존 Playwright 최종 시도는 첨부 API가 mock을 우회하여 localhost:8080에 연결을 시도했고 재시도에서 통과했다. 이는 깨끗한 통과로 기록하지 않았다. [Playwright의 공식 안내](https://playwright.dev/docs/network#missing-network-events-and-service-workers)에 따라 API mock을 쓰는 브라우저 컨텍스트에 `serviceWorkers: "block"`을 설정해 route 가시성을 고정했다. 테스트 assertion·timeout·retry 수는 완화하지 않았고 실제 service worker의 VM 기반 테스트도 유지했다. 수정 뒤 `CI=1 npm run e2e -- --retries=0`으로 2개 모두 첫 실행에 통과했고, 다시 만든 production build에서도 같은 명령으로 재시도 없이 2개 통과했다. SW가 이전 실패의 유일 원인이었음을 확정하는 운영 진단은 아니다.

재연결 시 원본 dev의 HEAD·git status·미커밋 7개 파일 SHA-256이 연결 중단 직전 스냅샷과 모두 같음을 확인했다.


## 추가 로컬 커밋과 완료 상태

| 커밋 | 범위 |
| --- | --- |
| `f749ab512` | 원본 미커밋 작업 지침·계절학기 결정/진단의 선택 보존 |
| `40782249e` | 외부 공유 중첩 타입 19개 분리와 참조 갱신 |
| `65b26386c` | outbox 순차 실행·단일 scheduler·Slack 직접 실행 |
| `8765c13b3` | 미사용 알림 경로·고정 컨텍스트·단일 구현 계약 제거와 실제 경로 검증 |
| `f1a3c9bb8` | 중복 수집/outbox 테스트를 실제 원자성 검증으로 통합 |
| `0ff996edf` | 중복 웹 테스트 정리·브라우저 API mock 격리 |

최종 변경 집합의 서버 clean 검증, 웹 전체 테스트·lint·타입 검사·production build·재시도 없는 두 브라우저 시나리오가 통과했다. 원본 dev의 HEAD·상태·7개 미커밋 파일을 보존했다. 구현 판단 대기나 차단된 필수 작업은 없으며 운영·실제 제공자·MySQL 환경 검증은 허용 범위 밖의 미검증 사항으로 남긴다.


## 후속 수정: 만료 WebPush 단말의 실패 격리

추가 리뷰에서 한 사용자의 첫 WebPush 단말이 404/410으로 만료되면 정상 단말도 시도하지 않고 delivery 전체가 DLQ에 들어가는 기존 문제를 확인했다. 사용자는 이 항목만 수정하고 CIRCUIT_OPEN attempts 소모는 유지하기로 결정했다.

`NotificationService.deliverSeatOpening`의 기존 반복에만 조건부 예외 처리를 넣었다. typed NotificationProviderException이고 WEB 채널·WEB_PUSH_INVALID_SUBSCRIPTION·non-retryable일 때만 나머지 단말을 계속 처리한다. 실제 sender가 기존대로 만료 단말을 삭제한다. 하나 이상 성공하면 기존 이력 1건과 성공 정산, 모두 만료면 같은 오류를 재전파해 이력 없이 DLQ다. 다른 영구 오류·일시적 실패는 부분 성공 여부와 관계없이 기존대로 즉시 전파한다. 관리자·사용자 테스트 알림 API는 바꾸지 않았다.

새 WebPushDeliveryTest는 실제 NotificationService→processor/settlement와 H2를 가짜 제공자에 연결한다. 수정 전 6건 중 3건 실패로 문제를 재현했다. 양 순서의 만료/정상 단말, 전체 만료, 첫째/둘째의 일시 실패와 보통 재시도, 부분 성공 후 다른 영구 오류를 검사한다. 부분 성공 후 재시도는 같은 delivery key로 전체 대상 목록을 다시 보내므로 앞선 성공 단말에 중복이 생길 수 있다. 단말별 영속 정산이나 exactly-once 보장을 추가하지 않았다.

추가 승인에 따라 실제 WebPush HTTP 취소와 단일 scheduler 지연 검증을 이어서 수행한다. 회로 차단 attempts·HTTP timeout·scheduler 설정은 이번 변경 대상이 아니며 결과를 근거로만 기록한다.


### 후속 검증 결과: 실제 HTTP 취소와 scheduler 지연

- WebPushHttpCancellationTest는 실제 NotificationService의 sender가 사용하는 WebPushNotificationSender→PushService 5.1.1 암호화/HTTP→localhost 지연 서버와 실제 NotificationProviderResilience를 연결했다. 테스트 전용 pass-through subclass는 super.sendAsync가 반환한 하위 Future 참조만 관측한다. endpoint 검증만 localhost 실험용 mock이며 VAPID/단말 키는 테스트에서 생성한다.
- 운영 기본 5,000ms 결과 대기 제한에서 집중 실행 5,041ms, 최종 clean 실행 5,035ms 뒤 TIMEOUT이었다. 서버는 실제 암호문 POST 81바이트를 이미 수신했고, 그 시점 하위 HTTP Future는 isCancelled=false·isDone=false였다. 서버 201 응답을 풀자 같은 Future가 늦은 201로 완료됐다. 이 실험에서는 외부 timeout/cancel이 하위 HTTP 요청 취소로 전파되지 않는 것이 확인됐다. 이 테스트는 기존 제한의 진단이며 취소 전파를 고치면 기대값도 갱신해야 한다. TLS·실제 제공자 수신은 확인하지 않았다.
- SchedulerDelayTest는 실제 SchedulingConfig·CourseScheduler/CourseCrawlerService·SeatNotificationOutboxWorker·신선도 검사 메서드와 가짜 I/O를 사용했다. outbox/freshness 주기만 5ms/60ms로 1,000:1 축소하고 수집 fetch를 latch로 약 300ms 지연시켰다. 최종 clean 실측 hold 305ms, outbox 지연 305ms, freshness 지연 306ms, 최대 동시 작업 1개, 해제 후 각 2회 반복 실행을 확인했다. 집중 실행 예는 hold 356ms·지연 311/313ms였다. hold는 fetch 시작, 지연은 이후 등록 시점부터 측정하므로 시작점이 다르다.
- 운영 180초×4시도 + 재시도 대기 3+6+9초 = 738초는 한 분류의 한 upstream 단계가 매번 timeout되는 조건부 계산이다. 실제 client는 매 시도 bootstrap GET 후 course POST를 실행하고 분류도 여러 개이므로 전체 수집의 최악 상한이 아니다. 축소 실험 실측을 운영 부하나 738초 실행 결과로 환산하지 않는다.
- 사용자 선택에 따라 CIRCUIT_OPEN의 attempts 소모는 수정하지 않았다. WebPush 취소 전파나 scheduler/timeout 값도 바꾸지 않았다. 이번 후속 수정의 production diff는 만료 단말 격리 21줄뿐이다. 새 병렬 추상화·프레임워크·스키마를 도입하지 않았다.

### 후속 전체 검증과 산출물

Java 21 `./gradlew --offline clean check bootJar`: **118개 클래스, 523건 통과, 실패/오류/skip 0**, PMD main/test 0·Checkstyle main/test 0, 실행 JAR 생성 성공. 추가 8건은 만료/정상·실패·정산 6건과 실제 HTTP 취소 및 scheduler 지연 진단 각 1건이다. 초기 probe 컴파일의 Micrometer 자원 종료 API 사용 오류를 수정한 뒤 집중 검증과 clean 전체 검증을 통과했다. 기존 테스트를 삭제하거나 skip하지 않았다. 이번 후속에서는 웹 코드가 바뀌지 않았으며 위 29dfb568f의 웹 검증 결과와 구분한다.

로그는 작업 디렉터리의 webpush-expired-before.log / webpush-expired-focused.log / notification-probes-focused.log / notification-followup-clean.log에 있다. JUnit XML의 system-out에 두 probe의 실측을 남겼다. 전체 구조 산출물은 [독립 HTML](current-architecture.html)로 제공하며 실제 소스·기본값·제약·검증 근거 링크를 포함한다. 브라우저 시각 검증과 최종 커밋은 아래 완료 기록에 추가한다.


### HTML 완료 기록과 최종 로컬 변경 단위

- `3ff6e6b95`: 만료 WebPush 단말 격리와 실제 발송/이력/정산 회귀 6건.
- `16234ac48`: 실제 WebPush HTTP 취소·scheduler 지연 검증과 실측 기록.
- `docs/current-architecture.html`은 코드 기준 `16234ac48`의 독립 파일이다. 전체 구조·수집/알림·사용자 요청·경계/검증 4개 탭, 트랜잭션·중복 방지·실패·재시도·시간 제한과 남은 한계를 실제 코드/테스트 링크로 연결한다. HTML 추가 뒤 production 코드는 바뀌지 않았다.
- Chromium에서 file://로 열어 1440·390·320px 각각 4개 탭을 확인했다. 모든 화면에서 document.scrollWidth가 viewport와 같았고 Home/End/ArrowRight 탭 이동·접기/펼치기·실제 Java 파일 링크 이동을 통과했다. 소스 링크 73개 모두 실존, 외부 HTTP 요청·JS 오류·깨진 링크 0이었다. 데스크톱·모바일 스크린샷을 시각적으로 확인해 글자·카드 잘림이 없음을 확인했다.
- 증거는 작업 디렉터리의 architecture-html-verification.json과 architecture-{overview,delivery,requests,boundaries}-{desktop,mobile}.png 8개다. HTML SHA-256은 0a09a340f7f8510ed18a1a59c47e3efe8da6bc2374d9e2d9a63c3abafbc5c77c다.
- 원본 dev에는 기존 unstaged 6개와 untracked 진단 문서 1개가 그대로 남아 있다. 필요한 수집 코드·테스트·지침·정책/진단은 단순화 브랜치에 이미 커밋돼 있으며, 원본의 미커밋 표시는 원본을 보존했기 때문이다. 과거 backend-redesign-checklist 메모는 의도적으로 복사하지 않았다. HEAD·상태·7개 파일 SHA-256 보존도 재확인했다.
- 원격 push·PR·병합·공개 게시·배포·실제 제공자/단말 발송·운영 DB/권한 변경은 수행하지 않았다. CIRCUIT_OPEN attempts 정책과 HTTP/scheduler 설정의 후속 변경은 승인된 범위가 아니므로 남은 위험을 기록하는 데서 마쳤다.


## 구조·흐름·시퀀스 다이어그램 보강

사용자의 추가 요청에 따라 설명 카드에 더해 실제 박스·화살표 SVG 4개를 추가했다. 전체 컴포넌트도, 수집→outbox→순차 발송/정산 흐름도, 수집·변경 감지·저장 시퀀스, 선점·단말 발송·정산 시퀀스다. 시퀀스 전용 탭을 추가했으며 요청/반환 화살표, lifeline, loop/alt, 트랜잭션 테두리, DB unique와 claim token을 소스와 대조했다. 이미 materialize된 delivery의 구독 취소 소급 없음, 발송 생략 후 SENT 정산, 만료 단말 격리, 단말 loop 종료 뒤 이력 1건 저장, 부분 성공 재시도·stale 정산 거부·늦은 HTTP 응답의 한계를 구분했다.

이미지 생성 모델이나 CDN을 사용하지 않았다. 고정 좌표 SVG를 브라우저 canvas에서 2배 해상도로 렌더해 docs/diagrams/current-components.png(2880×2140), current-notification-flow.png(2880×2360), current-crawl-sequence.png(2880×3340), current-delivery-sequence.png(2880×5480)를 내보냈다. UI 스크린샷이 아니라 전체 SVG만 PNG로 변환했다.

1440·390·320px의 5개 탭에서 페이지 가로 잘림 0, 네 도식의 모바일 키보드 가로 스크롤을 확인했다. SVG 경계 밖 텍스트·잘못된 marker·중복 ID·깨진 링크·외부 요청·JS 오류 0, 소스 링크 77개 실존이었다. 실제 PNG를 시각 검토하여 떠 있는 연결선, 선/박스에 가린 라벨, loop 안으로 읽히던 이력 저장 표현을 수정한 후 다시 export·검증했다. 증거는 작업 디렉터리의 architecture-diagrams-verification.json과 architecture-diagrams-*.png preview에 있다. 최종 HTML SHA-256은 75e8f550fa02ffe7f41c99b215fa891ab8fe3861bd09184de206dae09bc9d0f0이다.

Library HTML은 같은 identity의 버전 1로 교체했고 PNG 네 개는 각각 새 파일 버전 0으로 저장했다. 공개 공유 권한은 만들지 않았다. 일괄 준비 기능은 런타임 도구 발견 단계에서 제공되지 않아 세션 생성/파일 전송이 시작되지 않았음을 확인한 뒤, 지원되는 개별 replace/create 경로로 순서대로 저장했다. HTML은 기존 버전 0 조건을 유지해 교체했고 반환된 identity/version 메타데이터를 각 원본 파일에 보존했다.

도식 변경 커밋은 6f6aafa60이며 문서·이미지뿐이다. 앱 코드·설정·정책은 바꾸지 않아 서버 테스트를 반복 실행하지 않았다(직전 코드 검증 523건 통과). 최종 상태 확인 중 원본 dev는 HEAD aade57db가 같지만 이전 미커밋 7개가 사라져 clean으로 바뀐 것을 관측했다. 이 다이어그램 작업에서는 원본 파일 수정·reset·clean·stash를 실행하지 않았으며, 그 상태 변화 경위는 확인하지 못했다. 필요한 기존 수집 코드·테스트·지침·정책/진단은 단순화 브랜치에 이미 포함돼 있다. 현재 원본 상태를 임의로 복원하거나 바꾸지 않았다.


## 후속 선택 구현: 리뷰 캐시 보존과 미사용 기기 훅 정리

사용자는 추가 리뷰의 1번 캐시 오염 수정·3번 미사용 기기 훅 제거만 선택했다. 2번 서버 리뷰 범위 통합은 설명만 요청해 서버 구현·저장소·정책·잠금·트랜잭션을 변경하지 않았다.

useReviews의 목록·상세 캐시에 전체 updatedCourse를 합치던 두 곳을 파일 내부 withReviewStats로 바꿨다. averageRating·reviewCount·isReviewed 세 필드만 공유한다. 리뷰 생성·수정 API, 공유 범위와 통계 계산은 그대로다. 다른 개설 강의의 courseKey·연도·학기·분반·좌석·시간과 다른 리뷰 범위의 전체 객체를 보존하는 검증을 기존 성공 테스트 두 개에 추가했다. 수정 전 두 테스트가 모두 실패(나머지 9개 통과)했고 수정 후 11개가 통과했다.

저장소 전체 참조를 재확인한 useRegisterDevice·useUnregisterDevice는 자체 테스트 외 호출자가 없어 구현 34줄·자체 mock 테스트 3개(81줄)를 제거했다. 실제 useWebPush·기기 API·온보딩 구현은 바꾸지 않았다. 웹 단위 테스트는 79파일 278건에서 78파일 275건으로 줄었다. 리뷰 회귀는 기존 두 테스트에 보강해 테스트 수를 늘리지 않았다.

브라우저에는 리뷰 생성·수정 두 시나리오를 추가했다. 같은 과목·교수의 두 분반 상세 캐시를 먼저 읽고, 한 분반의 별점을 변경한 뒤 다른 분반의 시간·좌석·통계와 관심 강의 요청의 courseKey를 실제 화면에서 확인한다. 모든 API 요청은 가짜 응답으로 처리하고 외부 리소스는 차단해 운영 백엔드·제공자에 요청하지 않는다. 상세 캐시 객체의 전체 필드 보존은 단위 테스트에서 직접 확인한다.

### 서버 리뷰 범위 통합 설명 — 미적용 제안

- CourseReviewService는 별점 목록·중복 작성 확인·통계, CourseEmojiReviewService는 이모지 토글·집계·본인 여부, CourseReviewQueryService는 강의 응답의 isReviewed를 계산할 때 같은 단일/여러 키 분기를 각각 수행한다.
- 실제 키 형식은 연도:학기:과목코드:분반이다. 저장소 테스트의 2026:U211600010:0000131925:1과 2026:U211600020:0000131925:1은 다른 개설 강의다. 과목코드·정규화한 교수가 같으면 두 키의 리뷰를 공유한다. 다른 교수는 섞지 않는다.
- null·공백·교수 미지정은 식별 불가이므로 각 현재 courseKey만 쓴다. 외부교원1은 식별 가능한 이름이므로 같은 과목·외부교원1끼리 공유하고 외부교원2와 구분한다. ReviewScopeKey의 정규화와 CourseRepository의 과목코드·trim 교수명 조건이 실제 기준이다.
- 제안은 기존 CourseReviewCatalogService가 범위 키 목록을 반환하고, 미지정도 현재 키 하나의 목록을 반환하도록 해 호출자의 기존 IN 조회로 통일하는 것이다. 새 서비스·프레임워크·테이블은 필요 없다. 예상 범위는 카탈로그·리뷰 세 서비스, 리뷰 저장소 두 곳과 관련 테스트다.
- 미지정 강의의 학기별 분리·다른 교수 구분·본인 리뷰 우선 정렬·중복 작성 방지·통계·기존 행 잠금과 트랜잭션 보존 검증이 필요하다. 정책 변경이 아니라 중복 분기 감소 후보이며 이번에는 적용하지 않았다.

### 최종 웹 검증과 한계

- 집중 리뷰 훅 11건, 웹 전체 78파일 275건 모두 통과. 삭제·skip·assert 약화로 실패를 숨기지 않았다.
- TypeScript --noEmit·전체 ESLint·production build 통과. 최초 제한된 네트워크 빌드는 기존 Google Fonts 두 폰트 다운로드에 실패했고, 네트워크 접근을 허용한 동일 build가 성공했다. 폰트·빌드 설정은 변경하지 않았다.
- Playwright는 새 리뷰 생성/수정과 기존 인증 갱신·관리자 첨부 미리보기 총 4건이 --retries=0으로 통과(16.1초)했다. 최초 sandbox 실행은 로컬 3100 포트 bind EPERM으로 시작하지 못했다. 허용된 실행에서 첫 두 신규 시나리오는 숨김 모바일 좌석 요소를 먼저 잡는 선택자 때문에 실패해, 같은 좌석 값을 가진 보이는 요소를 확인하도록 고친 뒤 4건 모두 통과했다. 실패 로그·trace·스크린샷을 작업 디렉터리에 보존했다.
- 코드 독립 리뷰에서 추가 문제를 찾지 못했다. 서버·실제 WebPush·원본 dev에 변경이 없으며 서버 테스트는 반복하지 않았다. 실제 브라우저 push 구독·외부 제공자·운영 DB 검증은 이번 웹 캐시 수정의 검증 범위 밖이다.
- 로그는 작업 디렉터리의 review-cache-before.log, review-cache-focused.log, review-cache-full-test.log, review-cache-typecheck.log, review-cache-lint.log, review-cache-build.log, review-cache-e2e*.log와 review-cache-e2e-first-results/에 있다. 원격 push·배포·실제 발송은 수행하지 않았다.

추가로 저장소 전체 Markdown 링크 검사는 기존 옛 경로 등을 가리키는 126개 오류로 실패했다. 수정 전 HEAD를 별도 임시 디렉터리에 archive한 뒤 같은 검사로 비교해 오류 목록이 완전히 같음을 확인했다. 이번 변경이 추가한 링크 오류는 없으며 과거 문서 전체의 링크 정리는 이번 승인 범위 밖으로 남겼다. 비교 로그는 review-cache-markdown-current.log·review-cache-markdown-baseline.log다.


## 서버 리뷰 공유 범위 통합 완료

사용자가 후속 후보 2번을 승인해 CourseReviewCatalogService.findReviewCourseKeys로 대상 강의 선택을 모았다. 미지정 교수는 현재 키 하나, 식별 가능한 교수는 기존 과목코드·trim 교수명 조회 결과를 그대로 반환한다. 별점·이모지·본인 작성 여부는 기존 IN 쿼리를 쓰고, 평점·개수 갱신도 같은 대상 목록을 재사용한다. 불필요해진 단일 키/중복 저장소 메서드 12개와 전달·분기 코드가 줄어 production Java는 순감 145줄이다. 새 계층·프레임워크·DB/API 변경은 없다.

기존 Mockito 테스트의 결과·오류 검증은 유지하고 삭제된 단일 키 쿼리 stubbing/호출 검증만 목록 기반으로 옮겼다. 새 ReviewScopeContractTest는 격리 H2에서 실제 카탈로그·별점·이모지·작성 여부 서비스를 연결한다. null·빈값·공백·교수 미지정 변형 4개와 일반 교수·외부교원1의 공유 2개 시나리오는 변경 전에도 통과했다. 변경 후 같은 6개와 빈 교수 범위의 임의 fallback 금지 1개를 검증했다. 다른 학기·분반 공유, 외부교원2/다른 과목 배제, 중복 작성 거부, 내 리뷰 우선 정렬, 수정·삭제·생성의 평균/개수, 이모지 공유 취소·다시 추가·본인 여부를 확인한다.

작성·수정·삭제의 기존 입력 강의 PESSIMISTIC_WRITE 조회, saveAndFlush/flush 순서, 서비스 트랜잭션 애노테이션은 그대로다. 이는 입력 개설 강의 1행의 잠금이며 그룹 전체 동시 작성 방지를 새로 보장한 것은 아니다. 독립 읽기 전용 리뷰에서도 새 오류를 찾지 못했다.

검증: 집중 37건 통과, ./gradlew --offline clean check bootJar 성공(1분 17초). 서버 119 suite·530 tests, 실패·오류·skip 0, PMD main/test·Checkstyle main/test 위반 0, 실행 JAR 생성 완료. 최초 제한된 실행은 기존 Gradle 캐시 lock 쓰기 권한에서 시작하지 못했고 허용된 로컬 실행으로 검증했다. 변경 전/집중/전체 로그는 작업 디렉터리의 review-scope-before.log·review-scope-focused.log·review-scope-clean.log에 있다. 실제 MySQL 동시성·운영 DB·외부 제공자는 사용하지 않았다. 웹 구현은 바뀌지 않아 앞서 통과한 웹 검증을 반복하지 않았다.

로깅 전반의 메시지 한국어화·레벨·위치·중복/민감정보 점검은 사용자가 후속 할 일로만 요청했으므로 구현하지 않는다. CrawlerRunFailure/CrawlerStatus와 관리자 감사 엔티티 제거 여부도 현재는 읽기 전용 검토이며 삭제·로그 전환·스키마 변경을 하지 않는다. 일정/D-day 통합은 새 승인 범위로 별도 작업 단위에서 진행한다.


## DTO 역할 분류 완료

최신 사용자 기준에 따라 DTO 122개를 request 27개·response 53개·result 28개와 command 6개·criteria 4개·external 2개·provider 1개·diagnostic 1개로 분류했다. 직접 API에 노출되는 Dashboard Result·공유 Snapshot·감사 메타데이터는 response, 내부 User/조회 결과는 result다. 요청·서비스 입력·외부 수집 결과를 억지로 동일 역할에 넣지 않았다. 클래스명·필드·Jackson/validation 애노테이션은 유지했고 DTO 복제·변환 계층을 추가하지 않았다. 전체 목록과 판단은 dto-classification.md에 기록했다.

package/import와 생성자 쿼리, 같은 패키지의 테스트 경로를 함께 수정하고 현재 소스를 가리키는 문서 링크를 갱신했다. 서버·테스트 컴파일, PMD·Checkstyle main/test 모두 통과했다. 생성자 쿼리·감사 JSON·대시보드 관련 기존 20건도 실패/오류/skip 없이 통과했다. 로그는 작업 디렉터리의 dto-classification-check.log·dto-classification-tests.log다. 실제 코드의 정상 응답 래퍼·일정 통합·페이지 크기·주석·인터페이스 동작 변경은 후속 단위에서 진행한다.


## 정상 응답 직접 반환 완료

CommonResponse와 프론트 공통 성공 타입을 제거하고 26개 컨트롤러를 실제 객체·배열·Page/Slice·숫자·문자열·무본문으로 전환했다. 오류 공통 응답과 인터셉터·상태·쿠키/Location·파일 헤더·페이지 객체는 유지했다. 문자열은 JSON 문자열을 보장하고 nullable 대표 시간표는 기존 200/빈 본문을 클라이언트에서 null로 정규화한다. 성공 토스트는 기존 기능별 문구 또는 실제 수집 결과 문자열을 사용한다. API 소비 훅·상태 저장소·화면·SSR 메타데이터·사이트맵과 모의 응답을 함께 바꿨다. 업무 metadata.data와 오류/요청 data는 유지했다. 상세 계약은 api-response-contract.md에 있다.

서버 MockMvc 계약 6건과 웹 nullable 2건·SEO/사이트맵 2건을 보강했다. 초기 실패는 옛 성공 포장/문구 기대값과 Page 대신 이전 메서드를 mock하던 테스트, 쿠키 헤더 문자열 순서, JSON 경로 상수 중복이었다. 결과/오류 검증을 새 계약으로 옮겼고 쿠키는 실제 maxAge·값을 확인했다. 사이트맵 테스트 추가 중 기존 두 테스트를 덮어쓴 것을 확인해 모두 복원하고 새 검증만 더했다. 테스트를 삭제·skip하거나 핵심 assertion을 약화시켜 통과시키지 않았다.

최종 검증: 서버 120 suite·536 tests 실패/오류/skip 0, PMD·Checkstyle main/test 위반 0, check bootJar 성공. 웹 79파일·279건, 타입 검사·ESLint(경고 0)·production build 성공. Playwright 기존 인증 갱신·관리자 첨부·리뷰 생성/수정 네 건이 retries=0으로 모두 통과(19.5초)했다. 실제 백엔드·제공자 대신 가짜 응답을 사용했다. 빌드의 API_URL도 연결되지 않는 로컬 주소로 고정했다. 로그는 작업 디렉터리 direct-response-*에 있다. 운영 프론트·서버는 같은 응답 계약으로 함께 전환해야 하며 아직 배포하지 않았다.


## 일정 통합·페이지 정책·전달 인터페이스 정리 완료

일정과 D-day를 Schedule 하나로 통합하고 기존 일반 목록은 유지했다. 대표 선택은 여러 일정에 가능하며 진행 중 우선/종료 임박·예정 시작 임박·ID 보조 순으로 반환한다. 서울 시각의 시작 포함/종료 제외, 시각 없는 시작 0시·종료 다음날 0시, 생략한 선택값 보존·명시 해제, soft delete·복원 후 선택 유지, 관리자 권한을 검증했다. 옛 Dday 도메인·별도 API·웹 훅/패널을 제거하고 중복 320줄 관리자 페이지를 같은 패널로 통일했다. 옛 테스트의 게스트401/갱신 생략과 D-day 정렬 보장은 새 일정 계약 테스트로 옮겼다. V29는 기존 일정을 보존하고 선택 false/삭제시각 null을 추가한다. 옛 dday_settings는 매핑 없이 남겨 자동 DROP/DELETE를 하지 않는다. 사용자의 옛 데이터 폐기 허용에 따라 이관·ID 매핑을 만들지 않았으며 운영 DB에 접속하지 않았다.

모든 페이지 API는 요청한 page와 기존 sort를 유지하며 항상 30건을 사용한다. 원래 Page/Slice 응답과 offset 상한을 보존하고 실제 고정 크기로 offset을 계산한다. size=1/100/음수/0/잘못된 문자열과 다음 페이지를 요청하는 계약 테스트를 보강했다. 웹의 size 인수와 cache key 요소를 없앴으며 Kakao 외부 검색·outbox batch·대시보드 내부 제한 조회는 변경하지 않았다. 기존 API mock 호출 중 중복 2건은 page/sort/filter/metadata를 함께 확인하는 새 계약 테스트로 통합했다.

단일 구현 전달 인터페이스 21개를 제거하고 구체 서비스 19개를 직접 사용한다. 서비스 메서드·업무 본문·거래·검증은 유지했다. NotificationSender 네 구현과 외부 HTTP 경계 두 인터페이스, Spring Data 및 프레임워크 계약은 보존했다. 목록은 internal-interface-simplification.md에 있다.

서버 집중 49건이 통과했고 전체 검사 첫 시도는 기존 공지 테스트의 size2 기대값과 미사용 import1건으로 실패했다. 기존 정렬/고정 공지 assertion을 유지하며 기대값을 새 정책30으로 바꾸고 import를 정리한 뒤 전체 123 suite·572건이 실패/오류/skip 없이 통과했다. PMD·Checkstyle main/test 위반0, check bootJar 성공. 웹 전체79파일·295건, 타입·lint(경고0)·production build, Playwright5건이 retries=0으로 통과(7.9초). 새 브라우저 검증은 가짜 API의 일정 생성·선택·수정·삭제·복원이며 운영 API/제공자를 호출하지 않는다. 로그는 TASK/schedule-page-*에 있다.

읽기 전용 DB 기록·설정 점검과 보류된 계획은 simplification-review.md에 따로 기록했다. CrawlerRunFailure/Status/AdminActionLog·PasswordEncoder를 삭제하지 않았고, 로깅 정책·새 알림함·공지 채널 전달도 구현하지 않았다.


## 주석 정리와 최종 MySQL 검증

기능 변경과 별도로 소스273개 파일에서 설명/Javadoc/죽은 코드 주석1,423개를 제거했다. 과목 필드·외부 연동 매핑36개, 린터 지시8개, 빈 블록 lint 의미6개, shebang20개와 heredoc 내부1개를 보존했다. 문자열·URL·정규식·템플릿과 설정값은 언어별 토큰/AST/파싱 동등성으로 검증했다. 변경 계획 SHA273개 일치·재스캔 changed0/skip0·경계 자체검사16개·diff 검사 통과. 예외의 실제 위치와 이유는 comment-cleanup.md에 있다. 문서·생성물·vendor·lockfile·기존 migration은 제외했다.

최종 clean check migrationTest bootJar의 일반572건·정적검사는 통과했으나 첫 MySQL 업그레이드 검증은 정상 래퍼 제거 때 실수로 바뀐 감사 JSON 검사 경로3곳에서 실패했다. 감사 업무 metadata.data는 유지돼야 하므로 원래 SQL 경로를 복원했고 기존 기대값(PENDING/COMPLETED/already_safe)을 약화하지 않았다. 과거 migration SQL은 바꾸지 않았다. 실제 MySQL fixture에 기존 일정·D-day 행을 추가해 V29 후 원본·기본선택false·deletedAt null·255자 확장도 확인했다. 재실행 check migrationTest bootJar가58초에 성공했고 일반572건+MySQL2건 실패/오류/skip0, PMD·Checkstyle main/test0이다. 테스트 보정은 bc785e618, 주석 정리는 0bb2bf766 별도 커밋이다.

주석 정리 후 웹79파일295건·타입·lint 경고0·production build와 Playwright5건(retries0,8.4초)이 모두 통과했다. Compose runtime/local 렌더링·CD 정적 계약도 통과했다. 처음 두 shell 검사는 환경에 python 명령이 없어 시작하지 못해 TASK의 python3 임시 별칭으로 재실행했으며 소스는 바꾸지 않았다. 로그는 final-clean-server.log(실패 포함), final-mysql-verified.log(최종 성공), final-web-*.log, final-infra-*.log에 보존했다.

일정 선택은 기존 행 기본false이므로 배포 후 관리자가 대표 일정을 지정해야 한다. 옛 ddays API는 제거되고 정상 응답 포장도 바뀌므로 프론트·서버를 같은 계약으로 전환해야 한다. V29는 추가/확장만 수행하지만 자동 다운 마이그레이션을 제공하지 않으므로 적용 전 확인·백업이 필요하다. 운영 DB/외부 제공자/실제 브라우저 push 구독은 사용하지 않았다. 기존 HTTP 요청 잔존·단일 scheduler 지연·부분 성공 중복 가능성과 사용자가 유지한 회로 차단 attempts 정책은 그대로다.

HTML은 코드 기준0bb2bf766으로 업데이트하고 현재 DTO/응답·페이지30·일정/API/삭제복원 및 실제 소스링크를 맞췄다. 기존 알림 흐름/두 시퀀스는 동작이 바뀌지 않아 원본을 보존했다. 추가 삭제·설정 정리는 simplification-review.md의 읽기 전용 후보로 남겼다.


최종 시각 확인은 1440/390/320px의 5탭 모두 overflow0, 소스링크88개 정상, JS 오류·외부 요청0, SVG 글자 경계·marker 오류0, 키보드 탭·모바일 pan 정상이다. HTML과 current-components.png만 갱신됐고 기존 알림 흐름·두 시퀀스 PNG는 동일하다. Library 기존 파일 교체는 자동 승인 검토에서 payload/Library destination에 대한 명시적 사용자 승인 확인 부족으로 실행 전에 거부됐다. 전송은 시작하지 않았고 다른 경로로 우회하지 않았다. 로컬 산출물은 완성돼 있으며 Library 저장은 승인 대기다.
