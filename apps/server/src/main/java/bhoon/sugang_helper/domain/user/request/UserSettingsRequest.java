package bhoon.sugang_helper.domain.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "사용자 알림 설정 업데이트 요청 DTO")
public class UserSettingsRequest {

    @Email
    @Schema(description = "알림 수신용 이메일 주소", example = "notify@example.com")
    private String notificationEmail;

    @Schema(description = "이메일 알림 활성화 여부", example = "true")
    private boolean emailEnabled;

    @Schema(description = "웹 푸시 알림 활성화 여부", example = "true")
    private boolean webPushEnabled;

    @Schema(description = "FCM 알림 활성화 여부", example = "true")
    private boolean fcmEnabled;

    @Schema(description = "디스코드 알림 활성화 여부", example = "true")
    private boolean discordEnabled;

    public UserSettingsRequest(String notificationEmail, boolean emailEnabled, boolean webPushEnabled,
            boolean fcmEnabled, boolean discordEnabled) {
        this.notificationEmail = notificationEmail;
        this.emailEnabled = emailEnabled;
        this.webPushEnabled = webPushEnabled;
        this.fcmEnabled = fcmEnabled;
        this.discordEnabled = discordEnabled;
    }
}
