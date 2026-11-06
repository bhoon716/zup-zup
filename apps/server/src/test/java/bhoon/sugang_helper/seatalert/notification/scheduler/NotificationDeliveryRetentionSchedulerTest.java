package bhoon.sugang_helper.seatalert.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryRetentionSchedulerTest {

    @Mock
    private SeatNotificationDeliveryRepository deliveryRepository;

    @Captor
    private ArgumentCaptor<LocalDateTime> cutoffCaptor;

    private NotificationDeliveryRetentionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationDeliveryRetentionScheduler(deliveryRepository);
    }

    @Test
    @DisplayName("보존 기간을 최소 30일로 제한하고 DLQ 발송만 정리한다")
    void clampsConfiguredRetentionToAtLeastThirtyDaysAndOnlyPurgesDlqDeliveries() {
        // given
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7L);
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(30).minusSeconds(1);

        // when
        scheduler.removeExpiredDeadLetters();

        // then
        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(30).plusSeconds(1);
        verify(deliveryRepository).deleteByStatusAndDeadLetteredAtBefore(
                ArgumentMatchers.eq(SeatNotificationDeliveryStatus.DLQ), cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBetween(earliestExpectedCutoff, latestExpectedCutoff);
    }
}
