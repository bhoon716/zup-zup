package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    private static final List<SeatNotificationDeliveryStatus> ACTIVE_DELIVERY_STATUSES = List.of(
            SeatNotificationDeliveryStatus.PENDING, SeatNotificationDeliveryStatus.PROCESSING);

    private final SeatNotificationOutboxJpaRepository outboxRepository;
    private final SeatNotificationDeliveryJpaRepository deliveryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationChannelPolicy notificationChannelPolicy;
    private final NotificationService notificationService;
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
    public List<Long> claimReadyDeliveryIds() {
        LocalDateTime now = LocalDateTime.now();
        List<SeatNotificationDelivery> deliveries = deliveryRepository.findReadyForUpdate(
                SeatNotificationDeliveryStatus.PENDING,
                SeatNotificationDeliveryStatus.PROCESSING,
                now,
                PageRequest.of(0, batchSize));
        deliveries.forEach(delivery -> delivery.claim(now.plusSeconds(leaseSeconds)));
        return deliveries.stream().map(SeatNotificationDelivery::getId).toList();
    }

    @Transactional
    public void processDelivery(Long deliveryId) {
        SeatNotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != SeatNotificationDeliveryStatus.PROCESSING) {
            return;
        }

        try {
            notificationService.deliverSeatOpening(
                    delivery.getUserId(), delivery.getChannel(), delivery.getOutbox());
            delivery.markSent();
        } catch (RuntimeException e) {
            boolean deadLettered = delivery.markFailure(e.getMessage(), maximumAttempts,
                    LocalDateTime.now().plusSeconds(backoffSeconds(delivery.getAttempts() + 1)));
            if (deadLettered) {
                meterRegistry.counter("notification.outbox.dlq", "channel", delivery.getChannel().name()).increment();
                log.error("[Notification Outbox] Delivery moved to DLQ. deliveryId={}, outboxId={}",
                        delivery.getId(), delivery.getOutbox().getId(), e);
            } else {
                meterRegistry.counter("notification.outbox.retry", "channel", delivery.getChannel().name()).increment();
                log.warn("[Notification Outbox] Delivery scheduled for retry. deliveryId={}, attempts={}",
                        delivery.getId(), delivery.getAttempts(), e);
            }
        }
        updateOutboxTerminalStatus(delivery.getOutbox());
    }

    private void materializeDeliveries(SeatNotificationOutbox outbox) {
        List<Subscription> subscriptions = subscriptionRepository.findByCourseKeyAndIsActiveTrue(outbox.getCourseKey());
        if (subscriptions.isEmpty()) {
            outbox.markCompleted();
            return;
        }
        List<Long> userIds = subscriptions.stream().map(Subscription::getUserId).distinct().toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<UserDevice>> devicesByUser = userDeviceRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(UserDevice::getUserId));

        for (Long userId : userIds) {
            User user = usersById.get(userId);
            if (user == null) {
                continue;
            }
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (!notificationChannelPolicy.isChannelEnabled(user, channel)
                        || notificationChannelPolicy.resolveTargets(user, devicesByUser.get(userId), channel).isEmpty()
                        || deliveryRepository.existsByOutboxIdAndUserIdAndChannel(outbox.getId(), userId, channel)) {
                    continue;
                }
                deliveryRepository.save(SeatNotificationDelivery.builder()
                        .outbox(outbox)
                        .userId(userId)
                        .channel(channel)
                        .build());
            }
        }
        if (deliveryRepository.countByOutboxIdAndStatusIn(outbox.getId(), ACTIVE_DELIVERY_STATUSES) == 0) {
            outbox.markCompleted();
        } else {
            outbox.markProcessing();
        }
    }

    private void updateOutboxTerminalStatus(SeatNotificationOutbox outbox) {
        if (deliveryRepository.countByOutboxIdAndStatusIn(outbox.getId(), ACTIVE_DELIVERY_STATUSES) != 0) {
            return;
        }
        if (deliveryRepository.countByOutboxIdAndStatus(outbox.getId(), SeatNotificationDeliveryStatus.DLQ) > 0) {
            outbox.markDeadLettered();
        } else {
            outbox.markCompleted();
        }
    }

    private long backoffSeconds(int attempt) {
        return Math.min(300, 1L << Math.min(attempt - 1, 8));
    }
}
