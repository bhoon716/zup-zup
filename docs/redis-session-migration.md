# Redis 세션 단독 인증 전환

사용자가 2026-10-26에 기존 단순화 브랜치의 Redis 세션 단독 전환을 승인했다. 출발 코드는 `c7cc807c4`다. 로컬 코드·테스트·문서·커밋만 변경하며 운영 접속·설정 변경·DB 변경·배포·원격 push·병합은 하지 않는다.

## 최종 동작 계약

- Google OAuth로 사용자를 확인한 뒤 ID·email로 현재 DB의 미삭제 사용자를 다시 확인하고 세션에 직렬화 가능한 SessionPrincipal을 저장한다. 기존 세션 ID를 회전하여 로그인 전 ID로 인증을 이어가지 않는다. Discord 외부 OAuth와 연동 state 검사는 유지한다.
- Spring Session의 RedisIndexedSessionRepository를 사용한다. namespace는 `zup:sessions:v2`, 미사용 만료는 2,592,000초(30일)다. `@EnableRedisIndexedHttpSession`에 실제 interval을 명시하며 YAML 기본값만 바꿔 적용됐다고 가정하지 않는다.
- 유효한 인증 HTTP 요청은 활동으로 인정한다. Spring Session의 last-access 갱신과 같은 CookieSerializer/HttpSessionIdResolver를 통한 SESSION 쿠키 갱신으로 서버 TTL과 브라우저 만료를 함께 연장한다. 웹 푸시를 받는 것만으로 로그인 기간을 연장하지 않는다. 인증 API의 자동 조회도 HTTP 활동에는 포함하며 사람의 클릭 여부를 별도로 추적하지 않는다.
- 일반 로그아웃은 현재 세션만 무효화한다. 다른 기기·과목 구독·푸시 등록·채널 설정은 유지한다. 새로운 전체 로그아웃 UI/API는 이번 범위로 추가하지 않는다.
- 탈퇴는 principal index의 email 후보를 찾고 저장된 userId까지 일치하는 모든 세션을 폐기한다. 같은 이메일로 재가입한 다른 userId의 세션은 지우지 않는다. 매 요청의 현재 DB id/email/미삭제 검사로 삭제·경합·재가입의 오래된 권한을 차단한다. 현재 역할도 확인하므로 세션의 과거 ADMIN 권한만 신뢰하지 않는다.
- DB/Redis 장애는 기존 인증으로 계속 처리하거나 메모리 저장소로 대체하지 않는다. 일시 장애를 확정 로그아웃으로 바꾸지도 않으며 실패 응답으로 요청을 거절한다.

상태 유지는 Spring Session에 맡기며 요청마다 maxInactiveInterval을 다시 설정하지 않는다. Indexed repository가 로그아웃한 세션에 남기는 interval=0을 늦은 요청의 저장이 30일로 되돌려 세션을 부활시키지 않기 위해서다.

## API와 CSRF

| 경로 | 계약 |
| --- | --- |
| GET /api/auth/csrf | 공개 접근, `{ "token": "..." }`. 현재 세션의 masked CSRF 토큰이며 인증 수단은 아니다. |
| POST /api/auth/logout | 유효한 X-CSRF-TOKEN 필요. 현재 기기 세션 종료, 기존 200/빈 본문 유지. |
| POST /api/auth/refresh | 제거. 호환용 재발급이나 리다이렉트 경로를 만들지 않는다. |
| 보호 API | 유효한 SESSION 쿠키와 현재 사용자/권한 필요. Bearer만으로 인증하지 않는다. |
| POST /api/v1/courses/search | 읽기 전용 검색이므로 이 정확한 메서드·경로만 CSRF 예외. 일반 변경 API 전체를 예외로 하지 않는다. |

변경 요청은 Spring Security의 기본 HttpSessionCsrfTokenRepository와 Xor 처리로 검증한다. `/csrf` 응답은 repository의 raw token이 아닌 request attribute의 masked token을 사용한다. 로그인·로그아웃에서 토큰이 바뀌므로 웹은 새 세션에서 다시 준비한다. 잘못되거나 없는 CSRF는 403/A003, 일반 권한 거부는 기존 403/A002로 구분한다. CORS는 기존 명시 출처 제한을 유지하고 X-CSRF-TOKEN 헤더만 추가한다.

