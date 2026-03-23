package bhoon.sugang_helper.domain.notification.service;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.sender.NotificationSender;
import bhoon.sugang_helper.domain.notification.sender.NotificationTarget;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.entity.UserDevice;
import bhoon.sugang_helper.domain.user.enums.DeviceType;
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    private final NotificationProperties notificationProperties;

    /**
     * 빈자리 발생 이벤트를 처리하여 구독자들에게 알림을 발송합니다.
     */
    @Async
    @EventListener
    public void handleSeatOpenedEvent(SeatOpenedEvent event) {
        String redisKey = NOTIFICATION_KEY_PREFIX + event.courseKey();
        if (redisService.hasKey(redisKey)) {
            log.debug("[Notification] Skipped duplicate sending. courseKey={}", event.courseKey());
            return;
        }

        notifySubscribers(event);
        redisService.setValues(redisKey, "SENT", DEDUP_TTL);
        log.info("[Notification] Completed sending seat opening notifications. courseKey={}", event.courseKey());
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
        if (!ctx.forceSend() && !isChannelEnabled(user, channel)) {
            return;
        }

        List<NotificationTarget> targets = resolveTargets(user, devices, channel);

        if (targets.isEmpty()) {
            if (ctx.forceSend()) { // 테스트 요청인데 발송 대상이 없는 경우 예외 발생
                throw new CustomException(ErrorCode.NOT_FOUND, buildNoTargetMessage(channel));
            }
            return;
        }

        targets.forEach(target -> dispatch(target, message.title(), message.body(), channel));

        if (ctx.saveHistory()) {
            saveHistory(user.getId(), ctx.courseKey(), message, channel);
        }
    }

    /**
     * 채널별 발송 대상(Target)을 추출합니다.
     */
    private List<NotificationTarget> resolveTargets(User user, List<UserDevice> devices, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> List.of(NotificationTarget.of(resolveNotificationEmail(user)));
            case DISCORD ->
                user.getDiscordId() != null ? List.of(NotificationTarget.of(user.getDiscordId())) : List.of();
            case WEB, FCM -> {
                if (devices == null)
                    yield List.of();
                DeviceType type = (channel == NotificationChannel.WEB) ? DeviceType.WEB : DeviceType.FCM;
                yield devices.stream()
                        .filter(d -> d.getType() == type)
                        .map(d -> toDeviceTarget(d, channel))
                        .toList();
            }
        };
    }

    /**
     * 글로벌 채널 설정 및 사용자 개인 수신 설정을 복합 확인합니다.
     * 글로벌 설정이 꺼져 있으면 사용자 설정과 무관하게 발송을 차단합니다.
     */
    private boolean isChannelEnabled(User user, NotificationChannel channel) {
        if (!isGlobalChannelEnabled(channel)) {
            log.debug("[Notification] Channel disabled globally. channel={}", channel);
            return false;
        }
        return switch (channel) {
            case EMAIL -> user.isEmailEnabled();
            case DISCORD -> user.isDiscordEnabled() && user.getDiscordId() != null;
            case WEB -> user.isWebPushEnabled();
            case FCM -> user.isFcmEnabled();
        };
    }

    /**
     * application.yml의 글로벌 채널 설정 값을 확인합니다.
     */
    private boolean isGlobalChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> notificationProperties.email();
            case DISCORD -> notificationProperties.discord();
            case WEB -> notificationProperties.webpush();
            case FCM -> notificationProperties.fcm();
        };
    }

    /**
     * 알림을 수신할 이메일 주소를 결정합니다. (설정된 알림 이메일 우선)
     */
    private String resolveNotificationEmail(User user) {
        return (user.getNotificationEmail() != null && !user.getNotificationEmail().isBlank())
                ? user.getNotificationEmail()
                : user.getEmail();
    }

    /**
     * 발송 대상이 없을 때의 에러 메시지를 반환합니다.
     */
    private String buildNoTargetMessage(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> "등록된 이메일 정보가 없습니다.";
            case DISCORD -> "디스코드 연동 정보가 없습니다. 먼저 디스코드를 연동해 주세요.";
            case WEB -> "등록된 웹 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
            case FCM -> "등록된 앱 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
        };
    }

    /**
     * 기기 정보를 통해 NotificationTarget 객체를 생성합니다.
     */
    private NotificationTarget toDeviceTarget(UserDevice device, NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return NotificationTarget.ofWeb(device.getToken(), device.getP256dh(), device.getAuth());
        }
        return NotificationTarget.of(device.getToken());
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
        String title = String.format("[줍줍] 빈자리 알림: %s", event.courseName());
        String body = String.format("강의명: %s\\n과목코드: %s\\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), event.courseKey(), event.currentSeats());
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

    /** 알림 메시지 구조체 */
    private record NotificationMessage(String title, String body) {
    }

    /**
     * 알림 발송 컨텍스트: 이력 저장 여부, 강제 발송 여부, 과목 코드를 묶은 VO입니다.
     */
    private record NotificationContext(boolean saveHistory, boolean forceSend, String courseKey) {

        /** 실제 알림 발송 컨텍스트를 생성합니다. */
        static NotificationContext forReal(String courseKey) {
            return new NotificationContext(true, false, courseKey);
        }

        /** 테스트 알림 발송 컨텍스트를 생성합니다. */
        static NotificationContext forTest() {
            return new NotificationContext(false, true, "TEST");
        }
    }
}
