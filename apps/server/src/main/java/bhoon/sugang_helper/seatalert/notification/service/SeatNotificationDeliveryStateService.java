package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryDispatch;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryFailureResult;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
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

    private static final long MAX_RETRY_BACKOFF_SECONDS = 300;
    private static final int MAX_RETRY_BACKOFF_EXPONENT = 8;

    private final SeatNotificationDeliveryRepository deliveryRepository;
    private final SeatNotificationOutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

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
                .map(SeatNotificationDeliveryDispatch::from)
                .orElse(null);
    }

    @Transactional
    public boolean markSent(SeatNotificationDeliveryClaim claim) {
        Optional<SeatNotificationDelivery> claimedDelivery = findClaimedForSettlement(claim);
        if (claimedDelivery.isEmpty()) {
            return false;
        }
        SeatNotificationDelivery delivery = claimedDelivery.get();
        delivery.markSent();
        updateOutboxTerminalStatus(delivery.getOutbox());
        return true;
    }

    @Transactional
    public SeatNotificationDeliveryFailureResult markFailure(SeatNotificationDeliveryClaim claim, String failureCode,
                                                              int maximumAttempts) {
        Optional<SeatNotificationDelivery> claimedDelivery = findClaimedForSettlement(claim);
        if (claimedDelivery.isEmpty()) {
            return SeatNotificationDeliveryFailureResult.stale();
        }
        SeatNotificationDelivery delivery = claimedDelivery.get();
        LocalDateTime now = LocalDateTime.now();
        int nextAttempt = delivery.getAttempts() + 1;
        boolean deadLettered = delivery.markFailure(failureCode, maximumAttempts, now,
                now.plusSeconds(backoffSeconds(nextAttempt)));
        updateOutboxTerminalStatus(delivery.getOutbox());
        return new SeatNotificationDeliveryFailureResult(true, deadLettered, delivery.getAttempts());
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

    private long backoffSeconds(int attempt) {
        return Math.min(MAX_RETRY_BACKOFF_SECONDS, 1L << Math.min(attempt - 1, MAX_RETRY_BACKOFF_EXPONENT));
    }
}
