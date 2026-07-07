package bhoon.sugang_helper.user.application.command;

public record UpdateSettingsCommand(
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean fcmEnabled,
        boolean discordEnabled) {
}
