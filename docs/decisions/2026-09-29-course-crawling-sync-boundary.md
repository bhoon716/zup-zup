# 결정: 강의 수집 결과 동기화 경계

- 상태: Accepted
- 결정일: 2026-09-29
- 범위: `apps/server`의 강의 카탈로그·강의 수집 코드 경계와 동기화 트랜잭션
- 대체 결정: 없음
- 관련 자료: [백엔드 논리 도메인 경계 초안](2026-09-29-server-domain-boundaries.md), [카탈로그 동기화](../../apps/server/src/main/java/bhoon/sugang_helper/academic/course/application/CourseCatalogSynchronizationService.java), [크롤링 완료 조정](../../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/application/CourseCrawlCompletionService.java)

## 배경과 결정 질문

`crawling`의 `CourseSynchronizationService`가 `courses`와 `course_seat_histories`를 직접 썼고, `course`의 서비스와 컨트롤러는 크롤링 구현체를 직접 조회했다. 강의 데이터 소유권과 양방향 의존을 정리하면서 좌석 변경 알림의 원자성을 보존해야 했다.

## 선택지

| 선택지 | 얻는 점 | 비용·위험 |
| --- | --- | --- |
| 기존 동기화 코드를 crawling에 유지 | 변경이 작다. | 강의 테이블 쓰기 소유자가 계속 모호하고 양방향 참조가 남는다. |
| course가 강의 데이터를 쓰고 crawling이 조정 | 테이블 쓰기 소유권과 의존 방향이 분명해진다. | 경계 계약과 트랜잭션 회귀 검증이 필요하다. |
| 서비스·DB를 물리적으로 분리 | 런타임 격리를 얻을 수 있다. | 현재 필요를 뒷받침하는 근거가 없고 분산 트랜잭션 비용이 생긴다. |

## 결정

- `course` 애플리케이션이 강의와 좌석 이력을 저장하고 `SeatOpenedEvent`를 발행한다.
- `crawling` 애플리케이션이 fetch/parse 결과를 `course` 저장 use case에 넘기고, 같은 트랜잭션 안에서 크롤링 성공 상태를 기록한다.
- `course` 응답에 필요한 대상 학기와 마지막 성공 시각은 `course`가 소유한 조회 인터페이스로 요청하고, `crawling`이 구현한다. 검색 기본 학기 API 경로와 응답은 유지한다.
- 알림 outbox는 기존 `BEFORE_COMMIT` 이벤트 리스너로 기록한다. 스키마와 배포 구조는 바꾸지 않는다.

## 근거와 가정

- 통합 테스트에서 강의 좌석 변경, 좌석 이력, 크롤링 성공 상태, 알림 outbox가 함께 커밋되고 외부 트랜잭션 롤백 시 모두 남지 않는 것을 확인했다.
- `course` 운영 코드에서 `crawling` 패키지 import가 없어졌다. `crawling`에서 `course` 애플리케이션 계약을 참조하는 방향은 남는다.
- 같은 MySQL과 Spring 트랜잭션 매니저를 사용한다는 현재 구조를 전제로 한다.

## 결과와 위험

- `courses`와 `course_seat_histories` 쓰기는 `course`가 소유하고, 외부 수집과 실행 상태는 `crawling`이 소유한다.
- 두 업무는 여석 이벤트 outbox와 성공 상태를 한 트랜잭션으로 맞추기 위해 실행 시 결합되어 있다. 이는 코드 경계 정리이지 런타임 장애 격리 결정이 아니다.
- `crawler_*` 상태 갱신 또는 outbox 저장 방식을 별도 DB/서비스로 옮기면 원자성을 다시 설계해야 한다.

## 검증과 재검토 조건

- 편집 전 관련 테스트(`CourseSynchronizationServiceTest`, `CourseCrawlerServiceTest`, `CourseServiceTest`, `CourseControllerValidationTest`, `SeatNotificationOutboxTransactionTest`) 통과.
- 변경 뒤 서버 전체 `./gradlew test` 통과.
- 추가한 `CourseCrawlCompletionTransactionTest`가 함께 커밋과 롤백을 실제 JPA 트랜잭션으로 확인한다.
- 별도 데이터베이스, 다른 트랜잭션 관리자 또는 비동기 이벤트 발행으로 전환할 때 재검토한다.
