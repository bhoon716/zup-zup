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

**확정 기준:** 사용자 요청 `dev 기준으로 잡아`에 따라 로컬 dev `aade57db`를 기준으로 구현했다. MVC·저장소 통합과 RestClient 전환을 유지한다. 계절학기 수정은 **dev HEAD에는 없지만 원본 dev 작업 폴더의 미커밋 수정에는 이미 있었다**. main `5f73a8a4`/PR #34의 수정과 대조해 격리 브랜치에 보존했으며, 파서 및 두 회귀 테스트가 원본 미커밋 파일과 바이트 단위로 동일함을 확인했다. 원본 파일은 수정하지 않았고 중복 적용도 하지 않았다.

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

## 진행 결과

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

최종 `./gradlew --offline check bootJar`: **118개 클래스, 515개 테스트 통과, 실패/오류/skip 0**. PMD main/test **0건**, Checkstyle main/test **0건**, 실행 JAR 패키징 통과. 초기 집중 검증에서 발생했던 구독 조회 오류와 새 테스트의 잔여 데이터 격리 문제를 수정한 뒤 전체 검증이 통과했다. 실패를 숨기거나 테스트를 비활성화하지 않았다.

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
