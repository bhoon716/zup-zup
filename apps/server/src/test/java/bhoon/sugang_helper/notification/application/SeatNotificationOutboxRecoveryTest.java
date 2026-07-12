package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutbox;
import bhoon.sugang_helper.notification.domain.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.notification.infra.SeatNotificationOutboxJpaRepository;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
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
@Import({SeatNotificationOutboxProcessor.class, NotificationChannelPolicy.class,
        SeatNotificationOutboxRecoveryTest.TestConfig.class})
class SeatNotificationOutboxRecoveryTest {

    @Autowired
    private SeatNotificationOutboxProcessor processor;
    @Autowired
    private SeatNotificationOutboxJpaRepository outboxRepository;
    @Autowired
    private SeatNotificationDeliveryJpaRepository deliveryRepository;
    @MockitoBean
    private SubscriptionRepository subscriptionRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserDeviceRepository userDeviceRepository;
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
        Subscription subscription = Subscription.builder().userId(1L).courseKey("course-key").isActive(true).build();
        User user = User.builder().id(1L).email("user@example.com").role(Role.USER).emailEnabled(true).build();
        when(subscriptionRepository.findByCourseKeyAndIsActiveTrue("course-key")).thenReturn(List.of(subscription));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));
        when(userDeviceRepository.findByUserIdIn(List.of(1L))).thenReturn(List.of());

        processor.materializePendingOutboxes();
        List<Long> deliveryIds = processor.claimReadyDeliveryIds();
        deliveryIds.forEach(processor::processDelivery);

        assertThat(deliveryIds).hasSize(1);
        assertThat(deliveryRepository.findAll()).allSatisfy(delivery ->
                assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.SENT));
        assertThat(outboxRepository.findById(outbox.getId())).get()
                .extracting(SeatNotificationOutbox::getStatus)
                .isEqualTo(SeatNotificationOutboxStatus.COMPLETED);
        verify(notificationService).deliverSeatOpening(any(), any(), any());
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
