package bhoon.sugang_helper.identity.user.dto.command;

public record VerifyEmailCommand(String email, String code) {
}
