package bhoon.sugang_helper.identity.user.dto;

public record VerifyEmailCommand(String email, String code) {
}
