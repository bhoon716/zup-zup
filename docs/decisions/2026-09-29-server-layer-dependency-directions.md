# 결정: 서버 도메인·계층 의존 방향

- 상태: Accepted
- 결정일: 2026-09-29
- 범위: `apps/server`의 Java 패키지 간 컴파일 의존성
- 대체 결정: 없음
- 관련 자료: [논리 도메인 경계 초안](2026-09-29-server-domain-boundaries.md), [강의 수집 결과 동기화 경계](2026-09-29-course-crawling-sync-boundary.md)

## 배경과 결정 질문

도메인 이름으로 패키지를 묶었지만 실제 코드에는 application→infra 직접 참조, 타 도메인 엔티티·저장소 접근, 상호 참조가 남아 있다. 한 Spring Boot 애플리케이션과 기존 DB/API를 유지하면서 코드 의존 방향을 정한다.

## 선택지

| 선택지 | 얻는 점 | 비용·위험 |
| --- | --- | --- |
| 현재 import를 유지하고 문서만 보강 | 비용이 없다. | 계층 경계가 컴파일 시점에 보이지 않고 순환 참조가 계속 늘 수 있다. |
| 단일 앱 안에서 port·contract로 의존을 정리 | 저장소/API 경계와 업무 로직을 유지하면서 참조 방향을 고정할 수 있다. | 교차 조회와 조합 코드를 계약으로 옮기고 기존 동작을 검증해야 한다. |
| Gradle 모듈 또는 별도 서비스로 분리 | 더 강한 빌드·배포 격리가 가능하다. | 현 단계에서 분리 근거가 없고 빌드·운영·트랜잭션 부담이 커진다. |

## 결정

- `presentation`은 같은 업무 경계의 application use case만 호출한다.
- `application`은 자기 도메인과 자신이 소유한 inbound/outbound contract에 의존한다. `infra` 구현 클래스는 직접 참조하지 않는다.
- `domain`은 `application`, `presentation`, `infra` 패키지를 참조하지 않는다. 타 도메인의 entity·repository도 참조하지 않는다.
- `infra`는 자기 도메인과 그 경계가 소유한 port를 사용하고 port를 구현한다. 다른 업무 경계의 repository/entity 또는 application service를 직접 사용하지 않는다.
- `admin`·`dashboard`는 조합 지점으로 유지한다. 각 데이터 소유자의 application query/command contract만 호출하고 소유자 repository/entity에는 접근하지 않는다.
- `readmodel`은 여러 데이터 소유자의 정보를 합치는 조회 전용 조합 지점이다. 필요한 다른 도메인 entity/query를 명시적으로 참조할 수 있지만 쓰기 책임을 갖지 않으며, 도메인 application은 readmodel 구현에 의존하지 않는다. 조인·정렬·페이징 결과는 통합 테스트로 고정한다.
- `common`은 실제 공유되는 기술 기반만 둔다. 도메인 간 공용 enum/event/DTO는 업무 contract 패키지에 두고, `common`에서 업무 패키지로 의존하지 않는다.
- 업무 경계 그래프에 상호 의존 순환을 두지 않는다. 필요한 조합은 명시적인 application port, query DTO, 또는 안정된 이벤트 contract로 표현한다.

API 경로·응답, DB 스키마·테이블 소유자, 오류 의미, 트랜잭션·이벤트 순서는 유지한다. 이번 결정은 코드 의존성만 정리하며 Gradle 모듈, 별도 프로세스, DB 분리는 포함하지 않는다.

## 근거와 가정

- 현재 Java import 정적 조사에서 교차 기능 import 143건과 상호 참조하는 기능 패키지 쌍 8개를 확인했다. 이 수에는 허용 가능한 관리자/대시보드 조합 후보도 포함되어 있어 모두 위반이라는 뜻은 아니다.
- `application`에서 `infra`를 가져오는 저장소·provider 타입과 `domain`에서 타 경계 entity/infra를 가져오는 타입이 확인됐다.
- 현재 단일 Spring Boot 애플리케이션과 단일 MySQL을 유지한다는 기존 결정에 따른다.

## 결과와 위험

- 교차 기능 조회·명령은 DTO와 포트 뒤에 놓여 소유 데이터의 저장 구현을 호출자가 알 필요가 없어진다.
- 계약 수가 늘고 일부 읽기 조합의 호출 경로가 길어질 수 있다. 동일 트랜잭션이 필요한 경로는 application 조합자가 경계를 소유하고 통합 테스트로 원자성을 지킨다.
- 이관 중에는 기존 endpoint, persistence, error, scheduling, event order를 characterization/integration test로 고정한다. 테스트가 없는 구간은 우선 테스트를 추가한 뒤 이동한다.

## 구현 결과

