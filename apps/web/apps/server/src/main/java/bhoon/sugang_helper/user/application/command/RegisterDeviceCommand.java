package bhoon.sugang_helper.user.application.command;

import bhoon.sugang_helper.user.domain.DeviceType;

public record RegisterDeviceCommand(
        DeviceType type,
        String token,
        String p256dh,
        String auth,
        String alias) {
}
