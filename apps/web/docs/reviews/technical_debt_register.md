# 기술 부채 대장 (Technical Debt Register)

본 대장은 향후 아키텍처 개선 및 코드 품질 향상을 위해 관리 및 상환해야 하는 기술 부채 목록입니다.

| 부채 ID | 타깃 컴포넌트 | 부채 내용 및 설명 | 발생 영향 | 상환 로드맵 |
| :--- | :--- | :--- | :--- | :--- |
| **DEBT-001** | Spring Rest | 공통 래핑 응답(`CommonResponse`)을 사용하지 않는 일부 예외적 엔드포인트 존재. | Next.js API 인터셉터에 예외 처리 코드가 점진적으로 증가함. | 모든 컨트롤러 메서드의 반환 형식을 `ResponseEntity<CommonResponse<T>>`로 전면 통일. |
| **DEBT-002** | Notifications | `NotificationService` 단일 클래스가 모든 발송 채널 구현(Discord, FCM, Mail)을 하드코딩으로 강결합 소유. | 신규 채널 연동 시 비즈니스 수정 영역이 거대해짐. | `NotificationSender` 전략 인터페이스를 정의하고 다형성 전략 패턴으로 분리. |
| **DEBT-003** | Spring Security | 무상태 인증 JWT를 쓰면서 HttpSession 스토어에 토큰을 저장 및 검색 처리하는 변칙 구조 혼재. | 구조의 일관성 저하 및 세션 유출 시 토큰 위협 노출. | 세션 저장 프로세스를 완전히 무효화하고 프론트엔드가 Bearer 헤더를 사용하도록 강제화. |
| **DEBT-004** | Soft Delete | `BaseEntity`를 활용한 소프트 딜리트 타깃 데이터가 `@SQLRestriction` 적용 누락으로 수동 조회 코드로 관리됨. | 휴먼 에러로 소프트 딜리트 데이터가 리스트에 유출될 리스크 잔존. | 엔티티 클래스 상단에 Hibernate 어노테이션 추가하여 글로벌 필터 적용. |
