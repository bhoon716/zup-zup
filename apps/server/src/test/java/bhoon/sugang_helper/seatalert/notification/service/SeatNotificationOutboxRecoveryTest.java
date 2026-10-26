package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.identity.user.dto.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.NotificationRecipient;
import bhoon.sugang_helper.identity.user.enums.NotificationDeviceType;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.identity.user.service.UserNotificationQuery;
import bhoon.sugang_helper.seatalert.notification.dto.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
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
    private SeatNotificationOutboxRepository outboxRepository;
    @Autowired
    private SeatNotificationDeliveryRepository deliveryRepository;
    @MockitoBean
    private SubscriptionQueryService subscriptionQueryService;
    @MockitoBean
    private UserNotificationQuery userNotificationQuery;
    @MockitoBean
    private NotificationService notificationService;

    @Test
    void persistedOutboxIsDeliveredAfterAWorkerRestart() {
        // Given: a persisted outbox survives the worker process.
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build());
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", null,
                true, true, true, true, "discord-id");
        when(subscriptionQueryService.findActiveRecipientIds("course-key")).thenReturn(List.of(1L));
        when(userNotificationQuery.findByIds(List.of(1L))).thenReturn(List.of(user));
        when(userNotificationQuery.findDevicesByUserIds(List.of(1L))).thenReturn(List.of(
                new NotificationDevice(1L, NotificationDeviceType.FCM, "fcm-token", null, null),
                new NotificationDevice(1L, NotificationDeviceType.WEB, "web-endpoint", "p256", "auth")));

        // When

        processor.materializePendingOutboxes();
        List<SeatNotificationDeliveryClaim> deliveryClaims = processor.claimReadyDeliveries();
        deliveryClaims.forEach(processor::processDelivery);

        // Then: each channel is materialized and settled once, and another poll has no work.
        assertThat(deliveryClaims).hasSize(4);
        assertThat(deliveryRepository.findAll())
                .extracting(SeatNotificationDelivery::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.values());
        assertThat(deliveryRepository.findAll()).allSatisfy(delivery ->
                assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.SENT));
        assertThat(processor.claimReadyDeliveries()).isEmpty();
        assertThat(outboxRepository.findById(outbox.getId())).get()
                .extracting(SeatNotificationOutbox::getStatus)
                .isEqualTo(SeatNotificationOutboxStatus.COMPLETED);
        for (NotificationChannel channel : NotificationChannel.values()) {
            verify(notificationService).deliverSeatOpening(eq(1L), eq(channel), any(), any());
        }
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
