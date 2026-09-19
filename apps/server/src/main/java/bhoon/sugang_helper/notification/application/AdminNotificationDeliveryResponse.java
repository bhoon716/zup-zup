package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import java.time.LocalDateTime;

/**
 * 관리자 운영 화면에 필요한 delivery 상태만 제공하며 수신자와 원본 idempotency key는 제외합니다.
 */
public record AdminNotificationDeliveryResponse(
        Long id,
        Long outboxId,
        String courseKey,
        String courseName,
        NotificationChannel channel,
        SeatNotificationDeliveryStatus status,
        int attempts,
        String lastError,
        String lastErrorReason,
        LocalDateTime deadLetteredAt,
        boolean idempotencyKeyRetained) {

    private static final String UNKNOWN_FAILURE_REASON = "기타 수신 실패";

    public static AdminNotificationDeliveryResponse from(SeatNotificationDelivery delivery) {
        return new AdminNotificationDeliveryResponse(
                delivery.getId(),
                delivery.getOutbox().getId(),
                delivery.getOutbox().getCourseKey(),
                delivery.getOutbox().getCourseName(),
                delivery.getChannel(),
                delivery.getStatus(),
                delivery.getAttempts(),
                delivery.getLastError(),
                resolveLastErrorReason(delivery.getLastError()),
                delivery.getDeadLetteredAt(),
                delivery.getIdempotencyKey() != null && !delivery.getIdempotencyKey().isBlank());
    }

    private static String resolveLastErrorReason(String lastError) {
        if (lastError == null) {
            return null;
        }
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode().equals(lastError)) {
                return errorCode.getMessage();
            }
        }
        return UNKNOWN_FAILURE_REASON;
    }
}
