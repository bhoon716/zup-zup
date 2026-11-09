# 백엔드 모듈과 공개 계약

하나의 Spring Boot 애플리케이션·DB·Gradle 프로젝트를 유지한다. 6개 업무 모듈, 2개 조합 모듈, 2개 지원 모듈을 Spring Modulith 1.4.14로 검증한다. [의사결정 이력](decisions/2026-11-05-spring-modulith.md)의 마지막 변경 결정이 현재 기준이다.

## 모듈 경계

| 구분 | 모듈 | 책임 | 공개 계약 |
| --- | --- | --- | --- |
| 업무 | identity | 계정·OAuth·세션·온보딩·인증·수신 설정·기기·탈퇴 | users / notifications / events |
| 업무 | academic | 강의·시간·좌석 이력·수집·추적 학기·리뷰 | catalog / operations / events / search |
| 업무 | planning | 개인 시간표·사용자 일정·찜 | api |
| 업무 | seatalert | 구독·대상 결정·outbox·발송·재시도·발송 이력 | api |
| 업무 | campus | 공지·학사 일정 | api |
| 업무 | support | 문의·답변·첨부의 수명주기 | 외부 공개 Java API 없음 |
| 조합 | dashboard | 사용자 화면용 조회 결과 조합 | 외부 공개 Java API 없음 |
| 조합 | admin | 운영 조회·여러 업무의 명령 조합 | 외부 공개 Java API 없음 |
| 지원 | audit | 관리자 감사 기록·메타데이터·조회·보존 | api |
| 지원 | common | 검토한 공통 오류·응답·기술 지원 | api |

HTTP controller가 public이라는 사실은 다른 Java 모듈의 공개 계약이라는 뜻이 아니다. 업무별 관리자 controller는 해당 업무 모듈에 둔다. admin/dashboard가 다른 업무의 엔티티·저장소를 직접 조작하지 않는다. 모듈은 독립 배포 단위나 미래의 마이크로서비스로 가정하지 않는다.

## 실제 공개 패키지

구현 서비스의 TYPE 어노테이션으로 경계를 열지 않는다. 아래 패키지에 `@NamedInterface`를 선언하고, 소비자는 인터페이스와 데이터 타입을 참조한다. 기존 서비스는 원래 MVC 위치에서 구현한다.

| 패키지 / Named Interface | 주요 계약 |
| --- | --- |
| academic.api.catalog / catalog | CourseCatalog, TrackedSemesterQuery, CourseSummary·CourseName·CourseScheduleSummary·TrackedSemester |
| academic.api.operations / operations | CrawlerTargets, CrawlerRuns, CrawlerAdminSnapshot |
| academic.events / events | SeatOpenedEvent |
| academic.api.search / search | CourseWishlistQuery — 아래의 명시적 검색 확장 계약 |
| identity.api.users / users | Users, UserDisplayNames, UserDashboardProfiles 및 조회 DTO |
| identity.api.notifications / notifications | NotificationRecipients, WebPushSubscriptionManagement, NotificationRecipient·NotificationDevice |
| identity.events / events | UserRegisteredEvent, UserWithdrawnEvent |
| planning.api / api | Timetables 및 시간표·하위 시간 DTO |
| seatalert.api / api | Subscriptions, NotificationHistoryQueries, NotificationOperations, NotificationChannel·NotificationHistoryItem |
| campus.api / api | Announcements, AcademicSchedules 및 조회 DTO |
| audit.api / api | AuditRecorder |
| common.api의 검토한 하위 패키지 / api | RedisAccess, FileStorage, EmailTemplates, OperationalAlerts, 오류·응답·보안 도구·설정 값·기반 영속성 타입 |

공개 DTO는 업무 엔티티를 필드·인자·반환 타입으로 노출하지 않는다. 공통 기반 mapped superclass와 생성 Querydsl 기반 메타모델은 common.api.persistence에 한정한다. 오류 처리·보안 설정이 이미 구현하는 Spring 인터페이스는 별도 중복 계약을 만들지 않고 그대로 활용한다.

구독은 `TrackedSemesterQuery.currentTarget()`의 연도·학기 코드만 읽는다. 크롤러 관리 응답·변경·정규화는 노출하지 않는다. 구독 개수·추적 학기 허용 정책은 seatalert에 남는다.

