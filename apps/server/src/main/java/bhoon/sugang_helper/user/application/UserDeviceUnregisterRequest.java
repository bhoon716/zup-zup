package bhoon.sugang_helper.user.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 기기 해제 요청 DTO")
public record UserDeviceUnregisterRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "해제할 푸시 토큰", example = "fcm-registration-token")
        String token) {
}
