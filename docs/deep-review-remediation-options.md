# 심층 리뷰 결과와 해결 방안 비교

검토 기준은 dev `aade57db9`부터 `878f16f9a`까지다. 원본 보고서 ID는 `deep-review-20261026-redis-session-full-878f16f9a`이며 로컬 `.agents/reviews/deep-code-review/`에 보존했다. 독립 reviewer 6명과 verifier 4명이 확인한 medium 문제 4건이다. 서로 다른 문제를 한 원인으로 합치지 않았다.

현재 구현 승인은 **CT-1 Blob CSRF 오류 복구와 CT-2 기본 시간 입력**이다. R-1은 설명·비교만 하고, R-2는 관리자 단독 운영을 고려해 현상 유지하기로 했다. 단일 Spring을 유지하며 새 프로세스·메시지 브로커를 도입하지 않는다. 운영 설정·DB·Redis·외부 OAuth에는 적용하지 않는다.

## 근거 수준과 현재 상태

| 항목 | 확인한 근거 | 실행하지 못한 검증 | 상태 |
| --- | --- | --- | --- |
| CT-1 Blob CSRF 복구 | 원본 client/API·실제 Axios·Blob의 메모리 실행에서 옛 토큰 반복 실패를 재현 | 원래 리뷰의 Chromium 기동은 sandbox에서 실패. 후속 수정은 실제 Chromium 두 탭 회귀 통과 | 수정·검증 완료 |
| CT-2 일정 시간 입력 | 생산 picker/handler를 메모리에서 실행하여 20:00·23:55·09:01 도달 불가 확인 | 기본 입력 전환 후 실제 Chromium UI/API payload 검증 통과. 실제 서버 저장은 미실행 | 수정·검증 완료 |
| R-1 세션 쿠키 응답 경합 | Spring Session 3.5.4의 실제 resolver/filter/Redis 저장·mapper와 앱 호출을 대조 | 실제 HTTP·Redis·OAuth 경합 재현은 미완료 | 미수정 |
| R-2 일정 수정·삭제 경합 | 실제 엔티티와 Hibernate 6.6.39.Final·Spring Data 3.5.7·MySQL 격리 의미를 대조 | 두 독립 DB 트랜잭션 실행 재현은 미완료 | 사용자 선택으로 보류 |

R-1·R-2는 도달 경로와 기존 guard를 독립적으로 반박한 코드·의존성 근거(B)다. 실행 재현(A)이라고 부르지 않는다. 500의 빈도·지속 시간, 실제 관리자 동시 요청 빈도, 운영 DB의 isolation override는 측정하지 않았으므로 추정 수치를 붙이지 않는다.

## CT-1 — Blob CSRF 오류 복구

위치는 [공통 client](../apps/web/src/shared/api/client.ts)와 관리자 첨부 열람 화면 `apps/web/src/app/(admin)/admin/feedbacks/page.tsx`이다. 다른 탭에서 재로그인하면 현재 탭의 CSRF 캐시는 오래된 값이 될 수 있다. 다운로드의 403 JSON은 Blob이므로 객체의 code만 읽는 기존 분기는 A003을 놓친다.

| 방안 | 장점 | 단점·복잡도 | 계약 영향 |
| --- | --- | --- | --- |
| **A. client에서 제한된 JSON Blob 오류를 해석하고 A003만 캐시 폐기** | 캐시 소유 위치에서 복구하고 실제 다운로드 호출부는 유지. 다음 사용자 클릭만 새 토큰을 조회 | 낮음. 비동기 Blob 읽기 전후 세대 확인과 잘못된 본문 처리가 필요 | API·인증·권한·자동 재전송 정책 유지 |
| B. 첨부 API의 오류 처리에서만 Blob을 해석하고 client의 캐시 무효화 함수를 호출 | 공통 오류 처리의 변경 범위가 좁음 | 낮음~중간. 기능 모듈이 client 내부 캐시 제어를 알아야 하고 다른 Blob 호출이 생기면 중복 | 공개 API는 유지하지만 내부 의존성이 늘어남 |

**추천·승인된 구현은 A다.** 403·application/json·64KiB 이하 Blob만 파싱하고 정확한 A003일 때만 CSRF 캐시를 비운다. 일반 A002 권한 거부, 잘못된 JSON, 배열, 빈 본문, 비 JSON·과대 Blob, 500은 CSRF 실패로 바꾸지 않는다. 파싱하는 동안 세션 세대가 바뀌면 새 캐시에 영향을 주지 않는다.

