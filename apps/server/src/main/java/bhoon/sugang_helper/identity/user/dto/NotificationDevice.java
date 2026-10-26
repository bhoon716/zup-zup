package bhoon.sugang_helper.identity.user.dto;

import bhoon.sugang_helper.identity.user.enums.NotificationDeviceType;

public record NotificationDevice(
        Long userId,
        NotificationDeviceType type,
        String token,
        String p256dh,
        String auth) {
}
