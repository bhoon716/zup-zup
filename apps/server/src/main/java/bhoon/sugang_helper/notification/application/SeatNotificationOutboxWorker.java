package bhoon.sugang_helper.notification.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatNotificationOutboxWorker {

    private final SeatNotificationOutboxProcessor processor;

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-ms:5000}")
    public void processPendingNotifications() {
        processor.materializePendingOutboxes();
        for (Long deliveryId : processor.claimReadyDeliveryIds()) {
            processor.processDelivery(deliveryId);
        }
    }
}
