package bhoon.sugang_helper.seatalert.notification.dto.response;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import java.time.LocalDateTime;




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
