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

## `@Async` 로직이 테스트 코드와 동일한 스레드에서 실행되도록 강제하여 `Thread.sleep()` 없이도 100% 신뢰할 수 있는 통합 테스트를 구현했습니다.

## 4. 민감한 외부 API URL의 안전한 관리

### 문제 상황

학교(JBNU)의 실제 API URL이 소스 코드나 `application.properties`에 하드코딩되어 노출될 경우, 보안상 취약점이 발생할 수 있고 외부 공개 시 문제가 될 소지가 있었습니다.

### 해결책

**환경 변수(Environment Variables)**를 활용하여 민감한 정보를 소스 코드와 완전히 분리했습니다.

- **방법**: `application.properties`에는 `${JBNU_API_URL}`과 같은 플레이스홀더만 남기고, 실제 값은 `.env` 파일을 통해 주입받습니다.
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

## 개발 과정에서 빈번하게 실행하는 기본 테스트 속도를 3배 이상 개선하여 개발 생산성을 확보했습니다.

## 6. DB 초기화 시 세션 불일치 및 401(Unauthorized) 처리

### 문제 상황

Docker 환경에서 DB만 초기화된 경우, 브라우저에는 유효한 JWT(세션)가 남아있으나 서버 DB에는 해당 유저가 존재하지 않는 상태가 발생했습니다. 이 경우 기존 로직은 `USER_NOT_FOUND` (404)를 반환했으나, 프론트엔드에서는 이를 단순 데이터 부재로 판단하여 세션을 유지하려 시도하고, 이후 모든 요청이 실패하는 현상이 발생했습니다.

### 해결책

세션 정보(Email/JWT)는 존재하지만 DB에 유저가 없는 경우를 명확한 **인증 실패(401 Unauthorized)** 상태로 정의하고, 에러 코드를 통일했습니다.

- **변경 내역**: `AuthService.reissue`, `SubscriptionService.getCurrentUser` 등에서 유저 미발견 시 `ErrorCode.USER_UNAUTHORIZED` (401)를 던지도록 전면 수정.
- **프론트엔드 연동**: Axios 인터셉터가 401 에러를 감지하면 즉시 로컬 세션을 정리(`logout()`)하고 로그인 페이지로 리다이렉트 시키도록 연동.

### 결과

DB 초기화나 세션 만료 시 사용자가 의도치 않게 깨진 상태(Broken State)에 머무는 것을 방지하고, 자연스러운 재로그인을 유도하여 UX 안정성을 확보했습니다.
