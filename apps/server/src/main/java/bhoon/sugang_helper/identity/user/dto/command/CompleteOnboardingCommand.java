package bhoon.sugang_helper.identity.user.dto.command;

public record CompleteOnboardingCommand(
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean fcmEnabled,
        boolean discordEnabled) {
}
