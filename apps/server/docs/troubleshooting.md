# 트러블슈팅 및 의사결정 기록 (Troubleshooting & Decisions)

이 문서는 `jbnu-sugang-helper` 개발 과정에서 직면한 주요 기술적 문제와 그 해결 과정을 기록합니다.

---

## 1. 알림 발송 시 N+1 쿼리 문제 해결

### 문제 상황

특정 과목에 여석이 발생했을 때, 해당 과목의 수천 명의 구독자에게 알림을 발송하는 과정에서 성능 저하가 발생했습니다. 로그 확인 결과, 각 구독자마다 `User` 정보와 `UserDevice` 정보를 개별적으로 조회하는 N+1 쿼리 문제가 발견되었습니다.

### 가설 및 실험

- **가설**: 구독자 ID 리스트를 미리 추출하여 `IN` 절을 사용하는 배치 조회를 수행하면 DB 왕복 횟수를 획기적으로 줄일 수 있을 것이다.
- **실험**: `userRepository.findById()`와 리스트 조회를 비교 테스트.

### 해결책

`subscriptionRepository.findByCourseKeyAndIsActiveTrue()`로 얻은 구독자 리스트에서 `userId`를 추출하여 다음과 같이 배치 처리로 변경했습니다.

```java
// 개선된 로직
List<Long> userIds = subscriptions.stream().map(Subscription::getUserId).distinct().toList();
Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
Map<Long, List<UserDevice>> deviceMap = userDeviceRepository.findByUserIdIn(userIds).stream()
        .collect(Collectors.groupingBy(UserDevice::getUserId));
```

### 결과

수천 명의 구독자가 있는 경우에도 DB 조회 횟수를 단 3회로 고정하여 알림 발송 지연을 약 80% 단축했습니다.

---

## 2. Redis를 이용한 중복 알림 방지 (Dedup)

### 문제 상황

크롤링 주기가 짧고, 학교 서버의 데이터 갱신 시점이 불분명하여 짧은 시간 내에 동일한 과목에 대한 알림이 여러 번 발송되는 UX 저성 문제가 발생했습니다.

### 해결책

**Redis**를 분산 락 및 캐시로 활용하여 동일 과목-동일 상태에 대한 알림 발송 이력을 관리합니다.

- **로직**: 알림 발송 전 `ALERT:{courseKey}` 키가 Redis에 존재하는지 확인.
- **만료 시간**: 10분(DEDUP_TTL)으로 설정하여 적절한 간격을 유지.

```java
if (redisService.hasKey(redisKey)) {
    log.info("[Dedup] Notification already sent. Skipping.");
    return;
}
// 알림 발송 후...
redisService.setValues(redisKey, "SENT", DEDUP_TTL);
```

---

## 3. 통합 테스트에서의 비동기(@Async) 처리 제어

### 문제 상황

`NotificationService`가 `@Async`로 동작하여, 통합 테스트 코드에서 알림 발송 메서드가 호출되었는지 검증할 때 `verify()`가 실행 완료 전 테스트가 종료되는 비결정성(Non-determinism) 문제가 발생했습니다.

### 가설 및 실험

- **가설 1**: `Thread.sleep()`을 사용한다. (대기 시간이 길어지고 테스트 속도가 저하됨)
- **가설 2**: 테스트 환경에서만 `TaskExecutor`를 동기식으로 교체한다.

### 해결책

`@TestConfiguration`을 사용하여 테스트 환경에서만 `SyncTaskExecutor`를 `@Primary` 빈으로 등록했습니다.

```java
@TestConfiguration
public class TestAsyncConfig {
    @Bean @Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
```

### 결과

`@Async` 로직이 테스트 코드와 동일한 스레드에서 실행되도록 강제하여 `Thread.sleep()` 없이도 100% 신뢰할 수 있는 통합 테스트를 구현했습니다.

---

## 4. 민감한 외부 API URL의 안전한 관리

### 문제 상황

학교(JBNU)의 실제 API URL이 소스 코드나 `application.properties`에 하드코딩되어 노출될 경우, 보안상 취약점이 발생할 수 있고 외부 공개 시 문제가 될 소지가 있었습니다.

