# Spring Modulith로 기능 모듈 경계를 검증한다

- 결정일: 2026-11-05
- 상태: Accepted — 사용자가 승인한 Spring Modulith 도입 범위
- 작업 브랜치: `feature/spring-modulith` (기존 dev에서 생성)

## 문제와 선택 기준

기능별 MVC 구조는 유지하지만 패키지 이름만으로는 순환 의존성과 다른 기능의 내부 접근을 막을 수 없다. 공통 인증 설정이 계정 기능에 의존하고, 강의 갱신이 알림의 outbox 서비스를 직접 호출하며, 계정 탈퇴가 구독·문의 서비스를 직접 호출하는 연결이 있었다. 감사 기능은 관리자 기능 안에 있으면서 구독·문의에서도 사용되어 역방향 의존성을 만들었다. 강의 검색은 찜 테이블을 조인하므로 단순한 서비스 의존성 검사만으로 실제 경계를 알 수 없었다.

기준은 기존 HTTP·DB·알림 정책 보존, 같은 DB 트랜잭션의 원자성, 읽기 쿼리와 페이지 정렬 보존, 예외 없는 자동 구조 검증, 이해하기 쉬운 MVC 구조다. 새 기능이나 성능 향상을 도입 근거로 주장하지 않는다.

## 결정

1. Spring Boot 3.5 계열과 호환되는 Spring Modulith 1.4.14 BOM을 고정한다. 생산 코드에는 모듈 API를 사용하고 구조 검사·문서 도구는 테스트 의존성으로 둔다. 기존 outbox가 내구성·재시도를 담당하므로 별도의 이벤트 저장소나 중복 발송 큐를 만들지 않는다.
2. academic·admin·audit·campus·common·dashboard·identity·planning·seatalert·support를 닫힌 모듈로 선언한다. `allowedDependencies`를 명시하고 필요한 개별 공개 타입만 named interface로 노출한다. 업무 엔티티·저장소는 외부에 공개하지 않는다. common의 추상 기반 엔티티는 공통 영속성 기반으로 공유한다.
3. 계정에 의존하는 보안 설정과 세션 인증 필터는 identity에 둔다. 기존 인증 규칙과 필터 동작은 유지한다. 공유 감사 기능은 독립 audit 모듈로 옮기고 URL·테이블·직렬화 형식은 유지한다.
4. 찜은 강의 검색의 기존 조인·정렬·필터와 함께 academic에 둔다. timetable은 planning에 유지한다. SQL과 데이터 소유권을 바꾸기 위한 포트·복제 모델·별도 조회를 추가하지 않는다.
5. 강의의 여석 발생 이벤트는 기존 강의 갱신 트랜잭션 안에서 동기 발행한다. seatalert의 기존 outbox 저장 메서드는 `@EventListener`와 `Propagation.MANDATORY`로 같은 트랜잭션에 참여한다. 저장 실패는 발행자로 전파하며 `@Async`, AFTER_COMMIT, REQUIRES_NEW로 분리하지 않는다. 기존 worker·재시도·DLQ·idempotency는 유지한다.
6. 탈퇴 사실은 identity 이벤트로 동기 전달하고 구독·문의는 자기 데이터를 같은 트랜잭션에서 처리한다. 계정의 테스트 알림 명령과 웹 푸시 endpoint 검증·만료 구독 관리는 작은 공개 계약으로 연결해 제공자 내부 구현에 대한 역방향 의존성을 없앤다. 기존 세션 폐기·외부 발송의 부수 효과 정책을 변경하지 않는다.
7. 구조 검사에서 순환·내부 접근·미선언 의존성을 필터링하거나 무시하지 않는다. 의도한 모듈 목록도 고정해 모듈이 검사 대상에서 빠지는 회귀를 막는다. 설계 문서는 동일한 모델에서 생성한다.

## 비교한 대안과 수용한 비용

- 전체 패키지를 open module로 선언하거나 위반을 무시하면 도입은 쉽지만 회귀 방지 목적을 달성하지 못한다.
- 업무 기능을 전부 한 모듈로 합치면 순환은 사라지지만 기능 경계가 사라진다. 실제 조인이 있는 찜·강의만 함께 둔다.
- 모든 호출을 비동기 이벤트로 바꾸면 outbox 기록과 강의 갱신의 원자성이 깨진다. 이벤트 저장소를 추가하는 방식도 기존 발송 큐와 책임이 겹치므로 채택하지 않는다.
- 독립 배포·별도 DB·전체 포트/어댑터 전환은 이번 범위가 아니다. 기존 기능별 MVC 결정을 유지한다.

타입의 공개 여부와 허용 의존성을 바꿀 때 선언과 구조 검사를 함께 수정해야 하는 비용을 수용한다. 동기 이벤트는 수신자의 실패가 발행자의 트랜잭션을 실패시키며, 이는 기존 직접 호출과 같은 일관성 선택이다. 기존 세션 폐기처럼 DB 밖의 부수 효과를 DB rollback으로 되돌릴 수 있다는 보장은 하지 않는다.

