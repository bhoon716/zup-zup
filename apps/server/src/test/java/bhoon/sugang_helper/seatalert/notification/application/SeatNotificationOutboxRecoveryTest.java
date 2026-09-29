package bhoon.sugang_helper.seatalert.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.seatalert.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.domain.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.seatalert.notification.infra.SeatNotificationOutboxJpaRepository;
import bhoon.sugang_helper.seatalert.subscription.contract.SubscriptionRecipientQuery;
import bhoon.sugang_helper.identity.user.contract.NotificationRecipient;
import bhoon.sugang_helper.identity.user.contract.UserNotificationQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({SeatNotificationOutboxProcessor.class, SeatNotificationDeliverySettlementService.class,
        NotificationChannelPolicy.class,
        SeatNotificationOutboxRecoveryTest.TestConfig.class})
class SeatNotificationOutboxRecoveryTest {

    @Autowired
    private SeatNotificationOutboxProcessor processor;
    @Autowired
    private SeatNotificationOutboxJpaRepository outboxRepository;
    @Autowired
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @MockitoBean
    private SubscriptionRecipientQuery subscriptionRecipientQuery;
    @MockitoBean
    private UserNotificationQuery userNotificationQuery;
    @MockitoBean
    private NotificationService notificationService;

    @Test
    void persistedOutboxIsDeliveredAfterAWorkerRestart() {
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build());
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", null,
                true, false, false, false, null);
        when(subscriptionRecipientQuery.findActiveRecipientIds("course-key")).thenReturn(List.of(1L));
        when(userNotificationQuery.findByIds(List.of(1L))).thenReturn(List.of(user));
        when(userNotificationQuery.findDevicesByUserIds(List.of(1L))).thenReturn(List.of());

        processor.materializePendingOutboxes();
        List<SeatNotificationDeliveryClaim> deliveryClaims = processor.claimReadyDeliveries();
        deliveryClaims.forEach(processor::processDelivery);

        assertThat(deliveryClaims).hasSize(1);
        assertThat(deliveryRepository.findAll()).allSatisfy(delivery ->
                assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.SENT));
        assertThat(outboxRepository.findById(outbox.getId())).get()
                .extracting(SeatNotificationOutbox::getStatus)
                .isEqualTo(SeatNotificationOutboxStatus.COMPLETED);
        verify(notificationService).deliverSeatOpening(any(), any(), any(), any());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        NotificationProperties notificationProperties() {
            return new NotificationProperties(true, true, true, true);
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
