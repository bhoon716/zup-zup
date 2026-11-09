package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
    private UserNotificationQueryService userNotificationQuery;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("저장된 Outbox는 작업자가 재시작한 뒤에도 발송한다")
    void persistedOutboxIsDeliveredAfterAWorkerRestart() {
        // given
        SeatNotificationOutbox outbox = outboxRepository.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build());
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", null,
                true, true, true, "discord-id");
        when(subscriptionQueryService.findActiveRecipientIds("course-key")).thenReturn(List.of(1L));
        when(userNotificationQuery.findByIds(List.of(1L))).thenReturn(List.of(user));
        when(userNotificationQuery.findDevicesByUserIds(List.of(1L))).thenReturn(List.of(
                new NotificationDevice(1L, "web-endpoint", "p256", "auth")));

        processor.materializePendingOutboxes();
        List<SeatNotificationDeliveryClaim> deliveryClaims = processor.claimReadyDeliveries();
        deliveryClaims.forEach(processor::processDelivery);

        // when & then
        assertThat(deliveryClaims).hasSize(3)
                .extracting(SeatNotificationDeliveryClaim::channel)
                .containsExactlyInAnyOrder(NotificationChannel.EMAIL, NotificationChannel.WEB, NotificationChannel.DISCORD);
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
            return new NotificationProperties(true, true, true);
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
