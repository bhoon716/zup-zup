package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.api.NotificationOperations;
import bhoon.sugang_helper.academic.events.SeatOpenedEvent;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.cache.RedisAccess;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.client.NotificationProviderResilience;
import bhoon.sugang_helper.seatalert.notification.client.NotificationSender;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationOperations {

    private static final String EVENT_KEY = "event";
    private static final String USER_ID_KEY = "userId";

    private static final String USER_TEST_COOLDOWN_KEY_PREFIX = "ALERT:USER_TEST:";
    private static final Duration USER_TEST_COOLDOWN = Duration.ofSeconds(10);

    private final RedisAccess redisService;
    private final NotificationRecipients userNotificationQuery;
    private final List<NotificationSender> notificationSenders;
    private final NotificationChannelPolicy notificationChannelPolicy;
    private final NotificationProviderResilience notificationProviderResilience;

    SeatDeliveryAttempt attemptSeatOpening(Long userId, NotificationChannel channel, SeatNotificationOutbox outbox,
                                           String idempotencyKey) {
        NotificationRecipient user = userNotificationQuery.findById(userId).orElse(null);
        if (user == null || !notificationChannelPolicy.isChannelEnabled(user, channel)) {
            return SeatDeliveryAttempt.skipped(false);
        }
        List<NotificationTarget> targets = resolveDeliveryTargets(user, channel);
        if (targets.isEmpty()) {
            return SeatDeliveryAttempt.skipped(channel == NotificationChannel.WEB);
        }

        NotificationMessage message = createSeatOpenedMessage(new SeatOpenedEvent(
                outbox.getCourseKey(), outbox.getCourseName(), outbox.getProfessor(),
                outbox.getPreviousSeats(), outbox.getCurrentSeats()));
        List<SeatDeliveryAttempt.TargetFailure> failures = new ArrayList<>();
        boolean delivered = false;
        RuntimeException channelFailure = null;
        for (NotificationTarget target : targets) {
            try {
                sendDelivery(channel, target, message.title(), message.body(), idempotencyKey, "/");
                delivered = true;
            } catch (RuntimeException exception) {
                failures.add(new SeatDeliveryAttempt.TargetFailure(target, exception));
                if (channel == NotificationChannel.WEB && InboxWebPushNotices.isInvalidSubscription(exception)) {
                    continue;
                }
                if (channelFailure == null || (!NotificationFailureClassifier.isRetryable(channelFailure)
                        && NotificationFailureClassifier.isRetryable(exception))) {
                    channelFailure = exception;
                }
            }
        }
        if (!delivered && channelFailure == null && !failures.isEmpty()) {
            channelFailure = failures.getFirst().exception();
        }
        return new SeatDeliveryAttempt(message.title(), message.body(), failures, channelFailure, false);
    }

    List<NotificationTarget> resolveDeliveryTargets(NotificationRecipient user, NotificationChannel channel) {
        List<NotificationDevice> devices = channel == NotificationChannel.WEB
                ? userNotificationQuery.findDevicesByUserId(user.id()) : List.of();
        return notificationChannelPolicy.resolveDeliveryTargets(user, devices, channel);
    }

    void sendDelivery(NotificationChannel channel, NotificationTarget target, String title, String body,
                      String idempotencyKey, String path) {
        NotificationSender sender = requireSender(channel);
        String providerKey = NotificationDeliveryIdempotencyKey.providerKey(idempotencyKey);
        notificationProviderResilience.execute(channel,
                () -> sender.sendDelivery(target, title, body, providerKey, path));
    }

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
            logNotificationTestSendStarted(channel, user.id());
            delivered = sendNotification(user, devices, notification, channel, failOnMissingTarget) || delivered;
        }
        return delivered;
    }

    public void sendUserTestNotification(Long userId) {
        acquireUserTestCooldown(userId);
        NotificationRecipient user = userNotificationQuery.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<NotificationChannel> channels = notificationChannelPolicy.userEnabledChannels(user);
        boolean delivered = sendTestNotification(user, channels, false);
        if (!delivered) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "활성화된 알림 채널에 발송 대상이 없습니다. 설정에서 수신 대상을 확인해주세요.");
        }
        logNotificationTestRequested(user.id(), channels);
    }

    void sendOnboardingTestNotification(NotificationRecipient user, List<NotificationChannel> channels) {
        acquireUserTestCooldown(user.id());
        Map<NotificationChannel, List<NotificationTarget>> targetsByChannel = requireTestTargets(user, channels);
        NotificationMessage message = createTestMessage();

        for (var entry : targetsByChannel.entrySet()) {
            NotificationChannel channel = entry.getKey();
            logNotificationTestSendStarted(channel, user.id());
            for (NotificationTarget target : entry.getValue()) {
                sendDelivery(channel, target, message.title(), message.body(), null, "/");
            }
        }
        logNotificationTestRequested(user.id(), channels);
    }

    private Map<NotificationChannel, List<NotificationTarget>> requireTestTargets(
            NotificationRecipient user, List<NotificationChannel> channels) {
        Map<NotificationChannel, List<NotificationTarget>> targetsByChannel = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : channels) {
            if (!notificationChannelPolicy.isGlobalChannelEnabled(channel) || !requireSender(channel).isConfigured()) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "현재 사용할 수 없는 알림 채널입니다: " + channel);
            }
            List<NotificationTarget> targets = resolveDeliveryTargets(user, channel);
            if (targets.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_INPUT, notificationChannelPolicy.buildNoTargetMessage(channel));
            }
            targetsByChannel.put(channel, targets);
        }
        return targetsByChannel;
    }

    private NotificationSender requireSender(NotificationChannel channel) {
        return notificationSenders.stream()
                .filter(candidate -> candidate.supports(channel))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                        "알림 채널 발송기가 없습니다: " + channel));
    }

    private void acquireUserTestCooldown(Long userId) {
        String cooldownKey = USER_TEST_COOLDOWN_KEY_PREFIX + userId;
        boolean acquired = redisService.setValuesIfAbsent(cooldownKey, "LOCK", USER_TEST_COOLDOWN);
        if (!acquired) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS,
                    "알림 테스트는 " + USER_TEST_COOLDOWN.toSeconds() + "초에 한 번만 가능합니다. 잠시 후 다시 시도해주세요.");
        }
    }

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

    private NotificationMessage createSeatOpenedMessage(SeatOpenedEvent event) {
        String professor = StringUtils.hasText(event.professor()) ? event.professor() : "미지정";
        String title = String.format("[줍줍] %s (%s) 여석 발생!", event.courseName(), professor);
        String body = String.format("과목명: %s (%s)\\n과목코드: %s\\n현재 여석이 발생했습니다! (%d명)",
                event.courseName(), professor, event.courseKey(), event.currentSeats());
        return new NotificationMessage(title, body);
    }

    private NotificationMessage createTestMessage() {
        return new NotificationMessage(
                "[줍줍] 시스템 테스트 알림",
                "줍줍 테스트 알림입니다. 수신이 정상적인지 확인해 주세요.");
    }

    private void dispatch(NotificationTarget target, String title, String message, NotificationChannel channel) {
        notificationSenders.stream()
                .filter(sender -> sender.supports(channel))
                .forEach(sender -> notificationProviderResilience.execute(channel,
                        () -> sender.send(target, title, message)));
    }

    private void logNotificationTestSendStarted(NotificationChannel channel, Long userId) {
        log.atDebug().addKeyValue(EVENT_KEY, "notification_test_send_started")
                .addKeyValue("channel", channel)
                .addKeyValue(USER_ID_KEY, userId)
                .log("[Notification Test] 테스트 알림 발송 시도.");
    }

    private void logNotificationTestRequested(Long userId, List<NotificationChannel> channels) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_test_requested")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("channels", channels)
                .log("[User] 테스트 알림 요청.");
    }

    private record NotificationMessage(String title, String body) {
    }

}
