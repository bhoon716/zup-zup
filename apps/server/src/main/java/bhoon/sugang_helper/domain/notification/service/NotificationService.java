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
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RedisService redisService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final List<NotificationSender> notificationSenders;

    private static final String NOTIFICATION_KEY_PREFIX = "ALERT:";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(10);

    @Async
    @EventListener
    public void handleSeatOpenedEvent(SeatOpenedEvent event) {
        String courseKey = event.courseKey();
        String redisKey = NOTIFICATION_KEY_PREFIX + courseKey;

        if (redisService.hasKey(redisKey)) {
            log.info("[Dedup] Notification already sent for course: {}. Skipping.", courseKey);
            return;
        }

        sendNotification(event);

        redisService.setValues(redisKey, "SENT", DEDUP_TTL);
        log.info("[Notification] Alert sent & Key set for course: {}", courseKey);
    }

    private void sendNotification(SeatOpenedEvent event) {
        List<Subscription> subscriptions = subscriptionRepository.findByCourseKeyAndIsActiveTrue(event.courseKey());

        if (subscriptions.isEmpty()) {
            return;
        }

        List<Long> userIds = subscriptions.stream().map(Subscription::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<UserDevice>> deviceMap = userDeviceRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(UserDevice::getUserId));

        String title = String.format("[SugangHelper] 빈자리 알림: %s", event.courseName());
        String message = String.format("강의명: %s\n과목코드: %s\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), event.courseKey(), event.currentSeats());

        for (Subscription sub : subscriptions) {
            User user = userMap.get(sub.getUserId());
            if (user == null) {
                continue;
            }

            // 1. Email 발송
            if (user.isEmailEnabled()) {
                String targetEmail = (user.getNotificationEmail() != null && !user.getNotificationEmail().isBlank())
                        ? user.getNotificationEmail()
                        : user.getEmail();
                dispatch(NotificationTarget.of(targetEmail), title, message, NotificationChannel.EMAIL);
                saveHistory(user.getId(), event.courseKey(), title, message, NotificationChannel.EMAIL);
            }

            // 2. 등록된 기기들로 푸시 발송 (FCM, Web)
            List<UserDevice> devices = deviceMap.getOrDefault(user.getId(), List.of());
            for (UserDevice device : devices) {
                NotificationChannel channel = mapToChannel(device.getType());

                // 설정 확인
                if (channel == NotificationChannel.WEB && !user.isWebPushEnabled())
                    continue;
                if (channel == NotificationChannel.FCM && !user.isFcmEnabled())
                    continue;

                NotificationTarget target = (channel == NotificationChannel.WEB)
                        ? NotificationTarget.ofWeb(device.getToken(), device.getP256dh(), device.getAuth())
                        : NotificationTarget.of(device.getToken());

                dispatch(target, title, message, channel);
                saveHistory(user.getId(), event.courseKey(), title, message, channel);
            }

            // 3. 디스코드 발송
            if (user.isDiscordEnabled() && user.getDiscordId() != null) {
                dispatch(NotificationTarget.of(user.getDiscordId()), title, message, NotificationChannel.DISCORD);
                saveHistory(user.getId(), event.courseKey(), title, message, NotificationChannel.DISCORD);
            }
        }
    }

    private void dispatch(NotificationTarget target, String title, String message, NotificationChannel channel) {
        notificationSenders.stream()
                .filter(sender -> sender.supports(channel))
                .forEach(sender -> sender.send(target, title, message));
    }

    private void saveHistory(Long userId, String courseKey, String title, String message, NotificationChannel channel) {
        notificationHistoryRepository.save(NotificationHistory.builder()
                .userId(userId)
                .courseKey(courseKey)
                .title(title)
                .message(message)
                .channel(channel)
                .build());
    }

    public void sendTestNotification(User user,
            List<bhoon.sugang_helper.domain.notification.sender.NotificationChannel> channels) {
        String title = "[SugangHelper] 시스템 테스트 알림";
        String message = "관리자 페이지에서 전송된 테스트 알림입니다. 수신이 정상적인지 확인해 주세요.";

        for (bhoon.sugang_helper.domain.notification.sender.NotificationChannel channel : channels) {
            log.info("[NotificationTest] Attempting to send test notification via {}", channel);

            if (channel == NotificationChannel.EMAIL) {
                dispatch(NotificationTarget.of(user.getEmail()), title, message, channel);
                saveHistory(user.getId(), "TEST", title, message, channel);
                continue;
            }

            bhoon.sugang_helper.domain.user.enums.DeviceType deviceType = (channel == NotificationChannel.WEB)
                    ? bhoon.sugang_helper.domain.user.enums.DeviceType.WEB
                    : bhoon.sugang_helper.domain.user.enums.DeviceType.FCM;

            List<UserDevice> devices = userDeviceRepository.findByUserIdAndType(user.getId(), deviceType);

            if (devices.isEmpty() && channel != NotificationChannel.DISCORD) {
                String channelName = (channel == NotificationChannel.WEB) ? "웹 푸시" : "앱 푸시";
                throw new CustomException(ErrorCode.NOT_FOUND,
                        String.format("등록된 %s 기기가 없습니다. 먼저 기기를 등록해 주세요.", channelName));
            }

            if (channel == NotificationChannel.DISCORD) {
                if (user.getDiscordId() == null) {
                    throw new CustomException(ErrorCode.NOT_FOUND, "디스코드 연동 정보가 없습니다. 먼저 디스코드를 연동해 주세요.");
                }
                dispatch(NotificationTarget.of(user.getDiscordId()), title, message, channel);
                saveHistory(user.getId(), "TEST", title, message, channel);
                log.info("[NotificationTest] Successfully dispatched Discord notification to {}", user.getDiscordId());
                continue;
            }

            for (UserDevice device : devices) {
                NotificationTarget target = (channel == NotificationChannel.WEB)
                        ? NotificationTarget.ofWeb(device.getToken(), device.getP256dh(), device.getAuth())
                        : NotificationTarget.of(device.getToken());
                dispatch(target, title, message, channel);
                saveHistory(user.getId(), "TEST", title, message, channel);
                log.info("[NotificationTest] Successfully dispatched {} notification to device {}", channel,
                        device.getId());
            }
        }
    }

    private NotificationChannel mapToChannel(bhoon.sugang_helper.domain.user.enums.DeviceType type) {
        return switch (type) {
            case FCM -> NotificationChannel.FCM;
            case WEB -> NotificationChannel.WEB;
        };
    }
}
