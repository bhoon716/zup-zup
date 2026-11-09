package bhoon.sugang_helper.seatalert.notification.scheduler;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeatNotificationBacklogMonitor {

    private final SeatNotificationDeliveryRepository deliveryRepository;
    private final SeatNotificationOutboxRepository outboxRepository;
    private final Map<NotificationChannel, AtomicLong> depths = new EnumMap<>(NotificationChannel.class);
    private final Map<NotificationChannel, AtomicLong> oldestReadyTimestamps = new EnumMap<>(NotificationChannel.class);
    private final AtomicLong materializationPending = new AtomicLong(-1);
    private final AtomicLong lastSuccessTimestamp = new AtomicLong();
    private final AtomicLong healthy = new AtomicLong();

    public SeatNotificationBacklogMonitor(SeatNotificationDeliveryRepository deliveryRepository,
                                         SeatNotificationOutboxRepository outboxRepository,
                                         MeterRegistry registry) {
        this.deliveryRepository = deliveryRepository;
        this.outboxRepository = outboxRepository;
        for (NotificationChannel channel : NotificationChannel.values()) {
            AtomicLong depth = new AtomicLong(-1);
            AtomicLong oldestReady = new AtomicLong();
            depths.put(channel, depth);
            oldestReadyTimestamps.put(channel, oldestReady);
            Tags tags = Tags.of("channel", channel.name());
            registry.gauge("notification.outbox.queue.depth", tags, depth);
            registry.gauge("notification.outbox.oldest.ready.timestamp.seconds", tags, oldestReady);
        }
        registry.gauge("notification.outbox.materialization.pending", materializationPending);
        registry.gauge("notification.outbox.monitor.last.success.timestamp.seconds", lastSuccessTimestamp);
        registry.gauge("notification.outbox.monitor.healthy", healthy);
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.monitor-ms:60000}", scheduler = "monitoringTaskScheduler")
    public void refresh() {
        try {
            List<SeatNotificationDeliveryRepository.ReadyBacklog> backlogs = deliveryRepository.findReadyBacklog(
                    SeatNotificationDeliveryStatus.PENDING, SeatNotificationDeliveryStatus.PROCESSING,
                    LocalDateTime.now());
            long pending = outboxRepository.countByStatus(SeatNotificationOutboxStatus.PENDING);
            depths.values().forEach(value -> value.set(0));
            oldestReadyTimestamps.values().forEach(value -> value.set(0));
            for (SeatNotificationDeliveryRepository.ReadyBacklog backlog : backlogs) {
                depths.get(backlog.getChannel()).set(backlog.getDepth());
                oldestReadyTimestamps.get(backlog.getChannel()).set(
                        backlog.getOldestReadyAt().atZone(ZoneId.systemDefault()).toEpochSecond());
            }
            materializationPending.set(pending);
            lastSuccessTimestamp.set(Instant.now().getEpochSecond());
            healthy.set(1);
        } catch (RuntimeException exception) {
            healthy.set(0);
            depths.values().forEach(value -> value.set(-1));
            oldestReadyTimestamps.values().forEach(value -> value.set(0));
            materializationPending.set(-1);
            logBacklogCheckFailed(exception);
        }
    }

    private void logBacklogCheckFailed(RuntimeException exception) {
        log.atError().addKeyValue("event", "notification_backlog_check_failed")
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(exception))
                .log("[Notification Outbox] 대기열 상태 확인 실패.");
    }

}
