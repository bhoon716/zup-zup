# 감사 대장 (Audit Ledger)

본 대장은 발견된 모든 이슈의 라이프사이클과 조치 현황을 관리하는 지속 장부입니다.

| 이슈 ID | 카테고리 | 심각도 | 위치 | 상태 | 설명 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SEC-001** | Security | **CRITICAL** | [application-prod.yml](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/resources/application-prod.yml#L44-L103) | **Open** | 운영 설정 파일 내에 모든 연동 API 키와 JWT 비밀 서명키 하드코딩 노출. |
| **SEC-002** | Security | **HIGH** | [JwtAuthenticationFilter.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtAuthenticationFilter.java#L32-L38) | **Open** | 헤더에 JWT 토큰이 없으면 HttpSession에 담긴 JWT 토큰을 찾아 세션을 검증하는 흐름. 무상태성 위배 및 토큰 유출 경로 제공. |
| **DB-001** | Database | **MEDIUM** | [BaseEntity.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/common/audit/BaseEntity.java#L15) | **Open** | Soft-delete 필드인 `deleted_at`이 존재하나 JPA 기본 조회 시 자동으로 걸러지지 않고 조회가 이루어짐. |
| **PERF-001** | Performance | **MEDIUM** | [CourseReviewJpaRepository.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/review/infra/CourseReviewJpaRepository.java) | **Open** | 과목 리뷰 조회 시 다수 조인 대상이 되는 `courses(subject_code, professor)` 복합 인덱스 누락. |
| **CONSIST-001**| Consistency | **MEDIUM** | 컨트롤러 레이어 전반 | **Open** | API 응답 시 `ResponseEntity<CommonResponse<T>>` 형식을 사용하지 않는 엔드포인트가 혼합되어 있어 프론트 클라이언트 파싱 에러 유발. |
| **PERF-003** | Performance | **LOW** | [UserService.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/user/application/UserService.java#L211) | **Open** | 단순 조회 쿼리의 readOnly 최적화 적용 범위 점검 |
| **PERF-004** | Performance | **MEDIUM** | 페이지네이션 컨트롤러 | **Open** | 대용량 페이지 파라미터 조작 시 OOM 장애 유발 가능성 |
| **INFRA-001** | Infrastructure | **LOW** | [docker-compose.yml](file:///Users/bhoon/Project/jbnu-sugang-helper/infra/docker-compose.yml#L114) | **Open** | Promtail이 호스트의 루트 `/var/log` 전체를 볼륨 바인딩하여 호스트 정보 노출 리스크. |