사용자에게는 “로그인 상태가 변경되었습니다. 다시 시도해 주세요.”를 표시하고 실패 요청을 자동으로 다시 보내지 않는다. 다음 클릭에서 새 토큰을 준비한다. 일반 권한 거부는 기존 열람 실패 안내와 거부 상태를 유지한다.

검증 대상은 다른 탭 logout/relogin → 기존 탭 다운로드 403 → 자동 재전송 없음 → 다음 클릭의 새 토큰·성공, A002의 반복 거부, 잘못된 Blob 안전 처리, 늦은 오류 파싱의 새 세대 캐시 보호다.

## CT-2 — 통합 일정 화면의 시간 범위

위치는 통합 진입점 `apps/web/src/app/(admin)/admin/schedules/page.tsx`과 [일정 패널](../apps/web/src/features/admin/components/admin-schedule-panel.tsx)이다. 기존 제한 없는 type=time 화면이 범위가 좁은 picker로 교체됐다. 기존 야간 값은 그대로 보존되지만 새 20:00·23:55나 09:01을 선택할 수 없다. 분부터 고르면 00시 값에 도달하는 경로도 있으므로 “08~18시 외에는 전부 불가”라고 확대하지 않는다.

| 방안 | 장점 | 단점·복잡도 | 계약 영향 |
| --- | --- | --- | --- |
| **A. type=time, step=60인 기본 입력 사용** | 사용자 정의 picker 코드를 줄이며 24시간·분 단위를 지원. 기존 화면의 계약 복원 | 낮음. 표시와 조작 방식이 브라우저·OS마다 다름 | HH:mm API·시각 생략·서울 시간·기간 검증 유지 |
| B. 기존 picker를 24시·60분 선택 목록으로 확장 | 기존 시각 디자인과 조작 유지 | 낮음. 분 목록이 길고 사용자 정의 선택기 코드는 남음 | 동일한 HH:mm 계약 유지 |

**사용자가 A를 선택해 기본 시간 입력으로 전환했다.** 직접 Input 두 개로 구성하고 사용자 정의 TimePicker와 전용 Select 항목을 제거했다. 20:00→23:55 생성·시간 변경, 09:01 입력, 기존 20:01→23:57 일정의 날짜만 수정할 때 시각 보존과 선택 시각 초기화를 검증한다. 서버 변경은 없다.

## R-1 — 늦은 응답의 이전 세션 쿠키

위치는 [세션 인증 필터](../apps/server/src/main/java/bhoon/sugang_helper/common/security/session/SessionAuthenticationFilter.java)의 매 인증 응답 쿠키 갱신과 [로그인 ID 회전](../apps/server/src/main/java/bhoon/sugang_helper/identity/auth/service/AuthService.java)이다. 옛 S를 읽은 요청이 지연되는 동안 재로그인 N이 완료되고, 뒤늦은 S 응답이 같은 쿠키 이름을 덮을 수 있다. 지연 Redis delta가 불완전한 S hash를 다시 만들면 mapper 예외로 500도 가능하다는 코드 근거가 있다.

| 방안 | 장점 | 단점·복잡도 | 계약 영향 |
| --- | --- | --- | --- |
| **A. 일반 요청의 쿠키 재발급 제거, 브라우저 쿠키는 로그인 시 30일 고정 만료** | 변경이 가장 작음. 일반 지연 응답이 로그인 쿠키를 덮는 원인 제거 | 낮음. 계속 사용해도 브라우저 쿠키는 로그인 30일 뒤 만료 | **정책 변경.** Redis idle TTL은 연장되지만 브라우저 로그인 활동 연장은 포기. 기기별 logout·탈퇴·로그인 ID 회전 유지 가능 |
| B. 브라우저 세션 쿠키와 Redis 30일 idle 사용 | rolling 쿠키 처리가 없어 단순함 | 낮음. 브라우저 종료·복원 동작에 따라 다시 로그인 | **정책 변경.** 30일 persistent 로그인 보장을 포기 |
| C. 회전 세대마다 다른 쿠키 이름을 쓰는 resolver | 늦은 S 쿠키와 N 쿠키의 이름이 달라 N을 직접 덮지 못함. 새 인프라 없이 현재 Spring에서 설계 가능 | 중간~높음. 서버 기준 유효 세션 선택, 이전 쿠키 정리, 쿠키 개수·헤더 크기·logout 처리 필요 | **현재 계약을 유지할 수 있는 설계 후보.** 아직 전체 계약을 실행 검증한 구현은 아님 |

