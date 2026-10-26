package bhoon.sugang_helper.seatalert.notification.scheduler;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;




@Component
@Slf4j
public class SeatNotificationOutboxWorker {

    private final SeatNotificationOutboxProcessor processor;
    private final int batchSize;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Map<NotificationChannel, AtomicInteger> inFlightByChannel = new EnumMap<>(NotificationChannel.class);
    private volatile boolean stopping;

    public SeatNotificationOutboxWorker(
            SeatNotificationOutboxProcessor processor,
            MeterRegistry meterRegistry,
            @Value("${app.notification.outbox.batch-size:50}") int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("notification batch size must be positive");
        }
        this.processor = processor;
        this.batchSize = batchSize;
        for (NotificationChannel channel : NotificationChannel.values()) {
            AtomicInteger inFlight = new AtomicInteger();
            inFlightByChannel.put(channel, inFlight);

            meterRegistry.gauge("notification.outbox.queue.depth", Tags.of("channel", channel.name()), inFlight);
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-ms:5000}")
    public void processPendingNotifications() {
        if (stopping || Thread.currentThread().isInterrupted() || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            processor.materializePendingOutboxes();
            for (int processed = 0; processed < batchSize; processed++) {
                if (stopping || Thread.currentThread().isInterrupted()) {
                    return;
                }
                List<SeatNotificationDeliveryClaim> claims = processor.claimReadyDeliveries(1);
                if (claims.isEmpty()) {
                    return;
                }
                process(claims.getFirst());
            }
        } finally {
            running.set(false);
        }
    }

    private void process(SeatNotificationDeliveryClaim claim) {
        AtomicInteger inFlight = claim.channel() == null ? null : inFlightByChannel.get(claim.channel());
        if (inFlight != null) {
            inFlight.incrementAndGet();
        }
        try {
            processor.processDelivery(claim);
        } catch (RuntimeException exception) {

            log.error("[Notification Outbox] 발송 작업 처리 실패. deliveryId={}, failureCode={}, exceptionType={}",
                    claim.deliveryId(), SensitiveDataRedactor.failureCode(exception),
                    SensitiveDataRedactor.exceptionType(exception), SensitiveDataRedactor.safeStackTrace(exception));
        } finally {
            if (inFlight != null) {
                inFlight.decrementAndGet();
            }
        }
    }

    @EventListener(ContextClosedEvent.class)
    @PreDestroy
    public void shutdown() {
        stopping = true;
    }
}
