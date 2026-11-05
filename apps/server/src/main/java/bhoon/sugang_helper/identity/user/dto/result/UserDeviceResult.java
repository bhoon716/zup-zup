package bhoon.sugang_helper.identity.user.dto.result;

import bhoon.sugang_helper.identity.user.entity.UserDevice;
import java.time.LocalDateTime;

public record UserDeviceResult(
        Long id,
        String alias,
        String maskedToken,
        LocalDateTime registeredAt) {

    public static UserDeviceResult from(UserDevice device) {
        String token = device.getToken();
        String masked = token.length() > 10 ? token.substring(0, 5) + "..." + token.substring(token.length() - 5)
                : token;

        return new UserDeviceResult(
                device.getId(),
                device.getAlias(),
                masked,
                device.getCreatedAt());
    }
}