### 해결책

**환경 변수(Environment Variables)**를 활용하여 민감한 정보를 소스 코드와 완전히 분리했습니다.

- **방법**: `application.properties`에는 `${JBNU_API_URL}`과 같은 플레이스홀더만 남기고, 실제 값은 `.env` 파일을 통해 주입받습니다.
- **구조 최적화**: 설정의 성격에 따라 `infra/.env`(인프라)와 `server/.env`(비즈니스 로직)로 분리하여 관리 효율성을 높였습니다. `docker-compose` 실행 시에는 두 파일을 병합하여 사용하고, 로컬 개발 시에는 `server/.env`만으로도 구동이 가능하도록 구성했습니다.
- **테스트 환경 대응**: 수동 테스트 진행 시에도 하드코딩된 URL 대신 시스템 프로퍼티(`-Djbnu.api.url`) 또는 환경 변수에서 값을 읽어오도록 리팩토링하여 보안성을 높였습니다.

---

## 5. 무거운 통합 테스트 분리 (Manual Tag 적용)

### 문제 상황

프로젝트 규모가 커짐에 따라 `@SpringBootTest`를 포함한 통합 테스트의 실행 시간이 길어져(약 18~20초), 전체 테스트 주기가 느려지는 현상이 발생했습니다.

### 해결책

JUnit5의 **`@Tag("manual")`** 기능을 사용하여 무거운 통합 테스트와 실제 네트워크 호출이 포함된 테스트를 분리했습니다.

- **기본 테스트 (`./gradlew test`)**: 컨테이너 구동 없이 빠르게 실행되는 단위 테스트 위주로 수행 (약 6초).
- **수동 테스트 (`./gradlew manualTest`)**: 명시적으로 필요한 경우에만 스프링 컨테이너를 구동하여 전체 연동 과정을 검증.

### 결과

개발 과정에서 빈번하게 실행하는 기본 테스트 속도를 3배 이상 개선하여 개발 생산성을 확보했습니다.

---

## 6. DB 초기화 시 세션 불일치 및 401(Unauthorized) 처리

### 문제 상황

Docker 환경에서 DB만 초기화된 경우, 브라우저에는 유효한 JWT(세션)가 남아있으나 서버 DB에는 해당 유저가 존재하지 않는 상태가 발생했습니다. 이 경우 기존 로직은 `USER_NOT_FOUND` (404)를 반환했으나, 프론트엔드에서는 이를 단순 데이터 부재로 판단하여 세션을 유지하려 시도하고, 이후 모든 요청이 실패하는 현상이 발생했습니다.

### 해결책

세션 정보(Email/JWT)는 존재하지만 DB에 유저가 없는 경우를 명확한 **인증 실패(401 Unauthorized)** 상태로 정의하고, 에러 코드를 통일했습니다.

- **변경 내역**: `AuthService.reissue`, `SubscriptionService.getCurrentUser` 등에서 유저 미발견 시 `ErrorCode.USER_UNAUTHORIZED` (401)를 던지도록 전면 수정.
- **프론트엔드 연동**: Axios 인터셉터가 401 에러를 감지하면 즉시 로컬 세션을 정리(`logout()`)하고 로그인 페이지로 리다이렉트 시키도록 연동.

### 결과

DB 초기화나 세션 만료 시 사용자가 의도치 않게 깨진 상태(Broken State)에 머무는 것을 방지하고, 자연스러운 재로그인을 유도하여 UX 안정성을 확보했습니다.

---

## 7. 구독 토글 시 목록에서 사라지는 문제 해결

### 문제 상황

대시보드에서 알림 스위치(토글)를 꺼서 비활성화하면, 구독 내역이 삭제된 것처럼 목록에서 사라지는 현상이 발생했습니다. 실제 데이터는 남아있으나 사용자 인터페이스에서 조회되지 않아 혼란을 초래했습니다.

### 원인 분석