## 허용 의존 방향

`A → B`는 A 코드가 B의 지정된 공개 계약을 참조한다는 뜻이다. 실제 호출이나 이벤트 실행 방향과 구분한다. 사용하는 계약만 선언하며 wildcard·open module·위반 필터는 없다.

| 모듈 | 실제 allowedDependencies |
| --- | --- |
| common | 없음 |
| identity | common::api |
| audit | common::api |
| campus | common::api |
| academic | identity::users, common::api |
| planning | academic::catalog, academic::search, identity::users, identity::events, common::api |
| seatalert | academic::catalog, academic::events, identity::users, identity::notifications, identity::events, audit::api, common::api |
| support | identity::users, identity::events, audit::api, common::api |
| dashboard | identity::users, planning::api, seatalert::api, campus::api |
| admin | academic::operations, identity::users, identity::notifications, seatalert::api, common::api |

common은 업무 모듈을 참조하지 않고 업무 모듈은 admin/dashboard를 참조하지 않는다. 감사는 행위자 ID를 입력받고 인증·권한은 요청을 처리하는 쪽에서 확인한다. 감사 기록은 필요할 때 호출자의 트랜잭션에 동기로 참여한다.

## 첨부 설계에서 추가로 확인한 검색 연결

기존 CourseSearchReadRepository에는 찜 전용 필터와 인기순 정렬을 위한 Wishlist 서브쿼리가 있다. 찜을 planning에 유지하면서 이 Q 타입을 academic에서 직접 import하면 planning → academic과 반대 의존이 생긴다.

이번에는 `academic.api.search.CourseWishlistQuery`라는 두 메서드의 조회 확장 SPI를 두고 planning의 WishlistCourseSearch가 자기 테이블의 조건과 count 서브쿼리를 구성한다. academic은 자기 courseKey 표현식과 사용자 ID를 넘기고 조건·숫자 표현식을 받아 기존 SQL 필터·정렬·페이지 처리를 수행한다. 엔티티·Q 타입·테이블 이름을 공개 인자로 노출하지 않으며 문자열 SQL로 경계를 숨기지 않는다.

이 SPI는 일반적인 ID/조회 DTO 계약과 달리 Querydsl 표현식을 반환하는 기술 계약이다. 공개 기술 결합을 수용하는 지점이며 planning만 academic::search를 사용한다. 모든 찜을 메모리에 읽어 정렬하거나 캐시·복제 모델·DB migration을 추가하는 방식은 선택하지 않았다. 이 계약은 실제 조인 검색을 보존하기 위한 한 곳의 예외이며 범용 Repository·EntityManager나 임의 SQL 실행 권한을 노출하지 않는다.

정적 의존은 planning → academic이다. 실행 중 academic이 planning 구현을 호출하는 협력은 남는다. 모듈 간 협력을 없애는 것이 아니라 의존 방향과 소유권을 명시하는 선택이다. 실제 SQL 데이터와 HTTP로 인기순 동점 정렬·페이지 경계·인증 사용자의 찜만 조회하는 회귀를 검증한다.

## 동기 이벤트와 기존 Outbox

SeatOpenedEvent는 academic.events가 소유한다. 강의 동기화 트랜잭션 안에서 발행하고 seatalert 내부 SeatOpenedListener가 즉시 기존 SeatNotificationOutboxService.enqueue를 호출한다. enqueue는 `Propagation.MANDATORY`를 유지한다.

```text
강의·좌석 이력 변경
  → SeatOpenedEvent
  → 동기 SeatOpenedListener
  → 기존 outbox 저장 (MANDATORY)
  → 크롤러 성공 상태 기록
  → 같은 트랜잭션 commit

commit된 outbox → 기존 materialize/claim/발송/재시도/DLQ
```

리스너에 @Async, @ApplicationModuleListener, AFTER_COMMIT, REQUIRES_NEW나 예외를 삼키는 처리기를 적용하지 않는다. 메모리 이벤트 자체가 내구성을 제공하는 것이 아니라 업무 상태와 함께 저장되는 기존 outbox가 제공한다. 새 Event Publication Registry나 중복 발송 큐를 추가하지 않는다.

