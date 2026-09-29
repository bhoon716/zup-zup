package bhoon.sugang_helper.identity.user.application.command;

public record CompleteOnboardingCommand(
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean fcmEnabled,
        boolean discordEnabled) {
}