## 검증과 운영 범위

코드 변경마다 구조 검사와 단위 회귀를 실행하고 마지막에 격리된 E2E·빌드·스타일 검사 및 독립 리뷰를 수행한다. 저장소 지침에 따라 로컬 DB 통합 테스트는 실행하지 않는다. 운영 DB·설정·배포는 변경하지 않으며 새 커밋·푸시·PR은 사용자 확인 전 수행하지 않는다. 실제 실행 결과와 남은 한계는 완료 기록에서 구분한다.

## 공식 근거

- [Spring Boot 호환성](https://docs.spring.io/spring-modulith/reference/1.4/appendix.html)
- [모듈과 named interface](https://docs.spring.io/spring-modulith/reference/1.4/fundamentals.html)
- [구조 검증](https://docs.spring.io/spring-modulith/reference/1.4/verification.html)
- [동기 이벤트의 트랜잭션 의미](https://docs.spring.io/spring-modulith/reference/1.4/events.html)


## 후속 검증: 실제 루트 앱의 격리 HTTP E2E

- 결정일: 2026-11-05
- 상태: Accepted — 사용자의 단계별 검증 요청에 따른 최종 E2E 보강

정적 구조 검사·수동 adapter 단위 검증만으로는 루트 앱의 리스너 탐색과 실제 JPA 데이터 rollback을 실행 확인하지 못했다. 저장소의 단위·최종 E2E 지침에 따라 기존 DB 통합 테스트는 실행하지 않고, 실제 앱을 임의 loopback HTTP 포트에서 기동하는 최종 E2E를 추가했다. 일반 테스트의 중첩 fixture 유입을 피하도록 별도 소스 집합을 사용한다.

메모리 H2·격리 Redis만 허용하고 생산 설정을 읽지 않으며, 외부 수집·발송 transport와 worker는 mock으로 둔다. 대상 리스너는 수동 등록하지 않는다. 서비스의 직접 호출·수신자 실패·바깥 호출자 rollback을 구분한다. 실제 MySQL·Flyway·동시성·외부 제공자·DB와 Redis의 교차 자원 원자성은 이번 확인 범위가 아니다. 구체적 명령과 보장 범위는 [모듈 안내](../backend-modules.md)에 남긴다. 커밋·푸시·운영 변경은 하지 않는다.


## 변경 결정: 첨부 설계의 공개 패키지·좁은 계약·소유권에 맞춘다

- 결정일: 2026-11-05
- 상태: Accepted — 사용자의 첨부 설계대로 수정 요청

사용자가 6개 업무 + 2개 조합 + 2개 지원 모듈을 유지하고 기존 서비스를 공개 계약의 구현으로 남기는 설계를 승인했다. 앞선 TYPE NamedInterface 표시와 찜의 academic 배치를 이 결정으로 대체한다. 일반 API·DTO와 소유 모듈의 events 패키지를 실제 Named Interface로 선언하고 구현 클래스·업무 엔티티·저장소는 내부로 둔다.

찜은 planning으로 복원한다. 구독은 TrackedSemesterQuery만 사용하고 관리용 크롤러 API를 참조하지 않는다. 테스트 알림은 seatalert로 옮기되 두 기존 URL·인증·오류 코드·cooldown 정책을 유지한다. 인증 필터는 identity.auth.session에 둔다. 감사는 AuditRecorder를 동기 호출한다. SeatOpenedListener와 기존 MANDATORY outbox 저장을 분리하고 같은 트랜잭션을 유지한다. 새 이벤트 저장소·전면 비동기화·Gradle 멀티모듈은 도입하지 않는다.

첨부에 없던 academic의 Wishlist 직접 서브쿼리를 확인했다. 계획 소유권을 유지하면서 Querydsl 필터·정렬·페이지 처리를 바꾸지 않기 위해 academic.api.search의 좁은 CourseWishlistQuery를 planning이 구현한다. 일반 ID/DTO 조회와 달리 기술 표현식을 반환하는 명시적 예외다. 엔티티·Q 타입·임의 SQL은 공개하지 않는다. 대량 메모리 정렬, 테이블 복제/캐시, 문자열 SQL로 경계 우회, 찜 소유권 재변경을 피하기 위한 선택이다. 이 기술 결합과 실제 조회 검증 필요성을 수용한다.

구조·공개 타입 누출·단위 회귀 및 실제 앱의 격리 HTTP E2E로 검증한다. 저장소의 단위/최종 E2E 지침을 유지하고 @ApplicationModuleTest 및 기존 로컬 DB 통합 실행은 추가하지 않는다. 검증 범위와 실제 MySQL·외부 연동의 한계는 [현재 모듈 안내](../backend-modules.md)에 구분한다. 커밋은 사용자 허락 전 수행하지 않으며 PR은 만들지 않는다.
