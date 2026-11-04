package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.admin.audit.service.AdminAuditService;
import bhoon.sugang_helper.common.config.MdcTaskDecorator;
import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.identity.user.dto.result.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.result.NotificationRecipient;
import bhoon.sugang_helper.identity.user.enums.NotificationDeviceType;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.client.NotificationProviderResilience;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryClaim;
import bhoon.sugang_helper.seatalert.notification.dto.result.SeatNotificationDeliveryDispatch;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RetiredFcmDeliveryCompatibilityTest {

    @Mock private SeatNotificationDeliveryRepository deliveries;
    @Mock private SeatNotificationOutboxRepository outboxes;
    @Mock private SubscriptionQueryService subscriptions;
    @Mock private UserNotificationQueryService users;
    @Mock private NotificationService notifications;
    @Mock private MeterRegistry metrics;
    private NotificationChannelPolicy policy;
    private SeatNotificationDeliverySettlementService settlement;
    private SeatNotificationOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        policy = new NotificationChannelPolicy(new NotificationProperties(true, true, true));
        settlement = new SeatNotificationDeliverySettlementService(deliveries, outboxes);
        processor = processor(settlement);
    }

    @Test
    void legacyFcmPreferencesAndDevicesCannotProduceTargetsOrNewDeliveries() {
        // given
        NotificationRecipient legacy = new NotificationRecipient(1L, false, "legacy@example.invalid", null,
                false, false, true, false, null);
        List<NotificationDevice> oldDevices = List.of(
                new NotificationDevice(1L, NotificationDeviceType.FCM, "legacy-token", null, null));
        SeatNotificationOutbox outbox = outbox();
        when(outboxes.findByStatusForUpdate(eq(SeatNotificationOutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(subscriptions.findActiveRecipientIds("course-key")).thenReturn(List.of(1L));
        when(users.findByIds(List.of(1L))).thenReturn(List.of(legacy));
        when(users.findDevicesByUserIds(List.of(1L))).thenReturn(oldDevices);

        // when
        processor.materializePendingOutboxes();

        // then
        assertThat(policy.isChannelEnabled(legacy, NotificationChannel.FCM)).isFalse();
        assertThat(policy.userEnabledChannels(legacy)).isEmpty();
        assertThat(policy.resolveTargets(legacy, oldDevices, NotificationChannel.FCM)).isEmpty();
        assertThat(outbox.getStatus()).isEqualTo(SeatNotificationOutboxStatus.COMPLETED);
        verify(deliveries, never()).saveAll(any());
        verify(deliveries).countByOutboxIdAndStatusInAndChannelNot(eq(10L), any(), eq(NotificationChannel.FCM));
        verifyNoInteractions(notifications);
    }

    @Test
    void claimExcludesAndDefensivelyIgnoresLegacyRowsWithoutChangingThem() {
        // given
        SeatNotificationDelivery legacy = delivery(NotificationChannel.FCM);
        SeatNotificationDelivery supported = delivery(NotificationChannel.EMAIL);
        when(deliveries.findReadyForUpdate(eq(SeatNotificationDeliveryStatus.PENDING),
                eq(SeatNotificationDeliveryStatus.PROCESSING), any(LocalDateTime.class),
                eq(NotificationChannel.FCM), any(Pageable.class))).thenReturn(List.of(legacy, supported));

        // when
        List<SeatNotificationDeliveryClaim> claims = processor.claimReadyDeliveries(5);

        // then
        assertThat(claims).hasSize(1).allSatisfy(claim -> assertThat(claim.channel()).isEqualTo(NotificationChannel.EMAIL));
        assertThat(legacy.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PENDING);
        assertThat(legacy.getClaimToken()).isNull();
        assertThat(legacy.getAttempts()).isEqualTo(2);
        assertThat(legacy.getLastError()).isEqualTo("N002");
    }

    @Test
    void legacyClaimCannotBeDispatchedOrSettledAsSuccessOrFailure() {
        // given
        SeatNotificationDelivery legacy = delivery(NotificationChannel.FCM);
        String token = legacy.claim(LocalDateTime.now().plusMinutes(1));
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, token);
        when(deliveries.findClaimedForDispatch(1L, SeatNotificationDeliveryStatus.PROCESSING, token))
                .thenReturn(Optional.of(legacy));
        when(deliveries.findByIdForUpdate(1L)).thenReturn(Optional.of(legacy));
        String key = legacy.getIdempotencyKey();

        // when & then
        assertThat(settlement.loadForDispatch(claim)).isNull();
        assertThat(settlement.markSent(claim)).isFalse();
        assertThat(settlement.markFailure(claim, "UNEXPECTED", 5).settled()).isFalse();
        assertThat(legacy.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PROCESSING);
        assertThat(legacy.getClaimToken()).isEqualTo(token);
        assertThat(legacy.getIdempotencyKey()).isEqualTo(key);
        assertThat(legacy.getAttempts()).isEqualTo(2);
        assertThat(legacy.getLastError()).isEqualTo("N002");
        verifyNoInteractions(outboxes, notifications);
    }

    @Test
    void unexpectedLegacyDispatchCannotBeReportedAsSent() {
        // given
        SeatNotificationDeliveryClaim claim = new SeatNotificationDeliveryClaim(1L, "claim");
        SeatNotificationDeliverySettlementService stub = mock(SeatNotificationDeliverySettlementService.class);
        when(stub.loadForDispatch(claim)).thenReturn(new SeatNotificationDeliveryDispatch(
                1L, NotificationChannel.FCM, outbox(), "legacy-key"));

        // when
        processor(stub).processDelivery(claim);

        // then
        verify(stub, never()).markSent(any());
        verify(stub, never()).markFailure(any(), any(), eq(5));
        verifyNoInteractions(notifications, metrics);
    }

    @ParameterizedTest
    @EnumSource(SeatNotificationDeliveryStatus.class)
    void legacyRowsCannotBeReplayedEvenWithForcedSentReplay(SeatNotificationDeliveryStatus status) {
        // given
        SeatNotificationDelivery legacy = delivery(NotificationChannel.FCM);
        ReflectionTestUtils.setField(legacy, "status", status);
        when(deliveries.findByIdForUpdate(1L)).thenReturn(Optional.of(legacy));
        AdminAuditService audit = mock(AdminAuditService.class);
        AdminNotificationDeliveryService admin = new AdminNotificationDeliveryService(
                deliveries, audit, mock(UserIdentityQueryService.class));
        String key = legacy.getIdempotencyKey();

        // when & then
        assertThatThrownBy(() -> admin.replay(2L, 1L, true)).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_DELIVERY_REPLAY_NOT_ALLOWED);
        assertThat(legacy.getStatus()).isEqualTo(status);
        assertThat(legacy.getIdempotencyKey()).isEqualTo(key);
        assertThat(legacy.getAttempts()).isEqualTo(2);
        assertThat(legacy.getLastError()).isEqualTo("N002");
        verifyNoInteractions(audit);
    }

    @Test
    void retiredProviderCannotExecuteAnOutboundOperation() {
        // given
        Runnable operation = mock(Runnable.class);
        try (NotificationProviderResilience resilience = new NotificationProviderResilience(
                new SimpleMeterRegistry(), 5_000, 3, 30, new MdcTaskDecorator())) {
            // when & then
            assertThatThrownBy(() -> resilience.execute(NotificationChannel.FCM, operation))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        }
        verifyNoInteractions(operation);
    }

    private SeatNotificationOutboxProcessor processor(SeatNotificationDeliverySettlementService currentSettlement) {
        SeatNotificationOutboxProcessor result = new SeatNotificationOutboxProcessor(
                outboxes, deliveries, subscriptions, users, policy, notifications, currentSettlement, metrics);
        ReflectionTestUtils.setField(result, "batchSize", 5);
        return result;
    }

    private SeatNotificationDelivery delivery(NotificationChannel channel) {
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(outbox()).userId(1L).channel(channel).build();
        ReflectionTestUtils.setField(delivery, "id", 1L);
        ReflectionTestUtils.setField(delivery, "attempts", 2);
        ReflectionTestUtils.setField(delivery, "lastError", "N002");
        return delivery;
    }

    private SeatNotificationOutbox outbox() {
        SeatNotificationOutbox outbox = SeatNotificationOutbox.builder()
                .courseKey("course-key").courseName("Course").previousSeats(0).currentSeats(1).build();
        ReflectionTestUtils.setField(outbox, "id", 10L);
        return outbox;
    }
}
