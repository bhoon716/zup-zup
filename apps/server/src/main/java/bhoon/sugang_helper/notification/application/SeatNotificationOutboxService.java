package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class SeatNotificationOutboxService {

    private final SeatNotificationOutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void enqueue(SeatOpenedEvent event) {
        outboxRepository.save(SeatNotificationOutbox.from(event));
    }
}
