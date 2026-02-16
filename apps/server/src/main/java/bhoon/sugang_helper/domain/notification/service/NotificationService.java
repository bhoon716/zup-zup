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
    private static final String TEST_COURSE_KEY = "TEST";

    private final RedisService redisService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final List<NotificationSender> notificationSenders;

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

    public void sendTestNotification(User user, List<NotificationChannel> channels) {
        NotificationMessage notification = createTestMessage();
        for (NotificationChannel channel : channels) {
            sendTestByChannel(user, channel, notification);
        }
    }

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

            sendEmailIfEnabled(user, notification, event.courseKey());
            sendDeviceIfEnabled(user, deviceMap.getOrDefault(user.getId(), List.of()), notification, event.courseKey());
            sendDiscordIfEnabled(user, notification, event.courseKey());
        }
    }

    private Map<Long, User> findUsersById(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, List<UserDevice>> findDevicesByUserId(List<Long> userIds) {
        return userDeviceRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(UserDevice::getUserId));
    }

    private NotificationMessage createSeatOpenedMessage(SeatOpenedEvent event) {
        String title = String.format("[SugangHelper] 빈자리 알림: %s", event.courseName());
        String body = String.format("강의명: %s\n과목코드: %s\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), event.courseKey(), event.currentSeats());
        return new NotificationMessage(title, body);
    }

    private NotificationMessage createTestMessage() {
        return new NotificationMessage(
                "[SugangHelper] 시스템 테스트 알림",
                "관리자 페이지에서 전송된 테스트 알림입니다. 수신이 정상적인지 확인해 주세요.");
    }

    private void sendEmailIfEnabled(User user, NotificationMessage notification, String courseKey) {
        if (!user.isEmailEnabled()) {
            return;
        }

        String targetEmail = resolveNotificationEmail(user);
        dispatch(NotificationTarget.of(targetEmail), notification.title(), notification.body(),
                NotificationChannel.EMAIL);
        saveHistory(user.getId(), courseKey, notification, NotificationChannel.EMAIL);
    }

    private void sendDeviceIfEnabled(User user, List<UserDevice> devices, NotificationMessage notification,
                                     String courseKey) {
        if (devices.isEmpty()) {
            return;
        }

        for (UserDevice device : devices) {
            NotificationChannel channel = mapToChannel(device.getType());
            if (!isChannelEnabled(user, channel)) {
                continue;
            }

            NotificationTarget target = toDeviceTarget(device, channel);
            dispatch(target, notification.title(), notification.body(), channel);
            saveHistory(user.getId(), courseKey, notification, channel);
        }
    }

    private void sendDiscordIfEnabled(User user, NotificationMessage notification, String courseKey) {
        if (!user.isDiscordEnabled() || user.getDiscordId() == null) {
            return;
        }

        dispatch(NotificationTarget.of(user.getDiscordId()), notification.title(), notification.body(),
                NotificationChannel.DISCORD);
        saveHistory(user.getId(), courseKey, notification, NotificationChannel.DISCORD);
    }

    private String resolveNotificationEmail(User user) {
        if (user.getNotificationEmail() != null && !user.getNotificationEmail().isBlank()) {
            return user.getNotificationEmail();
        }
        return user.getEmail();
    }

    private boolean isChannelEnabled(User user, NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return user.isWebPushEnabled();
        }
        if (channel == NotificationChannel.FCM) {
            return user.isFcmEnabled();
        }
        return true;
    }

    private NotificationTarget toDeviceTarget(UserDevice device, NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return NotificationTarget.ofWeb(device.getToken(), device.getP256dh(), device.getAuth());
        }
        return NotificationTarget.of(device.getToken());
    }

    private void sendTestByChannel(User user, NotificationChannel channel, NotificationMessage notification) {
        log.info("[알림 테스트] 발송을 시도합니다. channel={}, userId={}", channel, user.getId());

        if (channel == NotificationChannel.EMAIL) {
            sendTestEmail(user, notification);
            return;
        }

        if (channel == NotificationChannel.DISCORD) {
            sendTestDiscord(user, notification);
            return;
        }

        sendTestDevice(user, channel, notification);
    }

    private void sendTestEmail(User user, NotificationMessage notification) {
        dispatch(NotificationTarget.of(user.getEmail()), notification.title(), notification.body(),
                NotificationChannel.EMAIL);
        saveHistory(user.getId(), TEST_COURSE_KEY, notification, NotificationChannel.EMAIL);
    }

    private void sendTestDiscord(User user, NotificationMessage notification) {
        if (user.getDiscordId() == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "디스코드 연동 정보가 없습니다. 먼저 디스코드를 연동해 주세요.");
        }

        dispatch(NotificationTarget.of(user.getDiscordId()), notification.title(), notification.body(),
                NotificationChannel.DISCORD);
        saveHistory(user.getId(), TEST_COURSE_KEY, notification, NotificationChannel.DISCORD);
        log.info("[알림 테스트] 디스코드 발송을 완료했습니다. discordId={}", user.getDiscordId());
    }

    private void sendTestDevice(User user, NotificationChannel channel, NotificationMessage notification) {
        DeviceType deviceType = mapToDeviceType(channel);
        List<UserDevice> devices = userDeviceRepository.findByUserIdAndType(user.getId(), deviceType);
        if (devices.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FOUND, buildNoDeviceMessage(channel));
        }

        for (UserDevice device : devices) {
            NotificationTarget target = toDeviceTarget(device, channel);
            dispatch(target, notification.title(), notification.body(), channel);
            saveHistory(user.getId(), TEST_COURSE_KEY, notification, channel);
            log.info("[알림 테스트] 디바이스 발송을 완료했습니다. channel={}, deviceId={}", channel, device.getId());
        }
    }

    private DeviceType mapToDeviceType(NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return DeviceType.WEB;
        }
        return DeviceType.FCM;
    }

    private String buildNoDeviceMessage(NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return "등록된 웹 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
        }
        return "등록된 앱 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
    }

    private void dispatch(NotificationTarget target, String title, String message, NotificationChannel channel) {
        notificationSenders.stream()
                .filter(sender -> sender.supports(channel))
                .forEach(sender -> sender.send(target, title, message));
    }

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

    private NotificationChannel mapToChannel(DeviceType type) {
        return switch (type) {
            case FCM -> NotificationChannel.FCM;
            case WEB -> NotificationChannel.WEB;
        };
    }

    private record NotificationMessage(String title, String body) {
    }
}
