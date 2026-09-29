package bhoon.sugang_helper.identity.user.application.command;

import bhoon.sugang_helper.identity.user.domain.DeviceType;

public record RegisterDeviceCommand(
        DeviceType type,
        String token,
        String p256dh,
        String auth,
        String alias) {
}
