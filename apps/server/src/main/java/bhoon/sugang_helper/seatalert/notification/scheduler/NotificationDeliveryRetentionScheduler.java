package bhoon.sugang_helper.seatalert.notification.scheduler;

import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;




@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryRetentionScheduler {

    private static final long MINIMUM_RETENTION_DAYS = 30;

    private final SeatNotificationDeliveryRepository deliveryRepository;

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