**A가 가장 단순하지만 현재의 활동 기반 브라우저 만료 연장을 바꾸므로 별도 정책 승인이 필요하다.** 그 계약을 유지한다면 C가 검토 후보이며, 단순 버그 수정이라고 확정하여 바로 추가하지 않는다. 클라이언트가 붙인 가장 큰 세대 숫자를 신뢰하거나 폐기된 S를 인증에 다시 허용해서는 안 된다. 서버 저장소에서 유효성과 세대를 판별해야 한다.

불완전한 Redis hash를 무효 세션으로 처리하는 mapper 보강은 세 안 모두에서 500 방지 보완책으로 검토할 수 있다. **mapper만 바꾸면 쿠키 S로 돌아가는 문제는 남는다.** 쿠키 발급을 체인 뒤로 옮기거나 한 번 재조회·서버 잠금을 거는 것만으로 브라우저의 실제 수신 순서까지 보장한다고 주장하지 않는다.

선택 전 필요한 판별은 실제 HTTP·격리 Redis에서 요청 A를 지연 → 다른 로그인 응답 N 수신 → A의 S 응답 수신 → 다음 인증 요청의 ID·상태·Redis hash를 관측하는 것이다. logout tombstone 경합과 로그인 rename 경합을 각각 확인하고, 활동 연장·최초 로그인 fixation 회전·다중 기기·현재 logout·탈퇴 회수를 함께 검증한다. 미수정 상태다.

## R-2 — 일정 수정과 소프트 삭제 경합

위치는 [ScheduleService](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/service/ScheduleService.java)의 수정·삭제·복원과 [Schedule](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/entity/Schedule.java)이다. 먼저 읽은 수정 트랜잭션이 삭제 커밋 뒤 전체 컬럼을 갱신하면서 deletedAt=null을 되돌릴 수 있다. 물리 삭제였던 base에서는 뒤늦은 UPDATE가 행을 다시 만들지 못했다.

| 방안 | 장점 | 단점·복잡도 | 계약 영향 |
| --- | --- | --- | --- |
| **A. Schedule에 @Version과 additive migration** | JPA의 기본 충돌 검사를 사용하고 수정·삭제·복원을 함께 보호. 기다리는 DB 잠금이 없음 | 낮음~중간. 버전 컬럼·기존 행 초기값 migration 필요. 충돌 시 사용자가 다시 조회 | 일반 API는 유지. 동시 수정은 기존 OptimisticLockingFailureException → 409/G005 처리로 연결 가능 |
| B. 쓰기 조회에만 비관적 행 잠금 | schema 변경 없이 쓰기를 순서대로 처리하고 활성 상태를 잠금 안에서 확인 | 중간. 느린 요청·잠금 대기·timeout을 고려해야 함. 모든 쓰기가 같은 경로를 사용해야 함 | 정상 API 유지. 경합 시 대기/실패 방식 변화 |
| C. 삭제 상태 조건을 포함한 metadata UPDATE와 영향 행 수 검사 | 삭제 컬럼을 덮어쓰지 않고 오래된 활성 수정 거부. schema 변경 없이 가능 | 중간. 필드 목록·updatedAt·bulk UPDATE 후 영속성 컨텍스트를 직접 관리 | 정상 API 유지. 영향 행 수 0의 오류 계약 결정·검증 필요 |

**현재 사용자 선택은 관리자 단독 운영을 고려한 현상 유지다.** 아래 기술 비교는 향후 다시 수정할 때의 제안으로 남긴다. 그때 기능 유지와 단순함을 함께 보면 A를 우선 검토할 수 있다. 현재 [공통 오류 처리](../apps/server/src/main/java/bhoon/sugang_helper/common/web/GlobalExceptionHandler.java)는 낙관적 충돌을 409/G005로 변환한다. Schedule만 대상으로 하고 다른 엔티티의 정책까지 확대하지 않는다. schema 변경을 피해야 한다면 B를 비교한다. 어느 안도 아직 구현하지 않았다.

