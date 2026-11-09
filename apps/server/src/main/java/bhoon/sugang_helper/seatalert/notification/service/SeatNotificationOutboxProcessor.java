package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryDispatch;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryFailureResult;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository.DeliveryRecipient;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatNotificationOutboxProcessor {

    private static final String EVENT_KEY = "event";
    private static final String DELIVERY_ID_KEY = "deliveryId";

    private static final String CHANNEL_TAG = "channel";
    private static final List<SeatNotificationDeliveryStatus> ACTIVE_DELIVERY_STATUSES = List.of(
            SeatNotificationDeliveryStatus.PENDING, SeatNotificationDeliveryStatus.PROCESSING);

    private final SeatNotificationOutboxRepository outboxRepository;
    private final SeatNotificationDeliveryRepository deliveryRepository;
    private final SubscriptionQueryService subscriptionQueryService;
    private final NotificationRecipients userNotificationQuery;
    private final NotificationChannelPolicy notificationChannelPolicy;
    private final NotificationService notificationService;
    private final SeatNotificationDeliverySettlementService settlementService;
    private final MeterRegistry meterRegistry;

    @Value("${app.notification.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.notification.outbox.max-attempts:5}")
    private int maximumAttempts;

    @Value("${app.notification.outbox.lease-seconds:60}")
    private long leaseSeconds;

    @Transactional
    public void materializePendingOutboxes() {
        List<SeatNotificationOutbox> outboxes = outboxRepository.findByStatusForUpdate(
                SeatNotificationOutboxStatus.PENDING, PageRequest.of(0, batchSize));
        for (SeatNotificationOutbox outbox : outboxes) {
            materializeDeliveries(outbox);
        }
    }

    @Transactional
    public List<SeatNotificationDeliveryClaim> claimReadyDeliveries() {
        return claimReadyDeliveries(batchSize);
    }

    @Transactional
    public List<SeatNotificationDeliveryClaim> claimReadyDeliveries(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<SeatNotificationDelivery> deliveries = deliveryRepository.findReadyForUpdate(
                SeatNotificationDeliveryStatus.PENDING,
                SeatNotificationDeliveryStatus.PROCESSING,
                now,
                PageRequest.of(0, Math.max(1, limit)));
        return deliveries.stream()
                .map(delivery -> {
                    String claimToken = delivery.claim(now.plusSeconds(leaseSeconds));
                    recordClaimLag(delivery, now);
                    return new SeatNotificationDeliveryClaim(delivery.getId(), claimToken, now, delivery.getChannel());
                })
                .toList();
    }

    public void processDelivery(SeatNotificationDeliveryClaim claim) {
        SeatNotificationDeliveryDispatch dispatch = settlementService.loadForDispatch(claim);
        if (dispatch == null) {
            return;
        }

        try {
            recordClaimToAttemptLag(claim, dispatch.channel());
            notificationService.deliverSeatOpening(
                    dispatch.userId(), dispatch.channel(), dispatch.outbox(), dispatch.idempotencyKey());
            if (!settlementService.markSent(claim)) {
                logSuccessSettlementSkipped(claim.deliveryId());
            }
        } catch (RuntimeException e) {
            handleDeliveryFailure(claim, dispatch, e);
        }
    }

    private void handleDeliveryFailure(SeatNotificationDeliveryClaim claim, SeatNotificationDeliveryDispatch dispatch,
                                        RuntimeException failure) {
        String failureCode = SensitiveDataRedactor.failureCode(failure);
        int attempts = 1;
        if (NotificationFailureClassifier.isRetryable(failure)) {
            attempts = maximumAttempts;
        }
        SeatNotificationDeliveryFailureResult result = settlementService.markFailure(claim, failureCode, attempts);
        if (!result.settled()) {
            logFailureSettlementSkipped(claim.deliveryId());
            return;
        }
        if (result.deadLettered()) {
            meterRegistry.counter("notification.outbox.dlq", CHANNEL_TAG, dispatch.channel().name()).increment();
            logDeadLetteredDelivery(claim, dispatch, failureCode, failure);
            return;
        }
        meterRegistry.counter("notification.outbox.retry", CHANNEL_TAG, dispatch.channel().name()).increment();
        logRetryScheduled(claim, dispatch, result.attempts(), failureCode, failure);
    }

    private void logDeadLetteredDelivery(SeatNotificationDeliveryClaim claim, SeatNotificationDeliveryDispatch dispatch,
                                         String failureCode, RuntimeException e) {
        log.atError().addKeyValue(EVENT_KEY, "notification_delivery_dead_lettered")
                .addKeyValue(DELIVERY_ID_KEY, claim.deliveryId())
                .addKeyValue("outboxId", dispatch.outbox().getId())
                .addKeyValue("channel", dispatch.channel())
                .addKeyValue("failureCode", failureCode)
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(e))
                .setCause(SensitiveDataRedactor.safeStackTrace(e))
                .log("[Notification Outbox] 발송 재시도 종료 후 DLQ 이동.");
    }

    private void logRetryScheduled(SeatNotificationDeliveryClaim claim, SeatNotificationDeliveryDispatch dispatch,
                                   int attempts, String failureCode, RuntimeException e) {
        log.atWarn().addKeyValue(EVENT_KEY, "notification_delivery_retry_scheduled")
                .addKeyValue(DELIVERY_ID_KEY, claim.deliveryId())
                .addKeyValue("attempts", attempts)
                .addKeyValue("channel", dispatch.channel())
                .addKeyValue("failureCode", failureCode)
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(e))
                .log("[Notification Outbox] 발송 재시도 예약.");
    }

    private void recordClaimLag(SeatNotificationDelivery delivery, LocalDateTime claimedAt) {
        if (delivery.getCreatedAt() == null) {
            return;
        }
        Timer timer = meterRegistry.timer(
                "notification.outbox.claim.lag", CHANNEL_TAG, delivery.getChannel().name());
        if (timer != null) {
            timer.record(Duration.between(delivery.getCreatedAt(), claimedAt));
        }
    }

    private void recordClaimToAttemptLag(SeatNotificationDeliveryClaim claim, NotificationChannel channel) {
        Timer timer = meterRegistry.timer(
                "notification.outbox.claim_to_attempt", CHANNEL_TAG, channel.name());
        if (timer != null) {
            timer.record(Duration.between(claim.claimedAt(), LocalDateTime.now()));
        }
    }

    private void materializeDeliveries(SeatNotificationOutbox outbox) {
        List<Long> recipientIds = subscriptionQueryService.findActiveRecipientIds(outbox.getCourseKey());
        if (recipientIds.isEmpty()) {
            outbox.markCompleted();
            return;
        }
        List<Long> userIds = recipientIds.stream().distinct().toList();
        Map<Long, NotificationRecipient> usersById = userNotificationQuery.findByIds(userIds).stream()
                .collect(Collectors.toMap(NotificationRecipient::id, Function.identity()));
        Map<Long, List<NotificationDevice>> devicesByUser = userNotificationQuery.findDevicesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(NotificationDevice::userId));
        Map<Long, Set<NotificationChannel>> existingChannelsByUser = deliveryRepository
                .findExistingRecipientsByOutboxId(outbox.getId(), List.of(NotificationChannel.values())).stream()
                .collect(Collectors.groupingBy(DeliveryRecipient::getUserId,
                        Collectors.mapping(DeliveryRecipient::getChannel, Collectors.toSet())));

        List<SeatNotificationDelivery> deliveriesToSave = new ArrayList<>();
        for (Long userId : userIds) {
            NotificationRecipient user = usersById.get(userId);
            if (user == null) {
                continue;
            }
            Set<NotificationChannel> existingChannels = existingChannelsByUser.getOrDefault(userId, Set.of());
            addRecipientDeliveries(outbox, user, devicesByUser.get(userId), existingChannels, deliveriesToSave);
        }
        if (!deliveriesToSave.isEmpty()) {
            deliveryRepository.saveAll(deliveriesToSave);
        }
        if (deliveryRepository.countByOutboxIdAndStatusIn(
                outbox.getId(), ACTIVE_DELIVERY_STATUSES) == 0) {
            outbox.markCompleted();
            return;
        }
        outbox.markProcessing();
    }

    private void addRecipientDeliveries(SeatNotificationOutbox outbox, NotificationRecipient user,
                                        List<NotificationDevice> devices,
                                        Set<NotificationChannel> existingChannels,
                                        List<SeatNotificationDelivery> deliveries) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            if (!notificationChannelPolicy.isChannelEnabled(user, channel)
                    || notificationChannelPolicy.resolveTargets(user, devices, channel).isEmpty()
                    || existingChannels.contains(channel)) {
                continue;
            }
            deliveries.add(SeatNotificationDelivery.builder()
                    .outbox(outbox)
                    .userId(user.id())
                    .channel(channel)
                    .build());
        }
    }

    private void logSuccessSettlementSkipped(Long deliveryId) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_success_settlement_skipped")
                .addKeyValue(DELIVERY_ID_KEY, deliveryId)
                .log("[Notification Outbox] 선점이 바뀐 작업의 성공 정산 생략.");
    }

    private void logFailureSettlementSkipped(Long deliveryId) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_failure_settlement_skipped")
                .addKeyValue(DELIVERY_ID_KEY, deliveryId)
                .log("[Notification Outbox] 선점이 바뀐 작업의 실패 정산 생략.");
    }

}
