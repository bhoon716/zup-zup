package bhoon.sugang_helper.domain.user.response;

import bhoon.sugang_helper.domain.user.entity.UserDevice;
import bhoon.sugang_helper.domain.user.enums.DeviceType;
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

        return UserDeviceResponse.builder()
                .id(device.getId())
                .type(device.getType())
                .alias(device.getAlias())
                .maskedToken(masked)
                .registeredAt(device.getCreatedAt())
                .build();
    }
}
