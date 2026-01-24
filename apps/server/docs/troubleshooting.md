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
