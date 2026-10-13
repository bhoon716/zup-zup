package bhoon.sugang_helper.seatalert.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatNotificationOutboxServiceTest {

    @Mock
    private SeatNotificationOutboxRepository outboxRepository;
    @InjectMocks
    private SeatNotificationOutboxService outboxService;

    @Test
    void seatOpeningIsPersistedAsAnOutboxRecordBeforeTheCourseTransactionCommits() {
        SeatOpenedEvent event = new SeatOpenedEvent("course-key", "Course", "Professor", 0, 1);

        outboxService.enqueue(event);

        verify(outboxRepository).save(any(SeatNotificationOutbox.class));
    }
}
