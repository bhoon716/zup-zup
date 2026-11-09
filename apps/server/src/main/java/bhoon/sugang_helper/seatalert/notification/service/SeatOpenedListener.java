package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.academic.events.SeatOpenedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatOpenedListener {

    private final SeatNotificationOutboxService outboxService;

    @EventListener
    public void on(SeatOpenedEvent event) {
        outboxService.enqueue(event);
    }
}
