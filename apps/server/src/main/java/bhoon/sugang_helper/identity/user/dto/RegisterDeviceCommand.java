package bhoon.sugang_helper.identity.user.dto;

import bhoon.sugang_helper.identity.user.enums.DeviceType;

public record RegisterDeviceCommand(
        DeviceType type,
        String token,
        String p256dh,
        String auth,
        String alias) {
}
