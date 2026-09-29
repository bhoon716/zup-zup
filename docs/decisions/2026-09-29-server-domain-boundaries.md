# 결정: 백엔드 논리 도메인 경계 초안

- 상태: Proposed
- 결정일: 2026-09-29
- 범위: `apps/server`의 논리적 책임·테이블 소유권·도메인 간 코드 계약
- 대체 결정: 없음
- 관련 자료: [재설계 계획](../backend-infra-redesign-plan.md), [현재 시스템 인벤토리](../current-system.md), [바텀업 백엔드 리팩토링](2026-09-29-bottom-up-backend-refactoring.md)

## 배경과 결정 질문

현재 최상위 패키지는 기능별로 나뉘어 있지만, 폴더 이름만으로 독립된 도메인이나 데이터 소유권을 보장하지 않는다. 도메인을 나누는 기준과 첫 리팩토링 전에 지켜야 할 경계를 정할 필요가 있다.

이 제안은 논리적 책임과 코드 의존 방향을 정한다. Gradle 멀티모듈, 별도 프로세스, 별도 DB로 나누거나 API를 변경하자는 결정은 아니다. 운영 구조와 외부 API는 현재 상태를 따른다.

사용자 요청에 따라 2026-09-29에 Java 패키지의 상위 폴더를 아래 업무 영역으로 이동했다. 이 변경은 탐색을 돕는 물리적 그룹화다. 서로 다른 기능 패키지 사이의 import와 계층 규칙까지 강제한 상태는 아니며, 아래 근거에 적은 경계 누수는 후속 리팩토링 대상으로 남는다.

```text
identity/{auth,user}
academic/{course,crawling,review}
planning/{wishlist,timetable}
seatalert/{subscription,notification}
campus/{announcement,schedule,dday}
support/{feedback}
admin, dashboard, common  # 조합·기술 지원 영역
```

## 선택지

| 선택지 | 얻는 점 | 비용·위험 |
| --- | --- | --- |
| 기존 feature 패키지를 도메인으로 간주 | 당장 이동 작업이 없다. | 실제 교차 저장소 접근과 소유권 혼선을 그대로 둔다. |
| 현재 쓰기·테이블·규칙을 기준으로 논리 경계를 정하고 단일 앱 안에서 점진 정리 | 데이터 소유와 변경 이유를 구별하면서 배포 복잡도를 유지한다. | 경계를 세부 흐름으로 확인하고, 일부 기존 호출을 계약으로 바꿔야 한다. |
| 서비스·DB·빌드 모듈을 한 번에 분리 | 격리와 컴파일 경계가 강해질 수 있다. | transaction·배포·운영 복잡도가 커지며 현재 근거만으로 필요성을 증명하지 못한다. |

## 결정

다음 논리 경계를 목표안으로 제안한다. 같은 Spring Boot 애플리케이션과 MySQL 안에서 시작하며, 아래 표의 경계는 먼저 소유권과 참조 규칙을 뜻한다. 패키지 이동이나 빌드 모듈 생성은 별도 단계에서 작은 단위로 검증한다.

| 논리 경계 | 소유할 데이터·규칙 | 경계 밖에 제공할 것 |
| --- | --- | --- |
| 계정·인증 | `users`, `user_devices`, 계정 설정·권한·인증 상태·탈퇴 | 사용자 ID, 인증 주체와 필요한 계정/채널 설정 조회 |
| 강의 카탈로그 | `courses`, `course_schedules`, `course_seat_histories`; 검색·강의·좌석 상태 | 검색/상세 조회와 검증된 강의 snapshot 반영 명령 |
| 강의 수집 | JUMP 연결·파싱·수집 스케줄·대상 학기·실행 상태 (`crawler_*`) | 수집 상태 조회와 검증을 마친 snapshot 전달 |
| 좌석 알림 구독 | `subscriptions`; 구독·해지·구독 제한·학기 유효성 | 알림 대상 사용자 조회 계약 |
| 강의 찜 | `wishlists`; 찜 추가·삭제 | 사용자 ID와 강의 키를 쓰는 찜 명령·조회 |
| 알림 전달 | `notification_histories`, `seat_notification_outbox`, `seat_notification_deliveries`; 수신자별 생성·전송·재시도·DLQ | 알림 발송/이력/운영 명령 계약 |
| 시간표 | `timetables`, `timetable_entries`, `custom_schedules`, `custom_schedule_times` | 시간표 명령·조회. 강의는 `course_key`로 참조 |
| 강의 리뷰 | `course_reviews`, `course_emoji_reviews`, `course_review_reactions` | 리뷰/반응 명령·조회. 강의 키와 사용자 ID를 사용 |
| 캠퍼스 정보 | 공지(`announcements`), 학사 일정(`schedules`), D-day(`dday_settings`) 각각의 CRUD·공개 여부 | 각 기능별 공개 조회·관리 명령 |
| 피드백·첨부 | `feedbacks`, `feedback_replies`, `feedback_attachments`; 첨부 접근·보존 규칙 | 사용자/관리자 피드백 명령·조회와 첨부 접근 |
| 감사 기록 | `admin_action_logs`; 관리자 작업의 최소 감사 정보 | 기록 명령과 감사 로그 조회 |
| API 조합·운영 화면 | 관리자·대시보드 응답 조합, HTTP 인증/응답 변환 | 각 소유 경계의 애플리케이션 계약만 호출; 업무 테이블을 직접 수정하지 않음 |
| 기술 공통 | HTTP 예외 매핑, 시계, 보안 주체 등 실제 공유가 필요한 기술 요소 | 업무 규칙이나 여러 도메인의 저장소를 소유하지 않음 |

