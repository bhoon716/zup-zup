package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.notification.infra.NotificationChannel;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Claims deliveries in FIFO order and processes only a bounded number at once.
 * The permit is acquired before claiming, so an overloaded worker leaves rows pending rather than losing them.
 */
@Component
public class SeatNotificationOutboxWorker {

    private final SeatNotificationOutboxProcessor processor;
    private final ThreadPoolExecutor executor;
    private final Semaphore capacity;
    private final Map<NotificationChannel, AtomicInteger> queueDepths = new EnumMap<>(NotificationChannel.class);

    @Autowired
    public SeatNotificationOutboxWorker(
            SeatNotificationOutboxProcessor processor,
            MeterRegistry meterRegistry,
            @Value("${app.notification.outbox.worker-threads:8}") int workerThreads,
            @Value("${app.notification.outbox.queue-capacity:32}") int queueCapacity) {
        this(processor, meterRegistry, workerThreads, queueCapacity,
                new ThreadPoolExecutor(workerThreads, workerThreads, 0L, TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(queueCapacity), new ThreadPoolExecutor.AbortPolicy()));
    }

    SeatNotificationOutboxWorker(SeatNotificationOutboxProcessor processor, MeterRegistry meterRegistry,
                                 int workerThreads, int queueCapacity, ThreadPoolExecutor executor) {
        if (workerThreads < 1 || queueCapacity < 1) {
            throw new IllegalArgumentException("notification worker capacity must be positive");
        }
        this.processor = processor;
        this.executor = executor;
        this.capacity = new Semaphore(workerThreads + queueCapacity);
        for (NotificationChannel channel : NotificationChannel.values()) {
            AtomicInteger depth = new AtomicInteger();
            queueDepths.put(channel, depth);
            meterRegistry.gauge("notification.outbox.queue.depth", Tags.of("channel", channel.name()), depth);
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-ms:5000}")
    public void processPendingNotifications() {
        processor.materializePendingOutboxes();
        int available = capacity.availablePermits();
        if (available == 0) {
            return;
        }
        List<SeatNotificationDeliveryClaim> claims = processor.claimReadyDeliveries(available);
        for (SeatNotificationDeliveryClaim claim : claims) {
            submit(claim);
        }
    }

    private void submit(SeatNotificationDeliveryClaim claim) {
        capacity.acquireUninterruptibly();
        AtomicInteger depth = claim.channel() == null ? null : queueDepths.get(claim.channel());
        try {
            if (depth != null) {
                depth.incrementAndGet();
            }
            AtomicInteger metricDepth = depth;
            executor.execute(() -> {
                try {
                    processor.processDelivery(claim);
                } finally {
                    capacity.release();
                    if (metricDepth != null) {
                        metricDepth.decrementAndGet();
                    }
                }
            });
        } catch (RejectedExecutionException exception) {
            capacity.release();
            if (depth != null) {
                depth.decrementAndGet();
            }
            throw exception;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
