package bhoon.sugang_helper.identity.user.dto.response;

import bhoon.sugang_helper.identity.user.dto.UserDeviceResult;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDeviceResponse {

    private final Long id;

    private final DeviceType type;

    private final String alias;

    private final String maskedToken;

    private final LocalDateTime registeredAt;

    public static UserDeviceResponse from(UserDevice device) {
        String token = device.getToken();
        String masked = token.length() > 10 ? token.substring(0, 5) + "..." + token.substring(token.length() - 5)
                : token;

        return fromValues(device.getId(), device.getType(), device.getAlias(), masked, device.getCreatedAt());
    }

    public static UserDeviceResponse from(UserDeviceResult result) {
        return fromValues(result.id(), result.type(), result.alias(), result.maskedToken(), result.registeredAt());
    }

    private static UserDeviceResponse fromValues(Long id, DeviceType type, String alias, String maskedToken,
                                                 LocalDateTime registeredAt) {
        return UserDeviceResponse.builder()
                .id(id)
                .type(type)
                .alias(alias)
                .maskedToken(maskedToken)
                .registeredAt(registeredAt)
                .build();
    }
}
