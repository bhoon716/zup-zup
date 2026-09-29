package bhoon.sugang_helper.seatalert.notification.application;

import bhoon.sugang_helper.academic.course.contract.SeatOpenedEvent;
import bhoon.sugang_helper.seatalert.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.domain.SeatNotificationOutboxRepository;
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
