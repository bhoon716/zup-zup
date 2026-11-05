package bhoon.sugang_helper.identity.user.dto.result;

public record NotificationDevice(
        Long userId,
        String token,
        String p256dh,
        String auth) {
}
