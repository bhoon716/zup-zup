# 아키텍처 진단 결과 (Architecture Findings)

## 1. 아키텍처 레이어 및 구조 분석
* 백엔드 프로젝트는 레이어드 아키텍처 패턴을 따르고 있습니다. `domain` 패키지가 도메인 모델을 온전히 소유하고, 비즈니스 규칙은 엔티티 및 VO 내부에서 강하게 응집됩니다.
* 프론트엔드는 Next.js 15+ App Router 구조 하에 FSD(Feature-Sliced Design) 지향식 설계로 기능별 컴포넌트, 훅, API 코드가 적절하게 모듈화되어 상호 격리되어 있습니다.

## 2. 식별된 아키텍처 왜곡 및 위반 사항 (Architecture Drift)

### 1) JWT 무상태성과 서블릿 세션의 오염 (ARC-001)
* **내용**: JWT 기반 인증 방식을 선택했음에도 불구하고, OAuth2 로그인 연동 성공 후 세션 객체(`HttpSession`)에 JWT Access/Refresh 토큰을 저장하고 있으며, Filter 단에서 헤더에 토큰이 부재하면 HTTP 세션에서 토큰을 추출하여 인증처리를 속행하고 있습니다.
* **영향**: 완벽한 무상태성(Stateless)을 전제로 구성된 REST API 구조가 상태 유지형 세션 구조와 얽히며 세션 고정 공격이나 세션 하이재킹 등 복합적 보안 취약점에 노출됩니다.
* **조치 제안**: HTTP Session을 완전 비활성화하고 프론트 클라이언트(Next.js)와 백엔드 API 서버는 순수 헤더(`Authorization: Bearer <token>`) 또는 HTTP-Only 보안 쿠키만을 이용하여 인증을 중개해야 합니다.

### 2) Soft-Delete 자동 필터링 수단 부재 (ARC-002)
* **내용**: 탈퇴 회원(`User`)이나 삭제된 리뷰(`CourseReview`) 처리를 위해 `deleted_at`을 활용한 Soft-delete 방식을 적용했으나, Spring Data JPA 기본 쿼리 메서드 호출 시 삭제된 엔티티가 여전히 정상 로드되는 아키텍처 동작 누락이 존재합니다.
* **영향**: 논리 삭제된 데이터가 정상 복원되어 서비스되는 데이터 오염 및 노출 정합성 리스크가 있습니다.
* **조치 제안**: `BaseEntity`를 구현하는 타깃 엔티티 클래스 상단에 `@SQLRestriction("deleted_at IS NULL")` 어노테이션을 설정하여 조회 쿼리 생성 시 기본적으로 필터링이 수행되도록 변경합니다.
