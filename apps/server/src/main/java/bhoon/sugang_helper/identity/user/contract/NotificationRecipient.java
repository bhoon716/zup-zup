package bhoon.sugang_helper.identity.user.contract;

public record NotificationRecipient(
        Long id,
        boolean deleted,
        String email,
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean fcmEnabled,
        boolean discordEnabled,
        String discordId) {
}