웹은 변경 요청 전에 CSRF를 한 번 준비해 공유하고 헤더로 전달한다. 안전한 조회에는 준비하지 않으며, CSRF·인증 오류 뒤 원래 변경 요청을 자동 재전송하지 않는다. 401은 기존 로그인 안내/guest 흐름으로 처리하고 일반 권한 403은 로그인 상태를 지우지 않는다. 토큰 준비 중 로그아웃·계정 변경이 일어나면 이전 변경 요청은 보내지 않는다. 일시적인 저장소 오류에서는 사용자 상태를 인증 완료로 새로 승격하지 않는다.

SESSION의 HttpOnly·SameSite·domain/path는 Boot의 CookieSerializer 설정을 재사용한다. 운영 프로필 템플릿은 TLS 종료 프록시 뒤에서도 Secure가 빠지지 않도록 `server.servlet.session.cookie.secure=true`를 명시한다. Compose 템플릿도 표준 `SERVER_SERVLET_SESSION_COOKIE_SECURE` 키를 사용하며 로컬 HTTP override만 false다. 실제 운영 환경은 변경하지 않았다. 공통 customizer는 max-age를 30일로 설정하고 활동 시 같은 serializer로 갱신한다. 운영 HTTPS·프록시의 실제 쿠키 플래그는 배포 전 확인해야 한다. 개발용 H2 console도 상태 변경 POST에는 CSRF가 적용되므로 기존 console HTML 양식은 토큰 전송 없이 차단될 수 있다. 운영 프로필의 H2 console 비활성화는 유지한다.

