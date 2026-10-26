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

/**
 * 한 건을 선점해 처리한 뒤 다음 건을 가져옵니다. 대기 중인 작업의 lease를 미리 시작하지 않습니다.
 */
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
            // 기존 지표 이름을 유지하며 현재 처리 중인 한 건만 집계합니다.
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
            // 정산까지 실패한 건도 PROCESSING lease 만료 후 기존 복구 경로가 다시 가져옵니다.
            log.error("[Notification Outbox] Delivery processing failed. deliveryId={}, failureCode={}",
                    claim.deliveryId(), SensitiveDataRedactor.failureCode(exception));
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
