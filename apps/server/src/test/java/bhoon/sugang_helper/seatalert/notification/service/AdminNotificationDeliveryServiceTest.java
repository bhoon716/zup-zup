package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.admin.audit.service.AdminAuditService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.response.AdminNotificationDeliveryResponse;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminNotificationDeliveryServiceTest {

    @Mock
    private SeatNotificationDeliveryRepository deliveryRepository;
    @Mock
    private AdminAuditService adminAuditRecorder;
    @InjectMocks
    private AdminNotificationDeliveryService service;

    private static final Long ADMIN_ID = 7L;

    @Test
    void replayingDlqDeliveryKeepsItsKeyAndWritesOneAuditLog() {
        SeatNotificationDelivery delivery = dlqDelivery();
        String idempotencyKey = delivery.getIdempotencyKey();
        when(deliveryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivery));

        AdminNotificationDeliveryResponse response = service.replay(ADMIN_ID, 1L, false);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.idempotencyKeyRetained()).isTrue();
        assertThat(delivery.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isZero();
        verify(adminAuditRecorder).recordDeliveryReplay(ADMIN_ID, 1L, "DLQ", 1);
    }

    @Test
    void sentDeliveryCannotBeReplayedWithoutAnExplicitOverride() {
        SeatNotificationDelivery delivery = sentDelivery();
        when(deliveryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.replay(ADMIN_ID, 1L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_DELIVERY_REPLAY_NOT_ALLOWED);

        assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.SENT);
        verify(adminAuditRecorder, never()).recordDeliveryReplay(ADMIN_ID, 1L, "SENT", 0);
    }

    @Test
    void sentDeliveryCanBeReplayedOnlyWithAnExplicitOverride() {
        SeatNotificationDelivery delivery = sentDelivery();
        String idempotencyKey = delivery.getIdempotencyKey();
        when(deliveryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivery));

        service.replay(ADMIN_ID, 1L, true);

        assertThat(delivery.getStatus()).isEqualTo(SeatNotificationDeliveryStatus.PENDING);
        assertThat(delivery.getIdempotencyKey()).isEqualTo(idempotencyKey);
        verify(adminAuditRecorder).recordDeliveryReplay(ADMIN_ID, 1L, "SENT", 0);
    }

    private SeatNotificationDelivery dlqDelivery() {
        SeatNotificationDelivery delivery = pendingDelivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 13, 14, 0);
        delivery.claim(now.plusMinutes(1));
        delivery.markFailure("N001", 1, now, now.plusSeconds(1));
        return delivery;
    }

    private SeatNotificationDelivery sentDelivery() {
        SeatNotificationDelivery delivery = pendingDelivery();
        delivery.claim(LocalDateTime.of(2026, 7, 13, 14, 1));
        delivery.markSent();
        return delivery;
    }

    private SeatNotificationDelivery pendingDelivery() {
        SeatNotificationOutbox outbox = SeatNotificationOutbox.builder()
                .courseKey("course-key")
                .courseName("Course")
                .previousSeats(0)
                .currentSeats(1)
                .build();
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder()
                .outbox(outbox)
                .userId(1L)
                .channel(NotificationChannel.EMAIL)
                .build();
        ReflectionTestUtils.setField(delivery, "id", 1L);
        return delivery;
    }
}
