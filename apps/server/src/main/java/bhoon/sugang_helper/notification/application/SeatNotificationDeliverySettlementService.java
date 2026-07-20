package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * provider 호출 전후 DB 전이를 짧은 트랜잭션으로 분리하고 claim token으로 stale worker를 차단합니다.
 */
@Service
@RequiredArgsConstructor
public class SeatNotificationDeliverySettlementService {

    private static final List<SeatNotificationDeliveryStatus> ACTIVE_DELIVERY_STATUSES = List.of(
            SeatNotificationDeliveryStatus.PENDING, SeatNotificationDeliveryStatus.PROCESSING);

    private final SeatNotificationDeliveryJpaRepository deliveryRepository;
    private final SeatNotificationOutboxJpaRepository outboxRepository;

    @Transactional(readOnly = true)
    public SeatNotificationDeliveryDispatch loadForDispatch(SeatNotificationDeliveryClaim claim) {
        return deliveryRepository.findClaimedForDispatch(
                        claim.deliveryId(), SeatNotificationDeliveryStatus.PROCESSING, claim.claimToken())
                .map(delivery -> new SeatNotificationDeliveryDispatch(
                        delivery.getUserId(), delivery.getChannel(), delivery.getOutbox(), delivery.getIdempotencyKey()))
                .orElse(null);
    }

    @Transactional
    public boolean markSent(SeatNotificationDeliveryClaim claim) {
        SeatNotificationDelivery delivery = findClaimedForSettlement(claim);
        if (delivery == null) {
            return false;
        }
        delivery.markSent();
        updateOutboxTerminalStatus(delivery.getOutbox());
        return true;
    }

    @Transactional
    public SeatNotificationDeliveryFailureResult markFailure(SeatNotificationDeliveryClaim claim, String failureCode,
                                                              int maximumAttempts) {
        SeatNotificationDelivery delivery = findClaimedForSettlement(claim);
        if (delivery == null) {
            return SeatNotificationDeliveryFailureResult.stale();
        }
        LocalDateTime now = LocalDateTime.now();
        int nextAttempt = delivery.getAttempts() + 1;
        boolean deadLettered = delivery.markFailure(failureCode, maximumAttempts, now,
                now.plusSeconds(backoffSeconds(nextAttempt)));
        updateOutboxTerminalStatus(delivery.getOutbox());
        return new SeatNotificationDeliveryFailureResult(true, deadLettered, delivery.getAttempts());
    }

    private SeatNotificationDelivery findClaimedForSettlement(SeatNotificationDeliveryClaim claim) {
        return deliveryRepository.findByIdForUpdate(claim.deliveryId())
                .filter(delivery -> delivery.isClaimedBy(claim.claimToken()))
                .orElse(null);
    }

    private void updateOutboxTerminalStatus(SeatNotificationOutbox outbox) {
        SeatNotificationOutbox lockedOutbox = outboxRepository.findByIdForUpdate(outbox.getId())
                .orElseThrow(() -> new IllegalStateException("notification outbox not found: " + outbox.getId()));
        if (deliveryRepository.countByOutboxIdAndStatusIn(outbox.getId(), ACTIVE_DELIVERY_STATUSES) != 0) {
            return;
        }
        if (deliveryRepository.countByOutboxIdAndStatus(outbox.getId(), SeatNotificationDeliveryStatus.DLQ) > 0) {
            lockedOutbox.markDeadLettered();
        } else {
            lockedOutbox.markCompleted();
        }
    }

    private long backoffSeconds(int attempt) {
        return Math.min(300, 1L << Math.min(attempt - 1, 8));
    }
}
