# 보안 진단 결과 (Security Findings)

## 1. 운영 시크릿 하드코딩 유출 취약점 (SEC-001)
* **심각도**: **CRITICAL**
* **위치**: [application-prod.yml](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/resources/application-prod.yml#L44-L103)
* **상세**: 운영 설정에 평문 텍스트로 보안 토큰들이 박혀 있습니다.
  * Google OAuth2 Client Secret 노출
  * AWS SES 이메일 전송용 credentials (Access Key ID / Secret Key) 노출
  * JWT 토큰 검증용 대칭키 (`jwt.secret`) 노출
  * Discord Bot Token 및 Client Secret 노출
* **영향**: 형상관리(Git) 저장소 접근 권한이 있는 모든 주체에게 탈취되어 2차 악용이 가능하고 인프라 비용 폭탄 및 사칭 범죄가 유발될 수 있습니다.
* **조치 제안**: 평문 토큰을 즉시 무효화하고 재생성하십시오. 설정값은 Spring Boot 프로퍼티 플레이스홀더를 사용하여 환경변수로 이관되어야 합니다.
  ```yaml
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  ```

## 2. 세션 하이재킹을 통한 JWT 획득 취약점 (SEC-002)
* **심각도**: **HIGH**
* **위치**: [JwtAuthenticationFilter.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/common/security/jwt/JwtAuthenticationFilter.java#L32-L38)
* **상세**: 헤더 토큰 확인 실패 시 세션 스토어(`ACCESS_TOKEN` 속성)에서 꺼내 인증하는 구조입니다.
* **영향**: 세션 식별자가 탈취될 경우, 그 세션 내부에 보관된 영구적인 JWT Access Token마저 연쇄 유출되는 보안 결함이 발생합니다.
* **조치 제안**: 세션 스토어에 토큰을 격리하여 저장하는 로직을 완전히 삭제하고, 오직 무상태 인증 메커니즘만 작동하도록 보정하십시오.
