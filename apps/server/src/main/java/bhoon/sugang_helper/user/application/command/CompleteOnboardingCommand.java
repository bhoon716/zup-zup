package bhoon.sugang_helper.user.application.command;

public record CompleteOnboardingCommand(
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean fcmEnabled,
        boolean discordEnabled) {
}
