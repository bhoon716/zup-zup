package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.academic.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.identity.user.dto.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.NotificationRecipient;
import bhoon.sugang_helper.identity.user.service.UserNotificationQuery;
import bhoon.sugang_helper.identity.user.service.UserNotificationTestSender;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.client.NotificationProviderResilience;
import bhoon.sugang_helper.seatalert.notification.client.NotificationSender;
import bhoon.sugang_helper.seatalert.notification.dto.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationHistoryRepository;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자에게 다양한 채널(Email, FCM, WebPush, Discord)을 통해 알림을 발송하는 통합 서비스입니다. 중복 발송 방지 및 알림 이력 관리 기능을 포함합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationTestSender, UserNotificationTestSender {

    private static final String USER_TEST_COOLDOWN_KEY_PREFIX = "ALERT:USER_TEST:";
    private static final Duration USER_TEST_COOLDOWN = Duration.ofSeconds(10);

    private final RedisService redisService;
    private final UserNotificationQuery userNotificationQuery;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final List<NotificationSender> notificationSenders;
    private final NotificationChannelPolicy notificationChannelPolicy;
    private final NotificationProviderResilience notificationProviderResilience;

    public void deliverSeatOpening(Long userId, NotificationChannel channel,
                                   SeatNotificationOutbox outbox,
                                   String idempotencyKey) {
        NotificationRecipient user = userNotificationQuery.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!notificationChannelPolicy.isChannelEnabled(user, channel)) {
            return;
        }
        List<NotificationTarget> targets = notificationChannelPolicy.resolveTargets(
                user, userNotificationQuery.findDevicesByUserId(userId), channel);
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
            notificationProviderResilience.execute(channel,
                    () -> sender.send(target, message.title(), message.body(), providerKey));
        }
        saveHistory(userId, outbox.getCourseKey(), message, channel);
    }

    /**
     * 특정 사용자에게 여러 채널로 테스트 알림을 발송합니다.
     */
    public void sendAdminTestNotification(Long userId, List<NotificationChannel> channels) {
        NotificationRecipient user = userNotificationQuery.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        sendTestNotification(user, channels, true);
    }

    private boolean sendTestNotification(NotificationRecipient user, List<NotificationChannel> channels,
                                         boolean failOnMissingTarget) {
        NotificationMessage notification = createTestMessage();
        List<NotificationDevice> devices = userNotificationQuery.findDevicesByUserId(user.id());
        boolean delivered = false;

        for (NotificationChannel channel : channels) {
            log.info("[Notification Test] Attempting to send. channel={}, userId={}", channel, user.id());
            delivered = sendNotification(user, devices, notification, channel, failOnMissingTarget) || delivered;
        }
        return delivered;
    }

    /**
     * 사용자 단에서 요청한 테스트 알림을 발송하며, 쿨타임을 적용합니다.
     */
    @Override
    public void sendUserTestNotification(Long userId) {
        String cooldownKey = USER_TEST_COOLDOWN_KEY_PREFIX + userId;
        boolean acquired = redisService.setValuesIfAbsent(cooldownKey, "LOCK", USER_TEST_COOLDOWN);
        if (!acquired) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS, "알림 테스트는 10초에 한 번만 가능합니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            NotificationRecipient user = userNotificationQuery.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            List<NotificationChannel> channels = notificationChannelPolicy.userEnabledChannels(user);
            boolean delivered = sendTestNotification(
                    user, channels, false);
            if (!delivered) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "활성화된 알림 채널에 발송 대상이 없습니다. 설정에서 수신 대상을 확인해주세요.");
            }
            log.info("[User] Send test notification: userId={}, channels={}", user.id(), channels);
        } catch (RuntimeException e) {
            redisService.deleteValues(cooldownKey);
            throw e;
        }
    }

    /**
     * 관리자·사용자 테스트 알림의 대상 확인과 발송을 수행합니다.
     */
    private boolean sendNotification(NotificationRecipient user, List<NotificationDevice> devices,
                                     NotificationMessage message,
                                     NotificationChannel channel, boolean failOnMissingTarget) {
        List<NotificationTarget> targets = notificationChannelPolicy.resolveTargets(user, devices, channel);

        if (targets.isEmpty()) {
            if (failOnMissingTarget) {
                throw new CustomException(ErrorCode.NOT_FOUND, notificationChannelPolicy.buildNoTargetMessage(channel));
            }
            return false;
        }

        for (NotificationTarget target : targets) {
            dispatch(target, message.title(), message.body(), channel);
        }
        return true;
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
                .forEach(sender -> notificationProviderResilience.execute(channel,
                        () -> sender.send(target, title, message)));
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

}