승인 후 실제 DB의 서로 다른 두 트랜잭션에서 수정 조회 → 삭제 커밋 → 수정 커밋을 강제로 교차시켜 삭제 상태·일반/대표 목록 제외 또는 명확한 충돌 실패를 확인한다. 순차 mock 테스트만으로 통과했다고 하지 않는다. 운영 DB의 실제 isolation override는 확인하지 않았다. MySQL의 일반 consistent read와 DML 차이는 [공식 consistent-read 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html)와 [격리 수준 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)를 근거로 했다.

## 범위와 미검증 사항

CT-1·CT-2만 코드 수정한다. R-1은 제안이며 R-2는 현상 유지한다. 운영 데이터·설정 변경이나 migration 적용은 수행하지 않는다. 실제 OAuth·학교 API·운영 DB/Redis·TLS 프록시·저장소 밖 소비자는 검증하지 않았다. 새 프레임워크나 서비스 분리는 제안하지 않았다. 원래 리뷰의 세부 반박·실패한 실험은 로컬 원본 보고서에 보존한다.


## CT-1 수정 완료와 검증 기록

- 공통 client·오류 분류 함수·관리자 첨부 안내만 수정했다. Schedule 시간 입력과 서버의 세션 쿠키·일정 동시성 코드는 바꾸지 않았다.
- 변경 전 회귀 테스트는 다음 사용자 다운로드도 403으로 실패했다. 수정 후 집중 30건, 최종 전체 웹 79파일·311건이 통과했다. TypeScript·ESLint·production build도 통과했다.
- 최종 Playwright 9건은 재시도 없이 14.0초에 통과했다. 새 2건은 실제 Chromium의 같은 browser context 안에서 두 탭의 공유 SESSION 쿠키, 앱 로그아웃, 가짜 OAuth 로그인, 옛 토큰의 403, 다음 클릭의 새 토큰 성공을 연결한다. 실제 Google이나 운영 서버를 호출하지 않았다.
- 권한 403/A002와 잘못된 JSON Blob은 계속 거부됐다. CSRF 조회와 다운로드 수를 세어 자동 재시도·중복 발송 요청이 없는지 확인했다. 단위 테스트는 비 JSON·과대 Blob·500 및 늦은 읽기 중 세대 변경도 검증한다.
- 첫 lint는 shared 테스트의 feature 계층 import를 거부했다. 해당 테스트를 shared client의 자체 API 계약으로 바꾸고 실제 소비자 연결은 브라우저 테스트로 검증했다. 경계 규칙을 끄지 않았다. 첫 브라우저 실행의 신규 2건은 제목 앞의 `[버그]`를 빠뜨린 locator 때문에 실패했고, 실제 heading 이름으로 수정한 뒤 집중·전체 검사를 재시도 0으로 통과했다. 실패 로그를 보존했으며 skip을 추가하지 않았다.
- 승인한 변경에 대한 독립 후속 코드 검토에서도 추가 결함을 발견하지 못했다. 서버 코드와 API 계약은 그대로여서 이전 서버 543건·격리 Redis HTTP 7건을 이번 웹 수정 때문에 반복 실행하지 않았다.

실행 로그는 작업 디렉터리의 `blob-csrf-before.log`, `blob-csrf-focused.log`, `blob-csrf-web-full-final.log`, `blob-csrf-web-types-final.log`, `blob-csrf-web-lint-final.log`, `blob-csrf-web-build.log`, `blob-csrf-e2e-focused.log`, `blob-csrf-e2e-final.log`에 보존한다. 원본 dev·운영 환경·자격 증명·외부 발송은 변경하지 않았고 push·merge·deploy를 하지 않는다.


## R-1 상세: 쿠키 발급과 저장의 실제 경로

설명용 S는 이전 세션 ID, N은 새 ID다. 실제 토큰·쿠키 값은 읽거나 기록하지 않았다. 다음은 기존 S가 살아 있는 동안 로그인 완료가 그 ID를 회전시키는 경우다.