참고 자료는 [Spring Session Redis 구성](https://docs.spring.io/spring-session/reference/configuration/redis.html), [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)다. 실제 구현 API와 경계는 로컬 Spring Session 3.5.4·Security 6.5.7 소스를 기준으로 대조했다.

## 기존 로그인과 배포 전환

전환 뒤에는 **모든 사용자가 한 번 다시 로그인해야 한다.** 새 namespace는 예전 spring:session 세션을 읽지 않는다. 옛 JWT·refresh 쿠키·기존 세션 ID의 호환 해석이나 이관 계층은 만들지 않는다. 서버가 자체 JWT를 발급·검증·갱신하지 않으며 브라우저 자동 refresh도 제거한다.

기존 RT/BL/RTR/UA와 옛 세션 키를 운영에서 삭제하지 않았다. TTL에 따라 남아 있을 수 있으며 이 코드가 인증에 사용하지 않는다. 실제 Redis 키 정리·보안 설정 변경·복구 작업은 여기서 실행하지 않는다. 되돌리려면 이전 서버/웹 계약과 세션 namespace를 함께 고려해야 하며 새 세션을 옛 코드가 이해한다고 가정하지 않는다.

서버와 웹을 같은 계약으로 함께 전환해야 한다. 옛 웹은 refresh 호출/CSRF 미전달로 동작하지 않을 수 있다. 저장소 밖 Bearer 소비자는 확인할 수 없으며 존재한다면 세션·CSRF 방식으로 함께 전환해야 한다. k6의 기존 JWT pool과 AUTH_TOKEN은 더 이상 인증에 사용되지 않는다.

## 시크릿과 설정 준비

새 세션 서명키는 필요 없다. 운영의 기존 Redis 연결과 Google·Discord OAuth 설정은 유지한다. 실제 값은 조회하거나 변경하지 않는다.

| 분류 | 환경변수·설정키 |
| --- | --- |
| 유지 | 운영 REDIS_HOST/REDIS_PORT/REDIS_PASSWORD, GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET/GOOGLE_REDIRECT_URI, Discord 연동 설정 |
| 유지 | APP_CORS_ALLOWED_ORIGINS, APP_CORS_REQUIRE_HTTPS, APP_OAUTH2_SUCCESS_REDIRECT_URI/FAILURE_REDIRECT_URI/AUTHORIZED_REDIRECT_URI |
| 감사 보호용 | APP_ADMIN_AUDIT_FINGERPRINT_SECRET. 기존 JWT_SECRET은 이 키의 fallback으로도 사용되므로 지금 삭제하지 않는다. 새 JWT를 만드는 용도는 아니다. |
| 전환 후 제거 가능 | JWT_ACCESS_EXPIRATION/JWT_REFRESH_EXPIRATION, APP_AUTH_REFRESH_COOKIE_SECURE 등 옛 JWT 전용 설정. 실제 운영에서 제거하지 않았다. |
| 운영 전 확인 | 운영 템플릿의 server.servlet.session.cookie.secure=true 적용, SESSION의 HttpOnly/SameSite/domain/path, Redis indexed session의 keyspace 알림 및 초기화 권한 |

30일 정책과 namespace는 소스에서 명시한다. Redis 자체의 영속성·ACL·키스페이스 알림·백업 정책은 실제 운영에서 변경하지 않았다.

## k6와 검증 방법

k6는 SESSION_COOKIE와 CSRF_TOKEN 환경변수 또는 gitignored `apps/server/scripts/k6/data/sessions.json`의 테스트 세션 목록을 사용한다. 항목은 cookie·csrfToken·선택적 admin 필드다. cookie는 `SESSION=...` 형태의 완성된 쿠키 헤더이며 실제 값은 저장소·로그에 남기지 않는다. 운영 쿠키 대신 격리 환경의 테스트 계정으로 로그인하고 `/api/auth/csrf`에서 같은 세션의 토큰을 준비한다. header helper는 Cookie와 X-CSRF-TOKEN을 보내며 Bearer를 만들지 않는다.

검증은 다음 역할로 구분한다.

- 서버 단위/MockMvc: 세션 principal·현재 DB 사용자·역할·CSRF·인가·로그아웃·실패 처리.
- `./gradlew --offline sessionTest`: 실제 로컬 HTTP와 현재 Compose와 같은 Redis 6 계열의 일회용 컨테이너에서 직렬화·index·TTL·쿠키·다중 기기·장애 검증. Docker가 없으면 skip하지 않고 실패한다.
- 웹 단위/브라우저: CSRF 준비/공유·401·403·자동 재전송 없음·늦은 준비 응답 취소·기존 화면 기능.
- `node --experimental-vm-modules --test apps/server/scripts/k6/common-utils.test.cjs`: 외부 요청 없이 k6의 세션/CSRF 헤더 계약 검증.

실제 외부 OAuth 교환과 운영 Redis·TLS·프록시·자격 증명은 검증하지 않는다. 실행 결과와 남은 한계는 완료 시 추가한다.


## 배포 시 확인할 경계

웹과 API가 같은 site의 HTTPS 주소로 서비스된다는 현재 배치를 기준으로 SameSite=Lax를 유지한다. 서로 다른 site로 옮기면 쿠키·CORS·OAuth redirect를 함께 검토해야 하며 이번 작업에서 SameSite=None이나 넓은 domain으로 바꾸지 않았다. Redis indexed session은 principal index와 만료 이벤트를 사용하므로 keyspace 알림 초기화 권한을 확인한다. 공유 Redis의 ACL이나 CONFIG 값을 여기서 변경하지 않았다.

실제 HTTP/Redis E2E는 격리 Redis 컨테이너와 테스트 전용 HTTP 서버를 사용하고 DB 사용자 조회 경계만 가짜로 둔다. OAuth 성공 handler 이후를 실행하며 실제 Google·Discord 교환이나 운영 DB 쿼리·TLS 프록시를 검증한 것은 아니다. 마지막 Redis 장애 케이스는 해당 테스트가 만든 컨테이너만 중지한다.


## 최종 로컬 검증 결과

- `./gradlew --offline clean check bootJar sessionTest` 통과(1분 9초). 서버 122 suite·543건과 격리 Redis HTTP 7건, 실패·오류·skip 0. Checkstyle main/test·PMD main/test 위반 0, 실행 JAR 생성 완료.
- 최초 HTTP E2E는 Redis 7.4에서 통과했고, 현재 Compose와 같은 Redis 6 계열로 바꾼 뒤 `check bootJar sessionTest`도 통과했다(1분 10초). 실제 운영 Redis에 연결한 결과가 아니다.
- 이전 JWT 단계의 기본 서버 테스트 593건에서 543건으로 줄었다. 삭제한 JWT 서명·토큰 종류·refresh family/CAS·blacklist·보상 테스트는 제거된 구현의 계약이다. 인증·현재 DB 역할·이메일 재사용/다른 ID·만료·CSRF·기기별 로그아웃·탈퇴·실패 거부·저장소 장애 보장은 세션 단위/권한 테스트와 실제 HTTP E2E로 남겼다. 과목·일정·리뷰·outbox·첨부 기능 테스트를 줄이지 않았다.
- 전체 실행에서 실패한 2건은 전환 전 JWT 오류 문구와 Redis 옵션 테스트의 실제 세션 초기화였다. 새 A001 문구를 정확히 검증하고 옵션 단위 테스트가 인증 저장소를 시작하지 않도록 범위를 분리했다. 실제 Redis 초기화/네트워크/TTL은 HTTP E2E에서 별도로 실행했다. skip·실패 무시·보장 약화를 넣지 않았다.
- 웹 79파일·300건 통과, TypeScript·ESLint 통과, production build 통과. Playwright 7건은 재시도 없이 10.0초에 통과했다. 시간표 mutation fixture에 빠진 `/api/auth/csrf`를 추가하고 요청 헤더를 함께 검증했다. 브라우저 테스트는 가짜 API를 쓰므로 실제 서버/Redis E2E와 구분한다.
- k6 helper의 세션·CSRF·역할별 pool·환경변수/명시값·anonymous 계약 3건 통과. 실제 부하 요청은 보내지 않았다.
- 예제 파일만 사용하는 local/runtime Compose config 계약 검사 통과. 검사 스크립트의 `python` 이름이 없는 환경이어서 실행 쉘에만 python3 함수를 제공했다. 시스템이나 저장소의 Python 설정은 바꾸지 않았다. 이 검사는 Compose를 기동하거나 운영에 적용하지 않는다.
- Markdown 검사에서는 기존 HEAD와 현재가 모두 오래된 링크 159개로 같았고 이번 변경으로 새로 깨진 링크는 0개였다. append-only 과거 결정 문서를 이번 범위에서 재작성하지 않았다. HTML/컴포넌트 그림도 세션 기준으로 갱신하고 1440/390/320px·5탭·101개 소스 링크를 확인했다. 알림 그림 3개는 유지했다.

로그는 작업 디렉터리의 `session-server-final-fixed.log`, `session-server-redis6-final.log`, `session-web-full-final.log`, `session-web-types-final-root.log`, `session-web-lint-final-root.log`, `session-web-build.log`, `session-web-e2e.log`, `session-k6-contract.log`, `session-compose-local-final.log`, `session-compose-runtime-final.log`에 있다. 자격 증명 값은 출력하지 않았고 원본 dev 작업 폴더를 수정하지 않았다. 실제 OAuth 교환·운영 DB/Redis·TLS 프록시 쿠키와 저장소 밖 JWT 소비자는 여전히 미검증이다.

코드 전환 커밋은 `a6ab4b5abf9d9a773c443389f9173d20b3f7d9ac`다. HTML의 코드 기준도 이 커밋이며 구조 그림과 검증 메타데이터는 별도 문서 커밋으로 보존한다.


## 후속 심층 리뷰와 Blob 오류 복구

`878f16f9a` 기준 심층 리뷰 뒤 사용자 승인으로 첨부 다운로드의 JSON Blob A003 오류도 현재 세대의 CSRF 캐시를 폐기하도록 수정했다. 일반 403은 구분하며, 사용자의 다음 클릭에서 새 토큰을 준비하고 자동 재전송은 하지 않는다. 웹 311건·타입/lint/build·실제 두 탭을 포함한 Playwright 9건(retries 0)이 통과했다. 남은 시간 입력·세션 응답 경합·일정 수정/삭제 경합은 코드 수정 없이 [해결안 비교](deep-review-remediation-options.md)에 근거 수준·장단점·계약 영향과 함께 남겼다.
