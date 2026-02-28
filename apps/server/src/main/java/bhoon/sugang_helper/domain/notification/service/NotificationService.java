package bhoon.sugang_helper.domain.notification.service;

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
    private static final Duration DEDUP_TTL = Duration.ofMinutes(10);
    private static final String USER_TEST_COOLDOWN_KEY_PREFIX = "ALERT:USER_TEST:";
    private static final Duration USER_TEST_COOLDOWN = Duration.ofSeconds(10);

    private final RedisService redisService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final List<NotificationSender> notificationSenders;

    /**
     * 빈자리 발생 이벤트를 처리하여 구독자들에게 알림을 발송합니다.
     */
    @Async
    @EventListener
    public void handleSeatOpenedEvent(SeatOpenedEvent event) {
        String redisKey = NOTIFICATION_KEY_PREFIX + event.courseKey();
        if (redisService.hasKey(redisKey)) {
            log.debug("[알림] 중복 발송을 방지했습니다. courseKey={}", event.courseKey());
            return;
        }

        notifySubscribers(event);
        redisService.setValues(redisKey, "SENT", DEDUP_TTL);
        log.info("[알림] 빈자리 알림 발송을 완료했습니다. courseKey={}", event.courseKey());
    }

    /**
     * 특정 사용자에게 여러 채널로 테스트 알림을 발송합니다.
     */
    public void sendTestNotification(User user, List<NotificationChannel> channels) {
        NotificationMessage notification = createTestMessage();
        List<UserDevice> devices = userDeviceRepository.findByUserId(user.getId());

        for (NotificationChannel channel : channels) {
            log.info("[알림 테스트] 발송을 시도합니다. channel={}, userId={}", channel, user.getId());
            sendNotification(user, devices, notification, channel, "TEST", false, true);
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

            for (NotificationChannel channel : NotificationChannel.values()) {
                sendNotification(user, deviceMap.get(user.getId()), notification, channel, event.courseKey(), true,
                        false);
            }
        }
    }

    /**
     * 알림 발송의 핵심 공통 로직을 수행합니다.
     * 
     * @param forceSend   활성 상태 설정을 무시하고 강제로 발송할지 여부 (테스트 용도)
     * @param saveHistory 발송 이력을 DB에 저장할지 여부
     */
    private void sendNotification(User user, List<UserDevice> devices, NotificationMessage message,
            NotificationChannel channel, String courseKey, boolean saveHistory, boolean forceSend) {
        if (!forceSend && !isChannelEnabled(user, channel)) {
            return;
        }

        List<NotificationTarget> targets = resolveTargets(user, devices, channel);

        if (targets.isEmpty()) {
            if (forceSend) { // 테스트 요청인데 타겟이 없는 경우 예외 발생
                throw new CustomException(ErrorCode.NOT_FOUND, buildNoTargetMessage(channel));
            }
            return;
        }

        targets.forEach(target -> dispatch(target, message.title(), message.body(), channel));

        if (saveHistory) {
            saveHistory(user.getId(), courseKey, message, channel);
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
     * 사용자의 알림 수신 설정이 채널별로 활성화되어 있는지 확인합니다.
     */
    private boolean isChannelEnabled(User user, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> user.isEmailEnabled();
            case DISCORD -> user.isDiscordEnabled() && user.getDiscordId() != null;
            case WEB -> user.isWebPushEnabled();
            case FCM -> user.isFcmEnabled();
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
     * 기기가 없을 경우의 에러 메시지를 생성합니다.
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
     * 발신 대상 기기 정보를 통해 NotificationTarget 객체를 생성합니다.
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
        String title = String.format("[전북대 수강신청 도우미] 빈자리 알림: %s", event.courseName());
        String body = String.format("강의명: %s\\n과목코드: %s\\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), event.courseKey(), event.currentSeats());
        return new NotificationMessage(title, body);
    }

    /**
     * 시스템 테스트 알림 메시지를 생성합니다.
     */
    private NotificationMessage createTestMessage() {
        return new NotificationMessage(
                "[전북대 수강신청 도우미] 시스템 테스트 알림",
                "전북대 수강신청 도우미 테스트 알림입니다. 수신이 정상적인지 확인해 주세요.");
    }

    /**
     * 알림 발송 객체를 통해 실제로 알림을 발송합니다.
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
     * 알림 메시지 구조체 (내부 레코드)
     */
    private record NotificationMessage(String title, String body) {
    }
}
