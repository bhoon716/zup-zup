package bhoon.sugang_helper.user.application;

import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.application.result.UserOnboardingResult;
import bhoon.sugang_helper.user.application.result.UserProfileResult;
import bhoon.sugang_helper.user.application.result.UserProfileUpdateResult;
import bhoon.sugang_helper.user.application.result.UserSettingsResult;
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
        return fromValues(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getNotificationEmail(),
                user.isEmailEnabled(),
                user.isWebPushEnabled(),
                user.isFcmEnabled(),
                user.isOnboardingCompleted(),
                user.getDiscordId(),
                user.isDiscordEnabled());
    }

    public static UserResponse from(UserProfileResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserProfileUpdateResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserSettingsResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    public static UserResponse from(UserOnboardingResult result) {
        return fromValues(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.notificationEmail(),
                result.emailEnabled(),
                result.webPushEnabled(),
                result.fcmEnabled(),
                result.onboardingCompleted(),
                result.discordId(),
                result.discordEnabled());
    }

    private static UserResponse fromValues(
            Long id,
            String email,
            String name,
            String role,
            String notificationEmail,
            boolean emailEnabled,
            boolean webPushEnabled,
            boolean fcmEnabled,
            boolean onboardingCompleted,
            String discordId,
            boolean discordEnabled) {
        return UserResponse.builder()
                .id(id)
                .email(email)
                .name(name)
                .role(role)
                .notificationEmail(notificationEmail)
                .emailEnabled(emailEnabled)
                .webPushEnabled(webPushEnabled)
                .fcmEnabled(fcmEnabled)
                .discordEnabled(discordEnabled)
                .discordId(discordId)
                .onboardingCompleted(onboardingCompleted)
                .build();
    }
}
