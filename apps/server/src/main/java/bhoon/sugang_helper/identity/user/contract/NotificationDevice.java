package bhoon.sugang_helper.identity.user.contract;

public record NotificationDevice(
        Long userId,
        Type type,
        String token,
        String p256dh,
        String auth) {

    public enum Type {
        FCM,
        WEB
    }
}
