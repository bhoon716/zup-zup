package bhoon.sugang_helper.identity.user.application.command;

public record VerifyEmailCommand(String email, String code) {
}
