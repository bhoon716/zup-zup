package bhoon.sugang_helper.identity.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.identity.user.dto.command.CompleteOnboardingCommand;
import bhoon.sugang_helper.identity.user.dto.command.SendVerificationCodeCommand;
import bhoon.sugang_helper.identity.user.dto.command.UpdateProfileCommand;
import bhoon.sugang_helper.identity.user.dto.command.UpdateSettingsCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserOnboardingResult;
import bhoon.sugang_helper.identity.user.dto.result.UserProfileResult;
import bhoon.sugang_helper.identity.user.dto.result.UserProfileUpdateResult;
import bhoon.sugang_helper.identity.user.dto.result.UserSettingsResult;
import bhoon.sugang_helper.identity.user.dto.command.VerifyEmailCommand;
import bhoon.sugang_helper.identity.user.dto.request.EmailRequest;
import bhoon.sugang_helper.identity.user.dto.request.EmailVerificationRequest;
import bhoon.sugang_helper.identity.user.dto.request.OnboardingRequest;
import bhoon.sugang_helper.identity.user.dto.request.UserSettingsRequest;
import bhoon.sugang_helper.identity.user.dto.request.UserUpdateRequest;
import bhoon.sugang_helper.identity.user.dto.response.UserResponse;
import bhoon.sugang_helper.identity.user.service.DiscordOAuthService;
import bhoon.sugang_helper.identity.user.service.UserService;
import bhoon.sugang_helper.identity.user.service.UserSessionLogout;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DiscordOAuthService discordOAuthService;
    private final UserSessionLogout authSessionLogout;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String frontendBaseUrl;
    @Value("${app.discord.client-id}")
    private String discordClientId;
    @Value("${app.discord.redirect-uri}")
    private String discordRedirectUri;

    private static final String DISCORD_STATE_ATTRIBUTE = "DISCORD_OAUTH_STATE";
    private static final String DISCORD_RETURN_PATH_ATTRIBUTE = "DISCORD_OAUTH_RETURN_PATH";
    private static final String ONBOARDING_PATH = "/onboarding";

    /**
     * 신규 가입 유저의 초기 설정(알림 이메일 등)을 저장하고 온보딩 상태를 완료로 변경합니다.
     */
    @PostMapping("/onboard")
    public ResponseEntity<CommonResponse<UserResponse>> completeOnboarding(
            @Valid @RequestBody OnboardingRequest request) {
        UserOnboardingResult result = userService.completeOnboarding(new CompleteOnboardingCommand(
                request.getNotificationEmail(),
                request.isEmailEnabled(),
                request.isWebPushEnabled(),
                request.isFcmEnabled(),
                request.isDiscordEnabled()));
        UserResponse response = UserResponse.from(result);
        return CommonResponse.ok(response, "온보딩 설정이 완료되었습니다.");
    }

    /**
     * 입력한 이메일로 인증 코드를 전송합니다.
     */
    @PostMapping("/email/code")
    public ResponseEntity<CommonResponse<Void>> sendVerificationCode(
            @Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
        userService.sendVerificationCode(new SendVerificationCodeCommand(request.getEmail()), resolveClientIp(httpRequest));
        return CommonResponse.ok(null, "인증 코드가 전송되었습니다. 이메일을 확인해주세요.");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 이메일로 전송된 인증 코드를 검증합니다.
     */
    @PostMapping("/email/verify")
    public ResponseEntity<CommonResponse<Void>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        userService.verifyEmail(new VerifyEmailCommand(request.getEmail(), request.getCode()));
        return CommonResponse.ok(null, "이메일 인증이 완료되었습니다.");
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getMyProfile() {
        UserProfileResult result = userService.getMyProfile();
        UserResponse response = UserResponse.from(result);
        return CommonResponse.ok(response, "사용자 프로필 정보입니다.");
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
     */
    @PatchMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        UserProfileUpdateResult result = userService.updateProfile(new UpdateProfileCommand(request.getName()));
        UserResponse response = UserResponse.from(result);
        return CommonResponse.ok(response, "사용자 프로필 정보가 수정되었습니다.");
    }

    /**
     * 현재 로그인한 사용자의 알림 수신 설정을 수정합니다.
     */
    @PatchMapping("/settings")
    public ResponseEntity<CommonResponse<UserResponse>> updateSettings(
            @Valid @RequestBody UserSettingsRequest request) {
        UserSettingsResult result = userService.updateSettings(new UpdateSettingsCommand(
                request.getNotificationEmail(),
                request.isEmailEnabled(),
                request.isWebPushEnabled(),
                request.isFcmEnabled(),
                request.isDiscordEnabled()));
        UserResponse response = UserResponse.from(result);
        return CommonResponse.ok(response, "사용자 알림 설정이 수정되었습니다.");
    }

    /**
     * 현재 로그인한 사용자의 활성화된 채널로 테스트 알림을 발송합니다.
     */
    @PostMapping("/notifications/test")
    public ResponseEntity<CommonResponse<Void>> sendTestNotification() {
        userService.sendTestNotification();
        return CommonResponse.ok(null, "알림 테스트를 전송했습니다.");
    }

    /**
     * 사용자 계정을 삭제하고 회원 탈퇴를 처리합니다.
     */
    @DeleteMapping("/me")
    public ResponseEntity<CommonResponse<Void>> withdraw(HttpServletRequest request, HttpServletResponse response) {
        userService.withdraw();
        authSessionLogout.logout(request, response);
        return CommonResponse.ok(null, "회원 탈퇴가 완료되었습니다.");
    }

    /**
     * 현재 계정에 연동된 디스코드 계정을 해제합니다.
     */
    @DeleteMapping("/me/discord")
    public ResponseEntity<CommonResponse<Void>> unlinkDiscord() {
        userService.unlinkDiscord();
        return CommonResponse.ok(null, "디스코드 연동이 해제되었습니다.");
    }

    @GetMapping("/discord/authorize")
    public ResponseEntity<Void> startDiscordAuthorization(@RequestParam(defaultValue = "/settings") String returnPath,
                                                           HttpSession session) {
        String safeReturnPath = ONBOARDING_PATH.equals(returnPath) ? ONBOARDING_PATH : "/settings";
        String state = createState();
        session.setAttribute(DISCORD_STATE_ATTRIBUTE, state);
        session.setAttribute(DISCORD_RETURN_PATH_ATTRIBUTE, safeReturnPath);

        String location = "https://discord.com/api/oauth2/authorize?client_id=" + discordClientId
                + "&redirect_uri=" + URLEncoder.encode(discordRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code&scope=identify%20applications.commands&integration_type=1&state=" + state;
        return ResponseEntity.status(302).header("Location", location).build();
    }

    /**
     * 디스코드 인증 후 리다이렉트되어 연동을 완료하는 콜백 엔드포인트입니다.
     */
    @GetMapping("/discord/callback")
    public ResponseEntity<Void> discordCallback(@RequestParam(name = "code", required = false) String code,
                                                @RequestParam(name = "state", required = false) String state,
                                                @RequestParam(name = "error", required = false) String error,
                                                HttpSession session) {
        String redirectPath = (String) session.getAttribute(DISCORD_RETURN_PATH_ATTRIBUTE);
        String expectedState = (String) session.getAttribute(DISCORD_STATE_ATTRIBUTE);
        session.removeAttribute(DISCORD_STATE_ATTRIBUTE);
        session.removeAttribute(DISCORD_RETURN_PATH_ATTRIBUTE);

        try {
            if (error != null && !error.isBlank()) {
                throw new IllegalArgumentException("Discord OAuth provider rejected request");
            }
            if (code == null || code.isBlank() || state == null || state.isBlank()) {
                throw new IllegalArgumentException("Discord OAuth callback is missing required parameters");
            }
            if (expectedState == null || !java.security.MessageDigest.isEqual(
                    expectedState.getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid Discord OAuth state");
            }
            linkDiscordAccount(code);
            return buildDiscordRedirectResponse(resolveDiscordRedirectPath(redirectPath), "success");
        } catch (Exception e) {
            log.warn("[Discord] OAuth callback failed. failureCode={}, exceptionType={}",
                    "INVALID_INPUT", e.getClass().getSimpleName());
            return buildDiscordRedirectResponse(resolveDiscordRedirectPath(redirectPath), "error");
        }
    }

    /**
     * 디스코드 인증 코드를 사용하여 계정을 연동합니다.
     */
    private void linkDiscordAccount(String code) {
        String accessToken = discordOAuthService.exchangeCodeForToken(code);
        String discordId = discordOAuthService.getDiscordUserId(accessToken);
        userService.linkDiscordId(discordId);
    }

    /**
     * 디스코드 연동 시도 시의 상태값에 따른 리다이렉트 경로를 결정합니다.
     */
    private String resolveDiscordRedirectPath(String state) {
        if ("onboarding".equals(state) || ONBOARDING_PATH.equals(state)) {
            return ONBOARDING_PATH;
        }
        return "/settings";
    }

    private String createState() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 디스코드 연동 결과와 경로를 포함한 리다이렉트 응답을 생성합니다.
     */
    private ResponseEntity<Void> buildDiscordRedirectResponse(String redirectPath, String result) {
        return ResponseEntity.status(302)
                .header("Location", frontendBaseUrl + redirectPath + "?discord=" + result)
                .build();
    }
}