UserWithdrawnEvent는 identity.events가 소유한다. identity는 계정·기기를 처리하고 seatalert/support가 동기 MANDATORY 수신자로 자기 데이터를 정리한다. UserRegisteredEvent의 기본 시간표 생성도 유지한다. Redis 세션·인증 상태 정리는 기존 위치와 정책을 유지하며 DB rollback으로 되돌아간다는 보장은 하지 않는다.

테스트 알림은 seatalert의 UserNotificationTestService와 controller가 소유한다. `/api/v1/notifications/test`, `/api/v1/users/notifications/test` 두 URL과 인증·CSRF·오류 코드·채널 확인·cooldown·발송 실패 의미를 유지한다. 이메일 인증과 사용자 기기/수신 설정의 소유권은 identity에 남는다.

## 검증 명령과 범위

apps/server에서 실행한다.

```bash
./gradlew test --tests 'bhoon.sugang_helper.modularity.*'
./gradlew modulithDocs
./gradlew modulithE2e
./gradlew sessionTest
```

기존 PR CI의 server 단계는 check·migrationTest와 함께 modulithE2e·sessionTest를 실행하도록 연결했다. 현재 작업은 커밋·푸시하지 않았으므로 새 원격 CI 실행 결과는 없다.

일반 test/check에 구조 검사와 공개 계약 누출 검사가 포함된다. 실제 모듈 10개를 고정하고 순환·내부 접근·미선언 의존성을 예외 없이 검사한다. API/events가 업무 구현·엔티티·저장소에 의존하거나 구현 클래스에 TYPE NamedInterface를 붙이는 회귀도 막는다. 구조 검증은 런타임 동작이나 문자열 SQL의 소유권을 자동 보장하지 않으므로 별도 검토한다.

modulithDocs는 검증한 모델에서 build/spring-modulith에 다이어그램·캔버스·집계 문서를 생성한다. 모듈 메타데이터는 build/resources/main/META-INF/spring-modulith/application-modules.json에 생성한다.

modulithE2e는 별도 src/e2e/java의 실제 SugangHelperApplication을 loopback 임의 포트로 기동한다. 메모리 H2·격리 Redis만 허용하고 생산 설정을 읽지 않는다. 외부 transport는 mock이며 outbox worker는 자동 실행하지 않는다. 필요한 시험은 실제 processor를 HTTP fixture로 명시 호출한다. 대상 리스너는 수동 등록하지 않는다.

다음 동작을 실제 JPA/H2 상태와 HTTP로 확인한다.

- 서비스 경로의 자체 트랜잭션, 리스너 자동 탐색, 정상 commit·호출자 실패·수신자 실패 rollback, 앞 청크 데이터 rollback
- 탈퇴의 계정·구독·문의·기기 상태와 트랜잭션 밖 이벤트 거절
- 인기순 동점 정렬·페이지 경계·인증 사용자 찜 필터와 이동한 알림 URL
- commit된 강의가 모의 외부 발송 실패로 rollback되지 않고 재시도가 예약되는 흐름
- 실제 관리자 API의 재전송과 감사 기록이 함께 commit되고 감사 저장 실패 시 함께 rollback되는 흐름

sessionTest는 별도 격리 Redis·실제 HTTP로 세션·CSRF·권한 회귀를 확인한다. 기존 DB/컨텍스트 통합 테스트는 저장소 지침에 따라 로컬에서 실행하지 않는다. 첨부안의 @ApplicationModuleTest 통합 실행은 구조 단위 검사와 최종 E2E로 대체하며, 운영 변경·커밋·PR은 하지 않는다.

H2 MySQL 모드는 실제 MySQL의 SQL·격리·잠금·Flyway를 대체하지 않는다. 모의 transport 실패는 실제 제공자 전송이 아니다. 같은 스냅샷 재처리의 추가 outbox 없음은 동일 이벤트의 중복 발행·동시성·정확히 한 번 전달 보장이 아니다. 바깥 탈퇴 이벤트가 첫 수신자의 실패에서 중단되면 두 수신자 각각의 거절을 모두 입증하는 것도 아니다.