찜과 좌석 알림 구독은 현재 별도 동작과 제약이 있으므로 같은 `user-course interest` 모델로 합치지 않는다. 추후 같은 불변 규칙을 공유한다는 근거가 생기면 다시 검토한다. 공지·학사 일정·D-day도 관리자 CRUD가 있다는 이유만으로 하나의 데이터 소유자로 합치지 않는다.

## 근거와 가정

- 확인된 양방향 의존: `academic/course/application/CourseService.java`가 `CourseCrawlerTargetService`·`CrawlerRunStateService`를 조회하고, `academic/crawling/application/CourseSynchronizationService.java`는 `academic.course`의 엔티티·repository를 사용해 강의와 좌석 이력을 쓴다.
- 확인된 원자성 요구: `CourseSynchronizationService`가 좌석 변화 이벤트를 발행하고 `seatalert/notification/application/SeatNotificationOutboxService.java`가 `BEFORE_COMMIT` 이벤트 리스너에서 outbox를 기록한다. 강의 좌석 변경·이력과 outbox 기록이 같은 DB commit에 들어가는 현재 의미를 보존해야 한다.
- 확인된 알림 의존: `seatalert/notification/application/SeatNotificationOutboxProcessor.java`가 `SubscriptionRepository`, `UserRepository`, `UserDeviceRepository`와 사용자/구독 엔티티를 직접 사용한다. `identity/user/domain/User.java`도 `seatalert.notification.infra.NotificationChannel`을 직접 참조한다.
- 확인된 교차 쓰기: `identity/user/application/UserService.java`의 `withdraw()`는 계정·토큰/Redis 상태와 함께 구독을 취소하고 기기를 삭제하며 피드백을 soft-delete한다. 따라서 계정 탈퇴는 여러 데이터 소유 경계를 가로지르는 고위험 흐름이다.
- 확인된 소유권 혼선: `support/feedback/domain/AdminActionLog.java`와 repository·타입은 `support.feedback` 패키지에 있지만 `admin/application/AdminAuditService.java`가 피드백 변경과 알림 delivery replay 감사를 함께 기록한다.
- 확인된 조합 책임: `dashboard/application/DashboardService.java`는 사용자, 시간표, 공지, 학사 일정, 알림 이력을 조합한다. `admin/application/AdminService.java`도 계정·구독·알림·수집 상태를 집계한다. 두 클래스는 각 데이터의 소유자라기보다 화면/운영 조합 지점에 가깝다.
- 추가 교차 조회: `seatalert/subscription/application/SubscriptionService.java`와 `planning/wishlist/application/WishlistService.java`는 강의·사용자 repository를 조회해 검증·응답을 만든다. `academic/course/application/CourseService.java`는 사용자와 리뷰 repository를 조회해 리뷰 여부를 계산한다. 실제 JPA write인지 조회 조합인지 구분한 뒤 경계별 계약을 정해야 한다.
- 가정 또는 미확인: 모든 테이블의 전체 쓰기 경로와 동적·reflection 기반 소비자, 도메인 간 트랜잭션 빈도는 아직 완전한 정적/동적 분석을 하지 않았다. 표는 첫 코드 조사에 근거한 목표 초안이다.
- 결정을 바꿀 수 있는 증거: 기능별 같은 불변 규칙·수명주기·변경 이유가 반복적으로 함께 바뀌거나, 반대로 별도 경계를 유지하기 위한 계약 비용이 이점보다 큰 경우.

## 경계 규칙

1. 각 테이블에 쓰기 소유자를 하나만 둔다. 다른 경계의 JPA 엔티티·repository를 직접 사용하지 않는다.
2. 경계 간 참조는 사용자 ID·강의 키 같은 안정된 식별자와 필요한 조회/명령 DTO로 한다. 조회 조합은 허용하지만 다른 경계의 데이터를 직접 갱신하지 않는다.
3. 단일 MySQL 내부에서 원자성이 필요한 흐름은 기존 transaction 의미를 보존한다. 특히 강의 좌석 변경·이력과 notification outbox 기록은 함께 커밋되는 계약을 먼저 고정한다.
4. HTTP controller와 관리자/대시보드 조합 코드는 데이터 소유자가 아니다. `common`에도 업무 규칙을 옮기지 않는다.
5. 경계 정리는 한 소유권 또는 실패 경계씩 한다. 패키지 이동, 스키마 변경, API 의미 변경을 한 변경에 묶지 않는다.

## 결과와 위험

- 기대 효과: 변경해야 할 규칙과 테이블이 어느 기능에 속하는지 찾기 쉬워지고, 새 교차 import를 줄일 기준이 생긴다.
- 감수하는 비용·제약: 한 응답에서 여러 기능 데이터를 조합하는 코드는 남을 수 있으며, 조회/명령 계약이 추가될 수 있다.
- 대응할 위험: 기존 transaction과 웹 API 계약을 먼저 테스트로 고정한다. 교차 조회를 일괄 금지하지 않고 소유 데이터 변경과 구분한다. 사용자 탈퇴, 좌석 수집/outbox, 알림 전달은 세부 영향·실패 의미를 확인하기 전 이동하지 않는다.

## 검증과 재검토 조건

2026-09-29에 패키지 이동 전후 `apps/server`에서 `./gradlew test`를 실행해 모두 통과했다. 이 검사는 컴파일과 현재 테스트 동작을 확인하지만, 도메인 간 import가 적절하거나 테이블 소유권 규칙이 지켜진다는 뜻은 아니다. 다음 단계에서 패키지별 import 방향, 실제 테이블 read/write 경로, transaction 경계를 더 확인해 이 초안의 소유자를 검증한다. 첫 동작 변경은 확인된 작은 경계 하나만 대상으로 하고, 기존 API·DB 결과·실패 계약을 같은 테스트로 비교한다.
