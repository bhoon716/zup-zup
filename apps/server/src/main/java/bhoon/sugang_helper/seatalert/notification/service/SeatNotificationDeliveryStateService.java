package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryDispatch;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryFailureResult;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatNotificationDeliveryStateService {

    private final SeatNotificationDeliveryRepository deliveryRepository;
    private final SeatNotificationOutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;
    private final NotificationHistoryRepository histories;
    private final NotificationRecipients recipients;
    private final NotificationChannelPolicy policy;
    private final InboxWebPushNotices notices;

    @Value("${app.notification.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.notification.outbox.lease-seconds:60}")
    private long leaseSeconds;

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
                .map(delivery -> claimDelivery(delivery, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatNotificationDeliveryDispatch loadForDispatch(SeatNotificationDeliveryClaim claim) {
        return deliveryRepository.findClaimedForDispatch(
                        claim.deliveryId(), SeatNotificationDeliveryStatus.PROCESSING, claim.claimToken())
                .filter(delivery -> delivery.getLockedUntil().isAfter(LocalDateTime.now()))
                .map(SeatNotificationDeliveryDispatch::from)
                .orElse(null);
    }

    @Transactional
    public SeatNotificationDeliveryFailureResult settleAttempt(SeatNotificationDeliveryClaim claim, Long userId,
                                                                SeatDeliveryAttempt attempt, String failureCode,
                                                                int maximumAttempts) {
        Optional<NotificationRecipient> active = attempt.failures().isEmpty() && !attempt.missingWebTarget()
                ? Optional.empty() : recipients.findActiveByIdForUpdate(userId);
        Optional<SeatNotificationDelivery> claimedDelivery = findClaimedForSettlement(claim);
        if (claimedDelivery.isEmpty()) {
            return SeatNotificationDeliveryFailureResult.stale();
        }
        SeatNotificationDelivery delivery = claimedDelivery.get();
        boolean deadLettered = false;
        if (attempt.channelFailure() == null) {
            delivery.markSent();
            if (attempt.sent()) {
                saveHistory(delivery, attempt);
            }
        } else {
            LocalDateTime now = LocalDateTime.now();
            deadLettered = delivery.markFailure(failureCode, maximumAttempts, now,
                    now.plusSeconds(NotificationDeliveryRetry.backoffSeconds(delivery.getAttempts() + 1)));
        }
        updateOutboxTerminalStatus(delivery.getOutbox());
        boolean terminalFailure = deadLettered;
        active.ifPresent(user -> recordNotices(delivery, attempt, user, terminalFailure));
        return new SeatNotificationDeliveryFailureResult(true, terminalFailure, delivery.getAttempts());
    }

    private void saveHistory(SeatNotificationDelivery delivery, SeatDeliveryAttempt attempt) {
        histories.save(NotificationHistory.builder()
                .userId(delivery.getUserId())
                .courseKey(delivery.getOutbox().getCourseKey())
                .title(attempt.title())
                .message(attempt.body())
                .channel(delivery.getChannel())
                .build());
    }

    private void recordNotices(SeatNotificationDelivery delivery, SeatDeliveryAttempt attempt,
                               NotificationRecipient user, boolean deadLettered) {
        SeatNotificationOutbox outbox = delivery.getOutbox();
        NotificationChannel channel = delivery.getChannel();
        if (attempt.missingWebTarget() && policy.isChannelEnabled(user, channel) && !notices.hasUsableDevices(user)) {
            notices.recordMissingDevice(user, "seat:" + outbox.getId(), outbox.getCourseKey(), outbox.getCourseName());
        }
        for (SeatDeliveryAttempt.TargetFailure failure : attempt.failures()) {
            NotificationDevice device = failure.target().getDevice();
            String sourceKey = "seat:" + delivery.getId() + (device == null ? "" : ":" + device.id());
            notices.recordFailure(user, sourceKey, outbox.getCourseKey(), outbox.getCourseName(),
                    channel, failure.target(), failure.exception(), deadLettered);
        }
    }

    private SeatNotificationDeliveryClaim claimDelivery(SeatNotificationDelivery delivery, LocalDateTime now) {
        String claimToken = delivery.claim(now.plusSeconds(leaseSeconds));
        recordClaimLag(delivery, now);
        return new SeatNotificationDeliveryClaim(delivery.getId(), claimToken, now, delivery.getChannel());
    }

    private Optional<SeatNotificationDelivery> findClaimedForSettlement(SeatNotificationDeliveryClaim claim) {
        return deliveryRepository.findByIdForUpdate(claim.deliveryId())
                .filter(delivery -> delivery.isClaimedBy(claim.claimToken()));
    }

    private void updateOutboxTerminalStatus(SeatNotificationOutbox outbox) {
        SeatNotificationOutbox lockedOutbox = outboxRepository.findByIdForUpdate(outbox.getId())
                .orElseThrow(() -> new IllegalStateException("알림 발송 대기 항목을 찾을 수 없습니다. ID: " + outbox.getId()));
        List<SeatNotificationDeliveryStatus> statuses = deliveryRepository.findStatusesByOutboxId(outbox.getId());
        if (SeatNotificationDeliveryStatus.ACTIVE_STATUSES.stream().anyMatch(statuses::contains)) {
            return;
        }
        if (statuses.contains(SeatNotificationDeliveryStatus.DLQ)) {
            lockedOutbox.markDeadLettered();
            return;
        }
        lockedOutbox.markCompleted();
    }

    private void recordClaimLag(SeatNotificationDelivery delivery, LocalDateTime claimedAt) {
        if (delivery.getCreatedAt() == null) {
            return;
        }
        Timer timer = meterRegistry.timer(
                "notification.outbox.claim.lag", "channel", delivery.getChannel().name());
        if (timer != null) {
            timer.record(Duration.between(delivery.getCreatedAt(), claimedAt));
        }
    }

}
