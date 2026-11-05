package bhoon.sugang_helper.identity.user.dto.result;

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
