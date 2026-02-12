package bhoon.sugang_helper.domain.user.response;

import bhoon.sugang_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "사용자 프로필 정보 응답 DTO")
public class UserResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "역할 (USER, ADMIN)", example = "USER")
    private String role;

    @Schema(description = "알림 수신 이메일")
    private String notificationEmail;

    @Schema(description = "이메일 알림 활성화 여부")
    private boolean emailEnabled;

    @Schema(description = "웹 푸시 알림 활성화 여부")
    private boolean webPushEnabled;

    @Schema(description = "FCM 알림 활성화 여부")
    private boolean fcmEnabled;

    @Schema(description = "온보딩(초기설정) 완료 여부")
    private boolean onboardingCompleted;

    @Schema(description = "디스코드 ID")
    private String discordId;

    @Schema(description = "디스코드 알림 활성화 여부")
    private boolean discordEnabled;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .notificationEmail(user.getNotificationEmail())
                .emailEnabled(user.isEmailEnabled())
                .webPushEnabled(user.isWebPushEnabled())
                .fcmEnabled(user.isFcmEnabled())
                .discordEnabled(user.isDiscordEnabled())
                .discordId(user.getDiscordId())
                .onboardingCompleted(user.isOnboardingCompleted())
                .build();
    }
}