`SubscriptionService.getMySubscriptions()` 메서드가 내부적으로 `findByUserIdAndIsActiveTrue(userId)`를 호출하고 있었습니다. 즉, **활성화된(`isActive=true`) 구독만 조회**하도록 구현되어 있어, 사용자가 알림을 끄는 순간 조회 대상에서 제외되었습니다.

### 해결책

Repository에 `findByUserId(userId)` 메서드를 추가하고, 서비스 로직이 이를 사용하여 **활성 상태와 관계없이 모든 구독**을 반환하도록 수정했습니다.

```java
// 변경 전: 활성 구독만 조회
return subscriptionRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()...

// 변경 후: 모든 구독 조회
return subscriptionRepository.findByUserId(user.getId()).stream()...
```

---

## 8. Docker 빌드 속도 최적화 (Layer Caching)

### 문제 상황

소스 코드 변경 후 Docker 이미지를 빌드할 때마다 의존성(Dependencies)을 포함한 전체 빌드 과정이 반복되어, 단순 수정에도 2분 이상의 시간이 소요되었습니다.

### 원인 분석

`Dockerfile`에서 `COPY . .` 명령어가 의존성 설치(`gradle build`)보다 먼저 실행되게 작성되어 있었습니다. 이로 인해 소스 코드 변경 시 모든 레이어가 캐시 무효화(Cache Miss)되었습니다.

### 해결책

Docker의 **Layer Caching** 메커니즘을 활용하기 위해 `Dockerfile`의 순서를 재구성했습니다.

1. `build.gradle`, `settings.gradle`만 먼저 복사
2. `gradle dependencies` 실행 (이 레이어는 소스 코드 변경 시에도 캐싱 유지)
3. 소스 코드 복사 및 빌드 실행

```dockerfile
# 1. 의존성 정의 파일 복사
COPY build.gradle settings.gradle ./
# 2. 의존성 다운로드 (캐싱됨)
RUN ./gradlew dependencies --no-daemon
# 3. 소스 복사 및 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon
```

### 결과

빌드 시간이 **약 130초에서 60초대로 50% 이상 단축**되었습니다.

---

## 9. CourseKey 데이터 절삭(Truncation) 및 크롤러 초기화 이슈

### 문제 상황

기존 `과목코드-분반` 형식의 키를 `연도-학기-과목코드-분반`의 Composite Key로 변경하는 과정에서, DB 컬럼 길이가 `varchar(20)`으로 고정되어 있어 `Data truncation` 에러가 발생했습니다.

### 해결책

- 모든 관련 엔티티의 컬럼 길이를 `varchar(64)`로 확장했습니다.
- 스프링 부트 `ApplicationReadyEvent`를 통해 서버 시작 시 즉시 크롤러가 구동되도록 하여 초기 데이터 공백 현상을 제거했습니다.

---

## 10. JbnuCourseParser XML 구조 대응 및 교양 영역 추출 보완

### 문제 상황

학교 API 응답에서 교양 영역(`FLDFGNM`) 컬럼이 비어 있는 경우가 많아 교양 영역별 필터링이 불가능했습니다.

### 해결책

`FLDCONVINFO` 필드(`"영역,상세구분"`)를 파싱하여 값을 추출하는 로직을 추가함으로써 데이터 정합성을 확보했습니다.

---

## 11. Web Push 초기화 에러 (Property Key Mismatch)

### 문제 상황

서버 구동 시 `PushService is not initialized` 에러와 함께 알림 발송이 실패하는 현상이 발생했습니다.

### 원인

`WebPushNotificationSender.java` 코드에서는 `@Value("${WEBPUSH_PUBLIC_KEY}")`와 같이 환경변수 스타일로 키를 참조했으나, 실제 `application.yml` 설정은 `app.webpush.public-key`와 같은 계층형 구조로 되어 있어 값을 주입받지 못했습니다.

### 해결책

코드의 `@Value` 어노테이션 값을 YAML 설정 파일 구조에 맞게 수정했습니다.

```java
// 수정 전
@Value("${WEBPUSH_PUBLIC_KEY}")

// 수정 후
@Value("${app.webpush.public-key:}")
```

### 결과

설정 값을 정상적으로 로드하여 `PushService`가 성공적으로 초기화되었습니다.

