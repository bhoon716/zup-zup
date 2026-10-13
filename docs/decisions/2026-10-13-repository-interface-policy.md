# 결정: 중복된 저장소 계약과 Spring Data 인터페이스를 통합한다

- 상태: Accepted
- 결정일: 2026-10-13
- 범위: 같은 기능의 저장소 계약과 Spring Data JPA 인터페이스 분리 구조
- 관련 자료: [WishlistRepository](../../apps/server/src/main/java/bhoon/sugang_helper/planning/wishlist/domain/WishlistRepository.java), [WishlistJpaRepository](../../apps/server/src/main/java/bhoon/sugang_helper/planning/wishlist/infra/WishlistJpaRepository.java), [CourseRepository](../../apps/server/src/main/java/bhoon/sugang_helper/academic/course/domain/CourseRepository.java), [CourseJpaRepository](../../apps/server/src/main/java/bhoon/sugang_helper/academic/course/infra/CourseJpaRepository.java), [RepositoryContract](../../apps/server/src/main/java/bhoon/sugang_helper/common/domain/RepositoryContract.java)

## 문제와 확인한 사실

강의·찜 저장소는 서비스용 저장소 인터페이스와 이를 상속하는 Spring Data JPA 인터페이스를 분리하고, 일부 같은 메서드를 양쪽에 선언한다. JPA 인터페이스는 서비스용 계약과 `JpaRepository`를 함께 상속한다. 중간 객체가 호출을 재전달하는 구조는 아니며, 검토한 비용은 실행 성능보다 선언·쿼리 위치를 추적하고 관리하는 부담이다.

공통 `RepositoryContract`에는 Spring Data의 `Page`·`Pageable`과 저장·삭제·flush 등의 메서드가 있다. 현재 분리만으로 저장 기술에 대한 완전한 독립성을 확보했다고 간주하지 않는다.

## 비교와 결정

| 선택지 | 이점 | 비용 |
| --- | --- | --- |
| 하나의 Spring Data 저장소 인터페이스로 통합 | 선언 중복과 추적할 파일을 줄이고 메서드·쿼리를 함께 확인 | 서비스가 Spring Data에 직접 의존하고 제공 메서드가 넓게 노출됨 |
| 현재 분리 유지 | 서비스가 사용하는 저장소 계약을 제한하고 JPA 선언을 분리 | 계약과 실제 쿼리를 나누어 확인·관리 |

분리를 유지하면서 불필요한 반복 선언만 정리하는 보완도 설명했다. 사용자는 상세 비교 후 우선 첫 번째 방식으로 진행하도록 선택했다.

1. 같은 기능에서 서비스용 저장소 계약과 Spring Data 인터페이스가 중복된 구조는 하나의 Spring Data 저장소 인터페이스로 통합하는 방향을 적용한다. 서비스는 통합한 인터페이스를 사용한다.
2. 확인한 강의·찜 구조부터 적용 대상을 검토한다. 다른 저장소도 같은 책임 중복 여부를 확인하고 전환하며, 모든 인터페이스를 일괄 제거하는 규칙으로 확대하지 않는다.
3. 기존 쿼리·잠금·트랜잭션과 저장·삭제 동작을 유지하도록 전환한다. 인터페이스 통합을 이유로 DB 구조나 기존 기능 정책을 바꾸지 않는다.
4. QueryDSL 등 실제 조회 로직을 담당하는 클래스는 별도 책임을 보고 판단한다. 기능 간 조회 계약과 채널 발송 인터페이스도 이번 결정으로 제거하지 않는다.
5. 구체적인 이름·패키지 위치와 `RepositoryContract`의 남은 사용처 정리는 상세 설계에서 정한다.

## 근거와 수용한 비용

현재 코드를 읽고 수정할 때 저장소 메서드와 쿼리를 바로 확인하는 편의를 우선한다. 검토한 범위에서 저장 방식 교체나 엄격한 저장소 API 제한이 필요한 구체적인 요구는 확인하지 못했다. 서비스의 Spring Data 의존성과 넓은 저장소 API 노출 비용을 수용한다.

독립적인 저장 구현이 실제로 필요하거나 저장소 API 노출로 업무 규칙 우회가 반복되는 등 문제가 확인되면 필요한 범위의 계약 분리를 재검토한다.

## 기록과 구현 상태

의사결정만 기록했다. 실제 저장소·서비스 코드나 DB를 변경하지 않았으며 테스트를 추가하거나 실행하지 않았다.

## 변경 결정: 클린 아키텍처에 맞춰 저장소 계약과 영속성 구현을 분리한다

- 상태: Accepted
- 결정일: 2026-10-13
- 대체 범위: 위의 서비스가 Spring Data 저장소 인터페이스를 직접 사용하도록 통합하는 선택
- 기준: [클린 아키텍처 적용 결정](2026-10-13-clean-architecture-policy.md)

사용자는 클린 아키텍처를 사용하고 이에 어긋나는 기존 결정을 수정하도록 요청했다. 위 통합 결정은 이력으로 보존하고, 같은 범위에서는 이번 변경 결정을 우선 적용한다.

1. 업무 서비스는 안쪽에 정의한 순수 저장소 계약을 사용한다. 계약은 `JpaRepository`를 상속하지 않고 Spring Data의 `Page`·`Pageable`·`Slice`나 JPA 타입을 노출하지 않는다.
2. 바깥의 영속성 어댑터가 계약을 구현하고 Spring Data 저장소를 호출한다. 업무에 필요한 메서드만 계약에 두며 현행 `RepositoryContract`도 이 기준으로 정리한다.
3. 순수 도메인 모델과 JPA 영속성 모델을 구분하고 어댑터가 변환한다. 조회 전용 경로는 안쪽의 순수 조회 DTO로 직접 매핑할 수 있다.
4. 기존 쿼리·잠금·DB 제약·원자적 저장·트랜잭션 의미를 보존한다. 저장소 분리가 DB 트랜잭션 분리를 뜻하지 않는다.
5. 구현이 하나여도 외부 영속성 의존을 안쪽에서 차단하는 계약에는 역할이 있다. 중복 선언 정리는 이 경계를 지키는 범위에서 수행한다.

계약·어댑터·모델 변환과 연결 코드가 늘어나는 비용을 수용한다. 이번 변경은 의사결정 정정이며 실제 저장소·서비스 코드와 DB는 변경하지 않았고 테스트를 실행하지 않았다.
