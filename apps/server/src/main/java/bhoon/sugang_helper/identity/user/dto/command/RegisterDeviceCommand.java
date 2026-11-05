package bhoon.sugang_helper.identity.user.dto.command;

public record RegisterDeviceCommand(
        String token,
        String p256dh,
        String auth,
        String alias) {
}