- 도메인 내부 계층과 교차 도메인 import를 다시 검사해 금지된 계층 참조, presentation의 타 도메인 구현 참조, 기능 패키지 순환이 없는 것을 확인했다. `common.domain.RepositoryContract` 같은 공용 기반 타입 의존은 허용한다. `bootstrap`은 조립 지점으로 두며, 각 controller는 자기 도메인 presentation에 위치한다.
- 사용자 기기 기능은 Web Push endpoint 검증·무효 기기 정리를 계약으로 공개하고, 알림 infrastructure가 사용자 application 구현을 직접 참조하지 않도록 했다.
- 사용자 탈퇴 정리는 사용자 도메인이 소유한 outbound contract를 통해 호출한다. 알림·피드백·구독·세션 기능이 사용자 생명주기의 구현 상세를 역방향으로 참조하지 않는다. 알림 테스트 API의 로그인 사용자 결정과 피드백의 현재 사용자·관리자 결정도 각 application use case가 담당한다.
- review repository가 JPQL에서 `Course`를 직접 조인하던 경로를 제거했다. 강의 도메인의 `CourseReviewCatalog`가 과목코드·교수명에 해당하는 강의 키만 반환하고, review repository는 자기 리뷰 테이블에서 해당 키를 조회한다. 미지정 교수는 기존처럼 해당 강의 키 하나로 제한한다. 이모지 통합 테스트에서 이전 학기 외부교원 리뷰 묶음과 교수 분리 동작을 확인한다.
- 관리자 피드백 읽기 SQL은 `users`를 직접 조인하지 않는다. 피드백 projection은 내부 작성자 ID만 반환하고, 피드백 application이 사용자 도메인의 일괄 계정 상태 조회를 요청해 기존의 탈퇴·누락 사용자 마스킹을 유지한다. 작성자 ID는 API 응답에 노출하지 않는다.
- 강의 검색의 찜 필터·인기순 하위 쿼리를 `CourseJpaRepositoryImpl`에서 `readmodel.course.infra.CourseSearchReadRepository`로 옮겼다. 이 어댑터는 `QWishlist`를 명시적으로 참조해 강의와 찜 데이터를 읽기 전용으로 조합한다. 데이터 의존은 이 readmodel에서 각 소유 도메인 방향으로만 흐르고, 강의·찜 도메인은 이 어댑터를 참조하지 않는다. 동일 SQL 안에서 필터·정렬·페이징하는 결과는 유지했다.
- 관리자 피드백 삭제 필터도 contract 타입으로 옮겼다. 입력 문자열은 기존과 같이 presentation에서 파싱하므로 오류 처리 동작을 유지한다.
- DB migration 파일은 변경하지 않았다.

## 검증과 재검토 조건

- 최종 상태에서 정적 import 검증으로 위 계층 규칙을 확인하고, 업무 경계 의존 그래프에 cycle이 없음을 확인한다. 교차 도메인 읽기는 `readmodel` 어댑터에서만 허용한다.
- 서버 전체 `./gradlew test`와 정적 분석을 실행한다. 변경된 데이터 경로는 소유자별 저장·조회, 실패/롤백 결과를 기존 테스트와 비교한다.
- 별도 Spring/DB module로 분리하거나 배포 경계를 바꾸려는 근거가 생기면 재검토한다.

## 후속 검증 (2026-09-29)

- 교차 도메인 검색 쿼리를 readmodel 어댑터로 옮기고, QueryDSL이 같은 찜 필터·인기순 정렬·페이징 결과를 내는 기존 통합 테스트를 통과했다.
- 서버 전체 테스트 489건 통과.
- 380개 Java 파일 검사에서 금지된 계층 참조, presentation의 타 도메인 참조, 기능 패키지 순환이 없음을 확인했다. `readmodel.course`만 강의·위시리스트 도메인을 향하는 조회 조합 의존성을 가진다.
- DB migration 변경 없음.

## 순환 참조 정리 후속 (2026-09-29)

- 사용자 설정 도메인은 알림 채널 enum을 참조하지 않는다. `UserNotificationTestSender`는 사용자 ID만 받으며, 알림 도메인이 사용자 조회 계약의 기존 설정값을 `NotificationChannel`로 매핑한다. enum은 실제 발송·이력·provider 정책을 소유하는 알림 도메인에 남겼다.
- `CourseSearchQuery`는 domain 모델을 입력·결과로 사용하는 강의 application port이므로 `contract`에서 `application.port`로 이동했다. application port가 domain을 참조하는 방향만 남긴다.
- `GlobalExceptionHandler`는 HTTP 예외 변환 어댑터이므로 `common.error`에서 `common.web`로 이동했다. 오류 코드·예외 타입은 웹 어댑터를 참조하지 않는다.
- 현재 main source 380개 파일·97개 Java package와 제안된 논리 도메인 경계를 기준으로 다시 검사해 package SCC와 domain SCC가 없음을 확인했다. 계층 방향 검사도 위반 0건이다. 교차 도메인 엔티티 조회는 기존 결정대로 `readmodel.course`에서만 한다.
- 최종 `./gradlew test` 결과 490개 통과, 실패·오류·skip 0개. API 응답과 DB migration은 변경하지 않았다.
