package bhoon.sugang_helper.identity.user.dto;

public record UpdateSettingsCommand(
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean fcmEnabled,
        boolean discordEnabled) {
}
