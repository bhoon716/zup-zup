package bhoon.sugang_helper.domain.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "온보딩(초기설정) 완료 요청")
public class OnboardingRequest {

    @Schema(description = "알림 수신 이메일", example = "notify@example.com")
    @NotBlank(message = "알림 수신 이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String notificationEmail;

    @Schema(description = "이메일 알림 수신 여부", example = "true")
    private boolean emailEnabled;

    @Schema(description = "웹 푸시 알림 수신 여부", example = "true")
    private boolean webPushEnabled;
}
