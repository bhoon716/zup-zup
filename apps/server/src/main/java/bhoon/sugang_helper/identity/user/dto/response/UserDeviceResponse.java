package bhoon.sugang_helper.identity.user.dto.response;

import bhoon.sugang_helper.identity.user.dto.result.UserDeviceResult;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDeviceResponse {

    private final Long id;

    private final String alias;

    private final String maskedToken;

    private final LocalDateTime registeredAt;

    public static UserDeviceResponse from(UserDevice device) {
        String maskedToken = maskToken(device.getToken());
        return new UserDeviceResponse(device.getId(), device.getAlias(), maskedToken, device.getCreatedAt());
    }

    public static UserDeviceResponse from(UserDeviceResult result) {
        return new UserDeviceResponse(result.id(), result.alias(), result.maskedToken(), result.registeredAt());
    }

    private static String maskToken(String token) {
        if (token.length() <= 10) {
            return token;
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
