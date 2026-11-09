package bhoon.sugang_helper.identity.api.notifications;

public record NotificationRecipient(
        Long id,
        boolean deleted,
        String email,
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean discordEnabled,
        String discordId) {
}
