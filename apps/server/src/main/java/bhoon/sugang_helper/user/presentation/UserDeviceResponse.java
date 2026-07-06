package bhoon.sugang_helper.user.presentation;

import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.application.result.UserDeviceResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 기기 정보 응답")
public class UserDeviceResponse {

    @Schema(description = "기기 ID", example = "1")
    private final Long id;

    @Schema(description = "기기 타입", example = "WEB")
    private final DeviceType type;

    @Schema(description = "기기 별칭", example = "Chrome on macOS")
    private final String alias;

    @Schema(description = "기기 토큰 (일부 마스킹)", example = "fcm_token_...")
    private final String maskedToken;

    @Schema(description = "등록 일시")
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
