package bhoon.sugang_helper.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxRepository;
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
