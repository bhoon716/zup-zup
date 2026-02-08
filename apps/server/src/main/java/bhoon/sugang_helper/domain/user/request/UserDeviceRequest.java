package bhoon.sugang_helper.domain.user.request;

import bhoon.sugang_helper.domain.user.enums.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 기기 등록 요청 DTO")
public class UserDeviceRequest {

    @NotNull
    @Schema(description = "기기 타입 (FCM, WEB)", example = "FCM")
    private DeviceType type;

    @NotBlank
    @Schema(description = "푸시 토큰", example = "fcm-registration-token")
    private String token;

    @Schema(description = "P256DH (Web Push용)", example = "p256dh-key")
    private String p256dh;

    @Schema(description = "Auth (Web Push용)", example = "auth-key")
    private String auth;

    @Schema(description = "기기 별칭", example = "Chrome on macOS")
    private String alias;
}
