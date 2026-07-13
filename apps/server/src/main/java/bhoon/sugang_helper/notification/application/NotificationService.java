package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.notification.domain.NotificationHistory;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.NotificationDeliveryIdempotencyKey;
import bhoon.sugang_helper.notification.infra.NotificationSender;
import bhoon.sugang_helper.notification.infra.NotificationTarget;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자에게 다양한 채널(Email, FCM, WebPush, Discord)을 통해 알림을 발송하는 통합 서비스입니다. 중복 발송 방지 및 알림 이력 관리 기능을 포함합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String NOTIFICATION_KEY_PREFIX = "ALERT:";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(1);
    private static final String USER_TEST_COOLDOWN_KEY_PREFIX = "ALERT:USER_TEST:";
    private static final Duration USER_TEST_COOLDOWN = Duration.ofSeconds(10);

    private final RedisService redisService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final List<NotificationSender> notificationSenders;
    private final NotificationChannelPolicy notificationChannelPolicy;

    /**
     * 빈자리 발생 이벤트를 처리하여 구독자들에게 알림을 발송합니다.
     */
    public void handleSeatOpenedEvent(SeatOpenedEvent event) {
        String redisKey = NOTIFICATION_KEY_PREFIX + event.courseKey();
        boolean acquired = redisService.setValuesIfAbsent(redisKey, "PENDING", DEDUP_TTL);
        if (!acquired) {
            log.debug("[Notification] Skipped duplicate sending. courseKey={}", event.courseKey());
            return;
        }

        notifySubscribers(event);
        redisService.setValues(redisKey, "SENT", DEDUP_TTL);
        log.info("[Notification] Completed sending seat opening notifications. courseKey={}", event.courseKey());
    }

    public void deliverSeatOpening(Long userId, NotificationChannel channel,
                                   bhoon.sugang_helper.notification.domain.SeatNotificationOutbox outbox) {
        deliverSeatOpening(userId, channel, outbox, null);
    }

    public void deliverSeatOpening(Long userId, NotificationChannel channel,
                                   bhoon.sugang_helper.notification.domain.SeatNotificationOutbox outbox,
                                   String idempotencyKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!notificationChannelPolicy.isChannelEnabled(user, channel)) {
            return;
        }
        List<NotificationTarget> targets = notificationChannelPolicy.resolveTargets(
                user, userDeviceRepository.findByUserId(userId), channel);
        if (targets.isEmpty()) {
            return;
        }
        NotificationSender sender = notificationSenders.stream()
                .filter(candidate -> candidate.supports(channel))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                        "알림 채널 발송기가 없습니다: " + channel));
        NotificationMessage message = createSeatOpenedMessage(new SeatOpenedEvent(
                outbox.getCourseKey(), outbox.getCourseName(), outbox.getProfessor(),
                outbox.getPreviousSeats(), outbox.getCurrentSeats()));
        String providerKey = NotificationDeliveryIdempotencyKey.providerKey(idempotencyKey);
        for (NotificationTarget target : targets) {
            sender.send(target, message.title(), message.body(), providerKey);
        }
        saveHistory(userId, outbox.getCourseKey(), message, channel);
    }

    /**
     * 특정 사용자에게 여러 채널로 테스트 알림을 발송합니다.
     */
    public void sendTestNotification(User user, List<NotificationChannel> channels) {
        NotificationMessage notification = createTestMessage();
        List<UserDevice> devices = userDeviceRepository.findByUserId(user.getId());

        for (NotificationChannel channel : channels) {
            log.info("[Notification Test] Attempting to send. channel={}, userId={}", channel, user.getId());
            sendNotification(user, devices, notification, channel, NotificationContext.forTest());
        }
    }

    /**
     * 사용자 단에서 요청한 테스트 알림을 발송하며, 쿨타임을 적용합니다.
     */
    public void sendUserTestNotification(User user, List<NotificationChannel> channels) {
        String cooldownKey = USER_TEST_COOLDOWN_KEY_PREFIX + user.getId();
        boolean acquired = redisService.setValuesIfAbsent(cooldownKey, "LOCK", USER_TEST_COOLDOWN);
        if (!acquired) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS, "알림 테스트는 10초에 한 번만 가능합니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            sendTestNotification(user, channels);
        } catch (RuntimeException e) {
            redisService.deleteValues(cooldownKey);
            throw e;
        }
    }

    /**
     * 해당 과목을 구독 중인 모든 사용자에게 알림을 발송합니다.
     */
    private void notifySubscribers(SeatOpenedEvent event) {
        List<Subscription> subscriptions = subscriptionRepository.findByCourseKeyAndIsActiveTrue(event.courseKey());
        if (subscriptions.isEmpty()) {
            return;
        }

        List<Long> userIds = subscriptions.stream()
                .map(Subscription::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = findUsersById(userIds);
        Map<Long, List<UserDevice>> deviceMap = findDevicesByUserId(userIds);
        NotificationMessage notification = createSeatOpenedMessage(event);

        for (Subscription subscription : subscriptions) {
            User user = userMap.get(subscription.getUserId());
            if (user == null) {
                continue;
            }
            dispatchAllChannels(user, deviceMap.get(user.getId()), notification, event.courseKey());
        }
    }

    /**
     * 사용자에게 모든 채널로 실제 알림을 발송합니다.
     */
    private void dispatchAllChannels(User user, List<UserDevice> devices, NotificationMessage message,
                                     String courseKey) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            sendNotification(user, devices, message, channel, NotificationContext.forReal(courseKey));
        }
    }

    /**
     * 알림 발송의 핵심 공통 로직을 수행합니다.
     */
    private void sendNotification(User user, List<UserDevice> devices, NotificationMessage message,
                                  NotificationChannel channel, NotificationContext ctx) {
        if (!ctx.forceSend() && !notificationChannelPolicy.isChannelEnabled(user, channel)) {
            return;
        }

        List<NotificationTarget> targets = notificationChannelPolicy.resolveTargets(user, devices, channel);

        if (targets.isEmpty()) {
            if (ctx.forceSend()) { // 테스트 요청인데 발송 대상이 없는 경우 예외 발생
                throw new CustomException(ErrorCode.NOT_FOUND, notificationChannelPolicy.buildNoTargetMessage(channel));
            }
            return;
        }

        boolean delivered = false;
        for (NotificationTarget target : targets) {
            try {
                dispatch(target, message.title(), message.body(), channel);
                delivered = true;
            } catch (RuntimeException e) {
                if (ctx.forceSend()) {
                    throw e;
                }
                log.warn("[Notification] Delivery failed but remaining targets will continue. userId={}, channel={}, failureCode={}, exceptionType={}",
                        user.getId(), channel, SensitiveDataRedactor.failureCode(e),
                        SensitiveDataRedactor.exceptionType(e));
            }
        }

        if (ctx.saveHistory() && delivered) {
            saveHistory(user.getId(), ctx.courseKey(), message, channel);
        }
    }

    /**
     * 사용자 ID 목록을 통해 사용자 맵을 조회합니다.
     */
    private Map<Long, User> findUsersById(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * 사용자 ID 목록을 통해 기기 목록 맵을 조회합니다.
     */
    private Map<Long, List<UserDevice>> findDevicesByUserId(List<Long> userIds) {
        return userDeviceRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(UserDevice::getUserId));
    }

    /**
     * 빈자리 알림 메시지를 생성합니다.
     */
    private NotificationMessage createSeatOpenedMessage(SeatOpenedEvent event) {
        String professor = (event.professor() != null && !event.professor().isBlank()) ? event.professor() : "미지정";
        String title = String.format("[줍줍] %s (%s) 여석 발생!", event.courseName(), professor);
        String body = String.format("과목명: %s (%s)\\n과목코드: %s\\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), professor, event.courseKey(), event.currentSeats());
        return new NotificationMessage(title, body);
    }

    /**
     * 시스템 테스트 알림 메시지를 생성합니다.
     */
    private NotificationMessage createTestMessage() {
        return new NotificationMessage(
                "[줍줍] 시스템 테스트 알림",
                "줍줍 테스트 알림입니다. 수신이 정상적인지 확인해 주세요.");
    }

    /**
     * 지정된 채널의 발송 객체를 통해 실제로 알림을 발송합니다.
     */
    private void dispatch(NotificationTarget target, String title, String message, NotificationChannel channel) {
        notificationSenders.stream()
                .filter(sender -> sender.supports(channel))
                .forEach(sender -> sender.send(target, title, message));
    }

    /**
     * 알림 발송 이력을 저장합니다.
     */
    private void saveHistory(Long userId, String courseKey, NotificationMessage notification,
                             NotificationChannel channel) {
        notificationHistoryRepository.save(NotificationHistory.builder()
                .userId(userId)
                .courseKey(courseKey)
                .title(notification.title())
                .message(notification.body())
                .channel(channel)
                .build());
    }

    /**
     * 알림 메시지 구조체
     */
    private record NotificationMessage(String title, String body) {
    }

    /**
     * 알림 발송 컨텍스트: 이력 저장 여부, 강제 발송 여부, 과목 코드를 묶은 VO입니다.
     */
    private record NotificationContext(boolean saveHistory, boolean forceSend, String courseKey) {

        /**
         * 실제 알림 발송 컨텍스트를 생성합니다.
         */
        static NotificationContext forReal(String courseKey) {
            return new NotificationContext(true, false, courseKey);
        }

        /**
         * 테스트 알림 발송 컨텍스트를 생성합니다.
         */
        static NotificationContext forTest() {
            return new NotificationContext(false, true, "TEST");
        }
    }
}
