package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(SeatNotificationOutboxService.class)
class SeatNotificationOutboxTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private SeatNotificationOutboxRepository outboxRepository;

    @Test
    void committedSeatOpeningTransactionPersistsAnOutboxRecord() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                new SeatOpenedEvent("course-key", "Course", "Professor", 0, 1)));

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void rolledBackSeatOpeningTransactionDoesNotPersistAnOutboxRecord() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new SeatOpenedEvent("course-key", "Course", "Professor", 0, 1));
            status.setRollbackOnly();
        });

        assertThat(outboxRepository.count()).isZero();
    }
}
