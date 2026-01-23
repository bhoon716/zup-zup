package bhoon.sugang_helper.domain.notification.service;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.sender.NotificationSender;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.UserDevice;
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.notification.sender.WebPushNotificationSender;
import java.util.List;
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
            log.info("[Notification] No active subscriptions for course: {}", event.courseKey());
            return;
        }

        String title = String.format("[SugangHelper] 빈자리 알림: %s", event.courseName());
        String message = String.format("강의명: %s\n과목코드: %s\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), event.courseKey(), event.currentSeats());

        for (Subscription sub : subscriptions) {
            userRepository.findById(sub.getUserId()).ifPresent(user -> {
                // 1. Email 발송 (Default)
                dispatch(user.getEmail(), title, message, NotificationChannel.EMAIL);
                saveHistory(user.getId(), event.courseKey(), title, message, NotificationChannel.EMAIL);

                // 2. 등록된 기기들로 푸시 발송 (FCM, Web)
                List<UserDevice> devices = userDeviceRepository.findByUserId(user.getId());
                for (UserDevice device : devices) {
                    NotificationChannel channel = mapToChannel(device.getType());

                    if (channel == NotificationChannel.WEB) {
                        dispatchWebPush(device, title, message);
                    } else {
                        dispatch(device.getToken(), title, message, channel);
                    }

                    saveHistory(user.getId(), event.courseKey(), title, message, channel);
                }
            });
        }
    }

    private void dispatch(String recipient, String title, String message, NotificationChannel channel) {
        notificationSenders.stream()
                .filter(sender -> sender.supports(channel))
                .forEach(sender -> sender.send(recipient, title, message));
    }

    private void dispatchWebPush(UserDevice device, String title, String message) {
        notificationSenders.stream()
                .filter(sender -> sender instanceof WebPushNotificationSender)
                .map(sender -> (WebPushNotificationSender) sender)
                .forEach(sender -> sender.sendWebPush(
                        device.getToken(),
                        device.getP256dh(),
                        device.getAuth(),
                        title,
                        message));
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

    private NotificationChannel mapToChannel(bhoon.sugang_helper.domain.user.enums.DeviceType type) {
        return switch (type) {
            case FCM -> NotificationChannel.FCM;
            case WEB -> NotificationChannel.WEB;
        };
    }
}
