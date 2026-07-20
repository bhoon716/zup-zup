package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DLQ 기록은 실제 DLQ 진입 시각부터 최소 30일 동안 보존합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryRetentionScheduler {

    private static final long MINIMUM_RETENTION_DAYS = 30;

    private final SeatNotificationDeliveryJpaRepository deliveryRepository;

    @Value("${app.notification.outbox.dlq-retention-days:30}")
    private long retentionDays;

    @Scheduled(cron = "${app.notification.outbox.dlq-retention-cron:0 30 3 * * *}")
    @Transactional
    public void removeExpiredDeadLetters() {
        long effectiveRetentionDays = Math.max(MINIMUM_RETENTION_DAYS, retentionDays);
        long deletedCount = deliveryRepository.deleteByStatusAndDeadLetteredAtBefore(
                SeatNotificationDeliveryStatus.DLQ, LocalDateTime.now().minusDays(effectiveRetentionDays));
        log.info("[Notification Delivery] DLQ retention cleanup completed. deletedCount={}", deletedCount);
    }
}