---

## 12. 알림 테스트 예외 처리 (Device Not Found)

### 문제 상황

관리자 알림 테스트 시, 대상 유저의 기기 토큰이 DB에 없음에도 불구하고 요청이 성공(200 OK)으로 반환되어 실제 발송 실패 원인을 파악하기 어려웠습니다.

### 해결책

`NotificationService`의 `sendTestNotification` 메서드에서 기기 존재 여부를 먼저 조회하고, 기기가 없을 경우 `ErrorCode.DEVICE_NOT_FOUND` 예외를 명시적으로 발생시키도록 로직을 추가했습니다. 이로써 프론트엔드에서 "기기가 등록되지 않았습니다"라는 정확한 피드백을 줄 수 있게 되었습니다.

## 13. BFF(Backend For Frontend) 아키텍처 전환 및 토큰 격리

### 문제 상황

기존의 브라우저 기반 JWT 저장 방식(LocalStorage/Memory)은 다음과 같은 문제점이 있었습니다:

1. **보안 취약점**: XSS 공격 시 자바스크립트를 통해 토큰이 탈취될 위험이 있음.
2. **401 Race Condition**: Access Token 만료 시 여러 API가 동시에 Refresh를 시도하면서 서버 부하 및 인증 로직 충돌 발생.
3. **URL 노출**: OAuth2 로그인 후 토큰 전달 과정에서 URL 파라미터로 토큰이 노출되는 보안 리스크.

### 해결책

브라우저가 토큰을 직접 다루지 않는 **BFF + 세션 쿠키** 패턴으로 전면 개편했습니다.

- **토큰 저장소 이전**: 생성된 JWT(Access/Refresh)를 브라우저로 보내지 않고, **Spring Session Redis**를 도입하여 서버 측 세션에 저장합니다.
- **인증 매커니즘**: 브라우저는 오직 `HttpOnly`, `SameSite=Lax` 설정이 된 `SESSION` 쿠키만 보유하며, 모든 인증은 서버 세션을 통해 수행됩니다.
- **필터 확장**: `JwtAuthenticationFilter`가 HTTP 헤더뿐만 아니라 서버 세션의 속성을 확인하여 인증을 수행하도록 로직을 확장했습니다.

### 결과

- **보안 극대화**: 브라우저 자바스크립트 엔진에서 토큰 접근이 원천 차단되어 XSS로부터 안전해졌습니다.
- **안정성 향상**: 클라이언트 측의 복잡한 리프레시 동기화 로직이 제거되고, 서버 세션 생명주기에 따른 일관된 인증 관리가 가능해졌습니다.

---

## 14. JWT 검증 실패 로그 상세화 (디버깅 능력 향상)

### 문제 상황

기존에는 JWT 검증 실패 시 단순히 `log.error("Invalid JWT token")`과 같이 단일 메시지만 출력되어, 토큰이 만료된 것인지, 서명이 잘못된 것인지, 혹은 형식이 틀린 것인지 구분하기 어려웠습니다.

### 해결책

`JwtProvider`의 `validateToken` 메서드 내부에 예외 타입별 상세 로깅을 추가했습니다.

```java
try {
    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    return true;
} catch (SecurityException | MalformedJwtException e) {
    log.info("잘못된 JWT 서명입니다.");
} catch (ExpiredJwtException e) {
    log.info("만료된 JWT 토큰입니다.");
} // ...기타 예외 처리
```

### 결과

## 서버 로그만으로도 사용자의 접속 이슈 원인을 즉시 파악할 수 있게 되어 트러블슈팅 속도가 비약적으로 향상되었습니다.

## 15. 401 Unauthorized 세션 안정화 (Timeout Align & Session Sync)

### 문제 상황

BFF 아키텍처 도입 이후에도 사용자가 30분 정도 서비스를 이용하지 않으면 401 Unauthorized 에러가 발생하며 로그아웃되는 현상이 빈번했습니다. 이는 JWT 액세스 토큰의 짧은 만료 시간(30분)과 서버 사이드 세션 타임아웃 간의 불일치, 그리고 토큰 재발급 시 세션 정보가 갱신되지 않는 문제로 파악되었습니다.