1. 요청 A가 S를 로드한다. SessionAuthenticationFilter는 현재 DB 사용자·역할을 확인하고 79행에서 `setSessionId(request, response, S)`를 호출한다. CookieHttpSessionIdResolver 77~82행은 요청 안에서 같은 ID를 두 번 쓰는 것만 막으며, DefaultCookieSerializer 122~161행은 실제 Set-Cookie 헤더를 추가한다. 아직 헤더를 받지 않은 브라우저와 느린 요청 A 사이에 시간이 생긴다.
2. 다른 요청 B에서 Google 로그인 성공 handler가 AuthService.login을 호출한다. 기존 세션이 있으면 30~32행의 `changeSessionId()`로 N을 만든다. RedisIndexedSessionRepository 964~996행은 저장 때 S hash를 N으로 rename하고, Spring Session은 바뀐 ID의 N 쿠키를 응답한다.
3. B 응답의 N을 브라우저가 먼저 저장한 뒤 A의 S 헤더를 받으면, 같은 이름·host·path 쿠키가 S로 다시 바뀐다. 브라우저가 Set-Cookie를 적용하므로 웹의 CSRF generation 검사만으로 그 적용을 막지 못한다.
4. Redis에 S가 없으면 다음 요청은 유효한 인증 세션을 찾지 못한다. A의 지연 delta 저장이 S에 변경 필드만 다시 쓰면 creationTime 등 필수 필드가 빠진 hash가 될 수 있다. RedisSessionMapper 66~82행은 필수 필드 누락에 예외를 내며 현재 필터는 500으로 거부한다.

이 경로의 영향은 새 로그인 손실·오류 반복이다. 분석한 기본 mapper에서 불완전 hash를 정상 구세션으로 인정하지 않는다. 이전 계정의 유효 인증 복원이나 계정 탈취를 실행으로 입증한 결과는 없다. 실제 HTTP·Redis·브라우저 순서 재현은 여전히 미완료다.

로그아웃 경로는 다르다. AuthService.logout은 session.invalidate와 expireSession을 호출하고 indexed repository의 deleteById는 maxInactiveInterval=0을 저장한다. 기존 실제 Redis E2E는 이 삭제 후 늦은 저장이 인증을 되살리지 않는 경우를 통과했다. 로그인 회전의 rename에는 이 만료 표시가 없으므로 두 경로를 같은 보장으로 취급하지 않는다. 로그아웃 뒤 재로그인에서도 늦은 쿠키가 인증을 끊는 순서는 따로 확인해야 하며, 모든 경우가 같은 500 경로라고 단정하지 않는다.

### 세 해결안의 정확한 전제

- **설정만으로 발급을 막을 수 없다.** 현재 serializer의 max-age를 30일로 두면 writeCookieValue가 호출될 때마다 Expires를 현재 시각+30일로 쓴다. Redis TTL만 고정하거나 cookie max-age만 바꿔도 필터 79행의 일반 응답 Set-Cookie 호출은 남는다.
- **A 고정 30일:** 일반 인증 요청의 명시적 setSessionId를 제거하고, 로그인/ID 변경 시 쿠키를 발급하는 경로를 사용해야 한다. 그러면 브라우저 만료는 로그인 시점 기준이며 Redis idle 만료가 연장돼도 쿠키는 연장되지 않는다. 서버 인증 자체의 절대 30일 상한과는 다르다. 서버도 로그인 시점부터 정확히 30일에 만료시키려면 creation time 기준의 별도 규칙을 결정·검증해야 하며 현재 idle 설정만으로 그 보장은 생기지 않는다.
- **B 브라우저 세션 쿠키:** 위 일반 요청 재발급 제거와 함께 cookie max-age를 -1로 설정한다. -1은 Max-Age/Expires를 생략하는 뜻이며, 그 설정 자체가 Set-Cookie 호출을 막지는 않는다. 브라우저 종료 후 유지·복원은 브라우저 정책에 의존한다.
- **Spring 기본 commit:** SessionRepositoryFilter 218~233행은 요청 세션을 저장한 뒤 ID가 유효하지 않거나 요청 ID와 달라졌을 때 쿠키를 쓴다. 기존의 유효한 같은 ID인 일반 응답은 이 조건으로 재발급하지 않는다. 새 세션 생성·ID 회전·무효화의 Set-Cookie는 계속 필요하므로 A/B가 모든 쿠키 응답이나 모든 경합을 없앤다고 표현하지 않는다.
- **C 세대별 이름:** 현재 SESSION 하나를 쓰는 기본 resolver 대신 요청별 세대를 구분하는 custom resolver·정리 정책이 필요하다. 기본 serializer의 이름 설정을 공유 객체에서 요청마다 바꾸는 방식은 동시성 검토 없이 적용할 수 없다. 서버 기준 유효 세대 선택·쿠키 정리·헤더 제한·logout·탈퇴를 함께 검증해야 하는 설계 후보다.

