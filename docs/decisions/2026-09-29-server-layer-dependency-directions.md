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

- 도메인 내부 계층과 교차 도메인 import를 검사해 위 규칙에 어긋나는 Java import가 남지 않은 것을 확인했다. 이 검사는 JPQL 문자열 의존성은 잡지 못한다. `bootstrap`은 조립 지점으로 두며, 각 controller는 자기 도메인 presentation에 위치한다.
- 사용자 기기 기능은 Web Push endpoint 검증·무효 기기 정리를 계약으로 공개하고, 알림 infrastructure가 사용자 application 구현을 직접 참조하지 않도록 했다.
- persistence 수준의 교차 도메인 의존은 남아 있다. `CourseJpaRepositoryImpl`은 희망 강의만 보기·인기순 계산을 위해 JPQL 문자열에서 `Wishlist`를 조회하고, review repository는 과목코드·교수명 기준 리뷰 묶음을 위해 `Course`를 조인한다. 계약 호출로 바꾸면 조회 수와 페이징·정렬 비용이 달라질 수 있어 다음 단계에서 쿼리 계획과 성능을 비교하며 소유권을 정한다.
- 관리자 피드백 삭제 필터도 contract 타입으로 옮겼다. 입력 문자열은 기존과 같이 presentation에서 파싱하므로 오류 처리 동작을 유지한다.
- 전체 서버 테스트 486건이 통과했고, DB migration 파일은 변경하지 않았다.

## 검증과 재검토 조건

- 최종 상태에서 정적 import 검증으로 위 계층 규칙을 확인하고, 업무 경계 의존 그래프에 cycle이 없음을 확인한다.
- 서버 전체 `./gradlew test`와 정적 분석을 실행한다. 변경된 데이터 경로는 소유자별 저장·조회, 실패/롤백 결과를 기존 테스트와 비교한다.
- 별도 Spring/DB module로 분리하거나 배포 경계를 바꾸려는 근거가 생기면 재검토한다.
