package bhoon.sugang_helper.identity.api.notifications;

public record NotificationDevice(
        Long userId,
        String token,
        String p256dh,
        String auth) {
}