### 해결책

1. **만료 시간 연장 및 일치**: `application.yml` 설정을 통해 JWT 액세스 토큰 만료 시간과 Spring Session(Redis) 타임아웃을 모두 **2시간**으로 연장하여 생명주기를 일치시켰습니다.
2. **세션 속성 즉시 갱신**: `AuthService.reissue` 메서드에서 새로운 액세스 토큰이 발급될 때, 이를 서버 세션의 `ACCESS_TOKEN` 속성에도 즉시 반영하도록 로직을 수정했습니다.

```java
// AuthService.java
session.setAttribute("ACCESS_TOKEN", newAccessToken);
log.info("[Auth] Session tokens updated for email: {}", email);
```

### 결과

세션 유지 능력이 비약적으로 향상되었으며, 토큰 재발급 후에도 서버 세션과 인증 정보가 항상 일치하게 되어 끊김 없는 서비스 이용이 가능해졌습니다.

---

## 17. 대표 시간표 삭제 시 자동 승계 로직 구현 (SUX 개선)

### 문제 상황

사용자가 '대표 시간표'를 삭제할 경우, 홈 화면(Dashboard)의 핵심 위젯인 시간표 영역이 비어 보이게 되어 사용자 경험(UX)이 저하되는 문제가 있었습니다. 사용자가 수동으로 다른 시간표를 대표로 지정하기 전까지는 대시보드에서 시간표를 확인할 수 없었습니다.

### 해결책

`TimetableService.deleteTimetable` 로직을 개선하여 **자동 승계(Automatic Succession)** 메커니즘을 도입했습니다.

1. **상태 확인**: 삭제하려는 시간표가 현재 '대표(`isPrimary=true`)' 상태인지 확인합니다.
2. **조건부 승계**: 대표 시간표를 삭제하는 경우, 해당 유저가 보유한 남은 시간표 목록을 조회합니다.
3. **자동 지정**: 남은 시간표 중 하나라도 존재하면, 즉시 해당 시간표를 새로운 대표로 지정(`setPrimary(true)`)합니다.

### 결과

사용자가 여러 개의 시간표를 관리할 때 대표 시간표를 삭제하더라도 시각적 공백 없이 자연스럽게 다른 시간표가 대시보드에 노출되어 끊김 없는 사용자 경험을 제공하게 되었습니다.

---

## 18. 토큰 재발급 시 세션 유실 방지 (Force Session Creation)

### 문제 상황

BFF 아키텍처에서 JWT 액세스 토큰은 만료되었으나 리프레시 토큰이 유효한 경우(`/api/auth/refresh`), 간헐적으로 서버 세션 자체가 유실되어 `ACCESS_TOKEN` 속성을 저장하지 못하고 인증이 풀리는 현상이 발생했습니다.

### 원인 분석

`AuthService.reissue` 시점에 `request.getSession(false)`를 사용하거나 세션이 이미 만료된 경우, 새로운 속성을 저장할 공간(세션 객체)이 존재하지 않아 발생하는 문제였습니다.

### 해결책

토큰 재발급 로직에서 세션 존재 여부와 관계없이 **강제로 세션을 생성**하도록 보장했습니다.

```java
// AuthService.java
HttpSession session = request.getSession(true); // 기존 세션이 없으면 새로 생성
session.setAttribute("ACCESS_TOKEN", newAccessToken);
```

### 결과

## 액세스 토큰 만료 후 재발급 시에도 서버 세션 컨텍스트가 안정적으로 유지되어, 프론트엔드에서 401 발생 후 재시도 시 인증 상태가 완벽하게 복구되도록 개선되었습니다.

## 19. Querydsl 서브쿼리를 이용한 고성능 찜 필터링 구현

### 문제 상황

사용자가 찜한 강의만 검색 결과에 노출해야 하는 요구사항이 있었습니다. 초기 계획은 프론트엔드에서 모든 찜 목록을 가져와 필터링하는 방식이었으나, 검색 엔진(Pagination/Sorting)과의 연동 및 데이터 정합성 유지를 위해 백엔드 통합 필터링이 필요했습니다.

