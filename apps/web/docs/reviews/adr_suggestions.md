# 아키텍처 결정 제안서 (Architecture Decision Suggestions)

## ADR-001: 설정 파일 내 중요 시크릿의 런타임 환경변수 주입 이관

### 상태 (Status)
제안됨 (Proposed)

### 컨텍스트 (Context)
* 현재 수강신청 헬퍼 프로젝트의 `application-prod.yml` 설정 파일에는 실제 운영망에서 사용하는 대외 인증 인프라용 비밀키와 데이터베이스 계정 패스워드들이 그대로 적혀 형상관리에 전파되어 있습니다.

### 의사결정 (Decision)
* 버전 제어 대상 설정 파일에서 모든 하드코딩된 평문 패스워드와 토큰 값을 제거합니다.
* 대신 스프링 프로퍼티 내에 런타임에 대체될 수 있는 환경변수 명칭을 선언합니다.
  ```yaml
  spring:
    security:
      oauth2:
        client:
          registration:
            google:
              client-secret: ${GOOGLE_CLIENT_SECRET}
  ```
* 운영 서버 환경에 배포 시 Docker Compose 배포 스크립트나 Kubernetes ConfigMap/Secrets를 활용하여 환경변수값을 주입하도록 인프라 구성을 바꿉니다.

### 결과 (Consequences)
* 깃허브 등 코드 보관 영역 유출 사고 시에도 내부 자산의 위험이 소멸됩니다.
* 코드의 재빌드 과정 없이 컨테이너 재실행만으로 운영 토큰을 즉시 로테이션할 수 있습니다.

---

## ADR-002: 스프링 인증 영역의 세션 격리 및 완전 무상태화 전환

### 상태 (Status)
제안됨 (Proposed)

### 컨텍스트 (Context)
* JWT 토큰 기법을 채택했음에도 일부 컨트롤러 및 소셜 로그인 프로세스에서 HTTP Session의 메모리에 `ACCESS_TOKEN`을 보관하고 서블릿 인증 필터에서 이를 우회 접근 검출 수단으로 상호 운용하고 있습니다.

### 의사결정 (Decision)
* `HttpSession`에 토큰 자산을 은닉하는 구조를 완전히 소멸시킵니다.
* 프론트엔드 API 요청자는 언제나 표준 규격 HTTP Authorization 헤더(`Bearer <Token>`) 또는 서버가 내려주는 Http-Only Secure 쿠키를 통해서만 자격 증명을 제공하도록 강제합니다.
* 스프링 시큐리티 세션 관리 정책을 `SessionCreationPolicy.STATELESS`로 명시 정렬합니다.

### 결과 (Consequences)
* 세션 하이재킹에 따른 이중 인증 탈취 우려를 근원적으로 차단할 수 있습니다.
* 세션 동기화 모듈이 무력화되어 서비스 단위를 손쉽게 이중화 및 다중 분산 배치할 수 있습니다.
