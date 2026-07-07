package bhoon.sugang_helper.user.application.result;

import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.domain.UserDevice;
import java.time.LocalDateTime;

public record UserDeviceResult(
        Long id,
        DeviceType type,
        String alias,
        String maskedToken,
        LocalDateTime registeredAt) {

    public static UserDeviceResult from(UserDevice device) {
        String token = device.getToken();
        String masked = token.length() > 10 ? token.substring(0, 5) + "..." + token.substring(token.length() - 5)
                : token;

        return new UserDeviceResult(
                device.getId(),
                device.getType(),
                device.getAlias(),
                masked,
                device.getCreatedAt());
    }
}
