# 레포지토리 감사 보고서 (Repository Audit Report)

본 보고서는 아래 전문 리뷰어 에이전트의 정밀 진단 결과를 검증(Verifier)하여 취합한 최종 **Full Repository Audit** 종합 리포트입니다.

---

## 1. 종합 평가 및 점수판 (Repository Scorecard)

본 프로젝트는 Spring Boot(백엔드)와 Next.js(프론트엔드)로 구성되어 전북대학교 수강신청 빈자리 알림 및 시간표 조합 기능을 제공하고 있습니다. 레이어드 구조와 도메인 주도 설계(DDD) 지향적인 패키지 구조가 우수하며 양측 모두 높은 테스트 커버리지(Vitest & MockMvc)를 보유하고 있습니다.

그러나 **운영 환경용 프로파일(`application-prod.yml`)에 다수의 중요 API Key 및 JWT 서명 비밀키가 평문 하드코딩**되어 유출 상태이며, 세션과 JWT를 혼용하는 보안 허점 및 Soft-Delete 정합성 누락 등 긴급하게 수정해야 할 보안 및 아키텍처 리스크가 식별되었습니다.

### 부문별 점수 (10점 만점)

| 평가 항목 | 점수 (1-10) | 평가 근거 및 요약 |
| :--- | :---: | :--- |
| **아키텍처 (Architecture)** | 8/10 | 계층 분리가 명확하나, 무상태(Stateless) JWT 구조에 세션(Session)을 혼용하는 아키텍처 변형이 존재함. |
| **보안 (Security)** | 2/10 | AWS, Google, Discord, JWT, VAPID 등 운영 환경의 거의 모든 Secrets가 형상 관리 대상 설정 파일에 하드코딩 유출됨. |
| **성능 (Performance)** | 7/10 | QueryDSL 슬라이스 페이지네이션 처리가 우수하나, 다대다 조인 대상 및 대량 데이터 조회를 위한 복합 인덱스가 일부 누락됨. |
| **유지보수성 (Maintainability)** | 8/10 | 도메인 모델이 잘 캡슐화되어 있고 SRP를 대체로 잘 따르나, 일부 컨트롤러 및 알림 채널 결합도가 높음. |
| **확장성 (Scalability)** | 6/10 | 단일 서버 컨테이너화(`Standalone` Next.js 빌드 등)는 준비되었으나 배치 동기화 락(Lock) 등이 분산 구조용으로 설계되지 않음. |
| **안정성 (Reliability)** | 7/10 | 액추에이터 헬스체크 및 Loki, Promtail, Grafana 모니터링은 구축되어 있으나 컨테이너 리소스 명시적 한계값(Limits) 지정이 누락됨. |
| **테스트 가능성 (Testability)** | 9/10 | 백엔드 40개, 프론트엔드 46개 등 광범위한 유닛/통합 테스트를 구축하여 기능 테스트 안정성이 최상 수준임. |
| **가독성 (Readability)** | 9/10 | 깔끔하고 합의된 네이밍 컨벤션을 충실히 이행함. |
| **관측 가능성 (Observability)** | 8/10 | 인프라 내에 Prometheus, Grafana, Loki 등이 정밀 통합되어 있어 관측성이 양호함. |

---

## 2. 핵심 식별 이슈 대장 (Audit Ledger Summary)

| 이슈 ID | 부문 | 심각도 | 위치 | 상태 | 요약 설명 |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **SEC-001** | 보안 | **CRITICAL** | [application-prod.yml](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/resources/application-prod.yml#L44-L103) | **Open** | Google OAuth, AWS SES, Discord, JWT 서명 키 평문 하드코딩 노출 |
| **SEC-002** | 보안 | **HIGH** | [JwtAuthenticationFilter.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtAuthenticationFilter.java#L32) | **Open** | JWT 헤더 부재 시 HTTP 세션으로부터 토큰을 꺼내서 인증하는 세션 강제 사용 아키텍처 허점 |
| **DB-001** | 데이터베이스 | **MEDIUM** | [BaseEntity.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/common/audit/BaseEntity.java#L15) | **Open** | Soft-Delete 처리용 `deleted_at` 적용 엔티티의 자동 조회 필터링 누락 |
| **PERF-001** | 성능 | **MEDIUM** | [CourseReviewJpaRepository.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/review/infra/CourseReviewJpaRepository.java) | **Open** | 과목코드+교수명 동적 조인 쿼리용 복합 인덱스 누락 |
| **CONSIST-001**| 일관성 | **MEDIUM** | 컨트롤러 전반 | **Open** | 공통 응답 구조(`CommonResponse`)를 래핑하지 않는 날것의 엔드포인트 응답 혼재 |
| **PERF-003** | 성능 | **LOW** | [UserService.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/user/application/UserService.java#L211) | **Open** | 읽기 전용 영속성 컨텍스트(JPA) 튜닝 최적화 |
| **PERF-004** | 성능 | **MEDIUM** | 페이지네이션 컨트롤러 | **Open** | 대용량 페이지 조회 크기 상한선 강제 제약 부재로 OOM 위험 |
| **INFRA-001** | 인프라 | **LOW** | [docker-compose.yml](file:///Users/bhoon/Project/jbnu-sugang-helper/infra/docker-compose.yml#L114) | **Open** | Promtail 컨테이너가 호스트의 루트 `/var/log`를 그대로 마운트하여 권한 과다 매핑 |