A/B는 현재 일반 인증 응답의 무조건 갱신이라는 원인을 줄이면서 로그인 유지 정책을 바꾸는 안이다. C는 그 정책을 유지하는 후보지만 구현 복잡도가 크다. 어느 안도 이번 작업에서 적용하지 않았다. 불완전 hash mapper 보강 역시 미구현이며 쿠키 덮어쓰기와 구분한다. 근거는 로컬에 설치된 Spring Session 3.5.4 실제 소스이며 운영 Redis·자격 증명·설정은 읽거나 변경하지 않았다.


## CT-2 수정 완료와 현재 선택

- 사용자 선택에 따라 기본 type=time 입력 두 개를 직접 사용한다. 사용자 정의 TimePicker·시/분 배열·전용 Select 그룹을 제거했고 초기화 버튼과 시간 생략은 보존했다. 서버·API·DB·세션 코드는 변경하지 않았다.
- 변경 전 기본 입력 회귀 3건은 실패했고, 변경 후 집중 16건과 전체 웹 79파일·315건이 통과했다. TypeScript·ESLint·production build도 통과했다.
- 최종 Playwright 9건은 재시도 없이 20.2초에 통과했다. 실제 Chromium에서 20:00→23:55 생성, 날짜만 바꿀 때 시각 보존, 09:01→23:57 변경, 대표 선택 유지·삭제·복원을 연결하고 다섯 변경 요청의 CSRF와 HH:mm payload를 검사했다. 날짜 경계·서버 저장을 새로 검증한 테스트는 아니며 가짜 API를 사용한다.
- 단위 테스트는 기존 20:01·23:57 보존, 00:00·23:59와 분 단위 값, 시간 초기화 후 생략을 확인했다. 브라우저·OS별 기본 시간 입력의 표시 방식 차이는 수용한 비용이다.
- R-1은 코드·의존성 설명과 해결안 비교만 추가했다. R-2는 사용자 결정에 따라 현상 유지한다. 실제 동시 HTTP/Redis 경합·DB 트랜잭션 재현은 추가 실행하지 않았다.

이번 로그는 작업 디렉터리의 `native-time-before.log`, `native-time-focused.log`, `native-time-web-full.log`, `native-time-web-types.log`, `native-time-web-lint.log`, `native-time-web-build.log`, `native-time-web-e2e.log`에 있다. 새 테스트 skip이나 실패 무시는 없다. 변경한 문서의 내부 링크 이상은 0이며 기존 저장소 전체의 오래된 링크 159개는 그대로다.

## 도입 이력과 첨부 기능의 의미

관리자 첨부 열람은 사용자가 `/feedback/write`의 문의·버그 제보에 JPEG/PNG 이미지를 첨부하고 관리자가 `/admin/feedbacks`의 전체 문의 관리에서 열어보는 기능이다. 관리자 다운로드 API는 `POST /api/v1/admin/feedbacks/{feedbackId}/attachments/{attachmentId}/download`다.

Git diff/blame으로 CT-1·R-1의 도입은 `a6ab4b5ab`, CT-2·R-2의 도입은 `c7890f6fb`임을 확인했다. CT-2의 제한된 picker 자체는 dev에 이미 있었지만 이번 통합에서 자유로운 type=time 페이지를 없애면서 입력 가능 범위가 축소됐다. dev와 보관된 origin/main `8efb5c4e5`에도 Redis 세션이 있었으며, R-1의 매 인증 응답 직접 SESSION 재발급이 이번에 추가된 부분이다. 기존 일정 삭제는 실제 DELETE였고 soft delete의 상태 덮어쓰기는 이번 통합으로 생긴 경로다. 원격 최신 상태를 새로 조회한 결과는 아니며 Git 이력 확인이 R-1·R-2의 실행 재현을 대신하지는 않는다.
