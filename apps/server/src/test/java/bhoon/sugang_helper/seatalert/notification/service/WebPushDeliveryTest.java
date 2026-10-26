package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.identity.user.dto.result.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.result.NotificationRecipient;
import bhoon.sugang_helper.identity.user.enums.NotificationDeviceType;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.client.NotificationProviderResilience;
import bhoon.sugang_helper.seatalert.notification.client.NotificationSender;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({SeatNotificationOutboxProcessor.class, SeatNotificationDeliverySettlementService.class,
        NotificationService.class, NotificationChannelPolicy.class, WebPushDeliveryTest.TestConfig.class})
class WebPushDeliveryTest {

    private static final String COURSE_KEY = "course-key";
    private static final String FIRST = "first-endpoint";
    private static final String SECOND = "second-endpoint";

    @Autowired private SeatNotificationOutboxProcessor processor;
    @Autowired private SeatNotificationOutboxRepository outboxes;
    @Autowired private SeatNotificationDeliveryRepository deliveries;
    @Autowired private NotificationHistoryRepository histories;
    @MockitoBean private SubscriptionQueryService subscriptions;
    @MockitoBean private UserNotificationQueryService users;
    @MockitoBean private RedisService redisService;
    @MockitoBean private NotificationSender sender;
    @MockitoBean private NotificationProviderResilience resilience;

    private final List<NotificationDevice> devices = new ArrayList<>();
    private final List<String> attemptedTargets = new ArrayList<>();
    private final List<String> attemptedKeys = new ArrayList<>();
    private SeatNotificationOutbox outbox;

    @BeforeEach
    void setUp() {
        deliveries.deleteAll();
        histories.deleteAll();
        outboxes.deleteAll();
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", null,
                false, true, false, false, null);
        when(subscriptions.findActiveRecipientIds(COURSE_KEY)).thenReturn(List.of(1L));
        when(users.findByIds(List.of(1L))).thenReturn(List.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findDevicesByUserIds(List.of(1L))).thenAnswer(ignored -> List.copyOf(devices));
        when(users.findDevicesByUserId(anyLong())).thenAnswer(ignored -> List.copyOf(devices));
        when(sender.supports(NotificationChannel.WEB)).thenReturn(true);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(resilience).execute(any(), any());
        outbox = outboxes.saveAndFlush(SeatNotificationOutbox.builder()
                .courseKey(COURSE_KEY).courseName("Course").previousSeats(0).currentSeats(1).build());
        devices.add(device(FIRST));
        devices.add(device(SECOND));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void expiredSubscriptionDoesNotPreventDeliveryToTheOtherDevice(boolean expiredFirst) {
        // Given: the real sender removes a 404/410 endpoint before reporting INVALID_RECIPIENT.
        String expired = expiredFirst ? FIRST : SECOND;
        stubSender(target -> {
            if (target.equals(expired)) {
                devices.removeIf(device -> device.token().equals(expired));
                throw invalidSubscription();
            }
        });

        // When
        processReadyDelivery();

        // Then
        assertThat(attemptedTargets).containsExactly(FIRST, SECOND);
        assertDelivery(SeatNotificationDeliveryStatus.SENT, SeatNotificationOutboxStatus.COMPLETED, 0);
        assertSuccessfulHistory();
        assertThat(processor.claimReadyDeliveries(1)).isEmpty();
    }

