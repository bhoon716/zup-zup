package bhoon.sugang_helper.notification.presentation;

/**
 * SENT 재처리는 기본적으로 막고, 운영자가 명시적으로 override한 경우에만 허용합니다.
 */
public record NotificationDeliveryReplayRequest(boolean forceSentReplay) {
}
