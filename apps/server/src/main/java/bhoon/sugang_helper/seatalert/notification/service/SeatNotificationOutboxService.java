package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.academic.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatNotificationOutboxService {

    private final SeatNotificationOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(SeatOpenedEvent event) {
        outboxRepository.save(SeatNotificationOutbox.from(event));
    }
}