    @Test
    void allExpiredSubscriptionsKeepTheExistingPermanentFailureSettlement() {
        // Given
        stubSender(target -> {
            devices.removeIf(device -> device.token().equals(target));
            throw invalidSubscription();
        });

        // When
        processReadyDelivery();

        // Then
        assertThat(attemptedTargets).containsExactly(FIRST, SECOND);
        assertThat(devices).isEmpty();
        assertDelivery(SeatNotificationDeliveryStatus.DLQ, SeatNotificationOutboxStatus.DLQ, 1);
        assertThat(delivery().getLastError()).isEqualTo(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION.getCode());
        assertThat(histories.findAll()).isEmpty();
        assertThat(processor.claimReadyDeliveries(1)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void temporaryFailureKeepsWholeDeliveryRetryAndTheSameIdempotencyKey(int failedTargetIndex) {
        // Given: index 1 means the first device already succeeded before the second fails.
        String failed = failedTargetIndex == 0 ? FIRST : SECOND;
        stubSender(target -> {
            if (target.equals(failed)) {
                throw new NotificationProviderException(ErrorCode.WEB_PUSH_SEND_ERROR,
                        true, "OUTAGE", 503, null);
            }
        });

        // When
        processReadyDelivery();

        // Then
        assertDelivery(SeatNotificationDeliveryStatus.PENDING, SeatNotificationOutboxStatus.PROCESSING, 1);
        assertThat(histories.findAll()).isEmpty();
        assertThat(attemptedTargets).containsExactlyElementsOf(
                failedTargetIndex == 0 ? List.of(FIRST) : List.of(FIRST, SECOND));
        String key = delivery().getIdempotencyKey();
        SeatNotificationDelivery ready = delivery();
        // Advance only the due time; do not reset attempts or use the admin replay path.
        ReflectionTestUtils.setField(ready, "nextAttemptAt", LocalDateTime.now().minusSeconds(1));
        deliveries.saveAndFlush(ready);
        stubSender(target -> { });

        // When: the ordinary retry sends the whole current target list again.
        processReadyDelivery();

        // Then: a prior success may be repeated; per-device exactly-once is not introduced.
        assertThat(attemptedTargets).containsExactlyElementsOf(failedTargetIndex == 0
                ? List.of(FIRST, FIRST, SECOND) : List.of(FIRST, SECOND, FIRST, SECOND));
        assertThat(attemptedKeys).containsOnly(NotificationDeliveryIdempotencyKey.providerKey(key));
        assertDelivery(SeatNotificationDeliveryStatus.SENT, SeatNotificationOutboxStatus.COMPLETED, 1);
        assertSuccessfulHistory();
    }

    @Test
    void otherPermanentWebPushErrorsAreNotIgnored() {
        // Given
        stubSender(target -> {
            if (target.equals(SECOND)) {
                throw new NotificationProviderException(ErrorCode.WEB_PUSH_MISSING_KEYS,
                        false, "MISSING_KEYS", null, null);
            }
        });

        // When
        processReadyDelivery();

        // Then
        assertThat(attemptedTargets).containsExactly(FIRST, SECOND);
        assertDelivery(SeatNotificationDeliveryStatus.DLQ, SeatNotificationOutboxStatus.DLQ, 1);
        assertThat(histories.findAll()).isEmpty();
    }

    private void stubSender(Consumer<String> send) {
        doAnswer(invocation -> {
            String target = invocation.getArgument(0, NotificationTarget.class).getRecipient();
            attemptedTargets.add(target);
            attemptedKeys.add(invocation.getArgument(3));
            send.accept(target);
            return null;
        }).when(sender).send(any(), anyString(), anyString(), anyString());
    }

    private void processReadyDelivery() {
        processor.materializePendingOutboxes();
        var claims = processor.claimReadyDeliveries(1);
        assertThat(claims).hasSize(1);
        processor.processDelivery(claims.getFirst());
    }

    private SeatNotificationDelivery delivery() {
        assertThat(deliveries.findAll()).hasSize(1);
        return deliveries.findAll().getFirst();
    }

    private void assertDelivery(SeatNotificationDeliveryStatus deliveryStatus,
                                SeatNotificationOutboxStatus outboxStatus, int attempts) {
        assertThat(delivery().getStatus()).isEqualTo(deliveryStatus);
        assertThat(delivery().getAttempts()).isEqualTo(attempts);
        assertThat(outboxes.findById(outbox.getId())).get()
                .extracting(SeatNotificationOutbox::getStatus).isEqualTo(outboxStatus);
    }

    private void assertSuccessfulHistory() {
        assertThat(histories.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getUserId()).isEqualTo(1L);
            assertThat(history.getCourseKey()).isEqualTo(COURSE_KEY);
            assertThat(history.getChannel()).isEqualTo(NotificationChannel.WEB);
        });
    }

    private static NotificationDevice device(String endpoint) {
        return new NotificationDevice(1L, NotificationDeviceType.WEB, endpoint, "p256", "auth");
    }

    private static NotificationProviderException invalidSubscription() {
        return new NotificationProviderException(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION,
                false, "INVALID_RECIPIENT", 410, null);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean NotificationProperties notificationProperties() {
            return new NotificationProperties(true, true, true, true);
        }
        @Bean SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