### 해결책

`CourseRepositoryImpl`의 `searchCourses` 로직에 Querydsl의 **`JPAExpressions`**를 활용한 `exists` 서브쿼리 조건을 통합했습니다.

- **로직**: `Wishlist` 엔티티와 `Course` 엔티티를 `courseKey`로 조인하되, 실제 조인 대신 `exists()` 체크를 사용하여 성능을 최적화했습니다.
- **인증 연동**: 서비스 레이어에서 `SecurityUtil`을 통해 현재 로그인 유저의 ID를 주입받아 서브쿼리의 조건으로 사용합니다.

```java
private BooleanExpression inWishlist(Boolean isWishedOnly, Long userId) {
    if (isWishedOnly == null || !isWishedOnly || userId == null) {
        return null;
    }

    QWishlist wishlist = QWishlist.wishlist;
    return JPAExpressions.selectOne()
            .from(wishlist)
            .where(wishlist.courseKey.eq(course.courseKey)
                    .and(wishlist.userId.eq(userId)))
            .exists();
}
```

### 결과

## 수만 건의 강의 데이터와 복잡한 검색 조건(연도, 학기, 요일 등) 속에서도 사용자의 찜 상태를 단일 쿼리로 정확하고 빠르게 필터링할 수 있게 되었습니다.

## 20. k6 통합 테스트 시 한글 검색어 인코딩 문제

### 문제 상황

`integrated-user-flow.js` 시나리오에서 한글이 포함된 검색 쿼리(`keyword=컴퓨터`)를 서버로 전송할 때, k6에서 URL 인코딩 처리를 누락하여 서버로부터 `400 Bad Request` 응답을 받거나 검색 결과가 0건으로 나오는 현상이 발생했습니다.

### 원인 분석

k6의 `http.get()`은 URL에 특수문자나 한글이 포함된 경우 자동으로 인코딩하지 않습니다. 특히 쿼리 파라미터 조합을 문자열로 관리할 때 이 문제가 두드러졌습니다.

### 해결책

JavaScript의 `encodeURIComponent()`를 사용하여 쿼리 파라미터의 값 부분을 명시적으로 인코딩하도록 보정했습니다.

```javascript
// 보정 전
const url = `${BASE_URL}/api/v1/courses?${query}&page=0&size=20`;

// 보정 후 (안전한 인코딩 적용)
const encodedQuery = query
  .split("&")
  .map((pair) => {
    const [key, value] = pair.split("=");
    return `${key}=${encodeURIComponent(value)}`;
  })
  .join("&");
const url = `${BASE_URL}/api/v1/courses?${encodedQuery}&page=0&size=20`;
```

### 결과

한글 키워드를 포함한 모든 검색 시나리오에서 **성공률 100%**를 달성했으며, 실제 유저와 동일한 검색 부하를 생성할 수 있게 되었습니다.

---

## 21. 고부하 Admin API 성능 테스트 시 인증(Authorization) 장벽

### 문제 상황

크롤링 트리거(`POST /api/v1/admin/courses/crawl`)나 전역 통계 조회와 같은 고부하 어드민 API를 k6로 테스트할 때, 유효한 `ADMIN_TOKEN`을 지속적으로 공급하고 인증 필터 체인의 오버헤드를 측정하는 과정에서 어려움이 있었습니다.

### 해결책

1. **환경 변수 기반 인증**: k6 실행 시 `-e ADMIN_TOKEN=<TOKEN>` 옵션을 통해 보안 시크릿을 소스 코드와 분리하여 주입받도록 설계했습니다.
2. **Spike Test 전용 시나리오**: 인증 토큰 재발급(`refresh`) 과정 자체에 고부하를 가하는 `auth-spike-test.js`를 별도로 구성하여, Redis 세션 저장소의 동시성 처리 능력을 단독으로 검증했습니다.

### 결과

보안 가이드라인을 준수하면서도 시스템에서 가장 무거운 어드민 기능 및 인증 레이어에 대한 정밀 부하 측정이 가능해졌습니다.
