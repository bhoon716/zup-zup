package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryRetentionSchedulerTest {

    @Mock
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @Captor
    private ArgumentCaptor<LocalDateTime> cutoffCaptor;

    private NotificationDeliveryRetentionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationDeliveryRetentionScheduler(deliveryRepository);
    }

    @Test
    void clampsConfiguredRetentionToAtLeastThirtyDaysAndOnlyPurgesDlqDeliveries() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7L);
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(30).minusSeconds(1);

        scheduler.removeExpiredDeadLetters();

        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(30).plusSeconds(1);
        verify(deliveryRepository).deleteByStatusAndDeadLetteredAtBefore(
                org.mockito.ArgumentMatchers.eq(SeatNotificationDeliveryStatus.DLQ), cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBetween(earliestExpectedCutoff, latestExpectedCutoff);
    }
}
