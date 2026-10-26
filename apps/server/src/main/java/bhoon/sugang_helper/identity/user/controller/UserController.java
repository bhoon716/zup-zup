package bhoon.sugang_helper.identity.user.controller;

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
import bhoon.sugang_helper.identity.auth.service.AuthService;
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
    private final AuthService authSessionLogout;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String frontendBaseUrl;
    @Value("${app.discord.client-id}")
    private String discordClientId;
    @Value("${app.discord.redirect-uri}")
    private String discordRedirectUri;

    private static final String DISCORD_STATE_ATTRIBUTE = "DISCORD_OAUTH_STATE";
    private static final String DISCORD_RETURN_PATH_ATTRIBUTE = "DISCORD_OAUTH_RETURN_PATH";
    private static final String ONBOARDING_PATH = "/onboarding";




    @PostMapping("/onboard")
    public ResponseEntity<UserResponse> completeOnboarding(
            @Valid @RequestBody OnboardingRequest request) {
        UserOnboardingResult result = userService.completeOnboarding(new CompleteOnboardingCommand(
                request.getNotificationEmail(),
                request.isEmailEnabled(),
                request.isWebPushEnabled(),
                request.isFcmEnabled(),
                request.isDiscordEnabled()));
        UserResponse response = UserResponse.from(result);
        return ResponseEntity.ok(response);
    }




    @PostMapping("/email/code")
    public ResponseEntity<Void> sendVerificationCode(
            @Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
        userService.sendVerificationCode(new SendVerificationCodeCommand(request.getEmail()), resolveClientIp(httpRequest));
        return ResponseEntity.ok(null);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }




    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        userService.verifyEmail(new VerifyEmailCommand(request.getEmail(), request.getCode()));
        return ResponseEntity.ok(null);
    }




    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        UserProfileResult result = userService.getMyProfile();
        UserResponse response = UserResponse.from(result);
        return ResponseEntity.ok(response);
    }




    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        UserProfileUpdateResult result = userService.updateProfile(new UpdateProfileCommand(request.getName()));
        UserResponse response = UserResponse.from(result);
        return ResponseEntity.ok(response);
    }




    @PatchMapping("/settings")
    public ResponseEntity<UserResponse> updateSettings(
            @Valid @RequestBody UserSettingsRequest request) {
        UserSettingsResult result = userService.updateSettings(new UpdateSettingsCommand(
                request.getNotificationEmail(),
                request.isEmailEnabled(),
                request.isWebPushEnabled(),
                request.isFcmEnabled(),
                request.isDiscordEnabled()));
        UserResponse response = UserResponse.from(result);
        return ResponseEntity.ok(response);
    }




    @PostMapping("/notifications/test")
    public ResponseEntity<Void> sendTestNotification() {
        userService.sendTestNotification();
        return ResponseEntity.ok(null);
    }




    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(HttpServletRequest request, HttpServletResponse response) {
        userService.withdraw();
        authSessionLogout.logout(request, response);
        return ResponseEntity.ok(null);
    }




    @DeleteMapping("/me/discord")
    public ResponseEntity<Void> unlinkDiscord() {
        userService.unlinkDiscord();
        return ResponseEntity.ok(null);
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
            log.warn("[Discord] OAuth 연결 응답 처리 실패. failureCode={}, exceptionType={}",
                    "INVALID_INPUT", e.getClass().getSimpleName());
            return buildDiscordRedirectResponse(resolveDiscordRedirectPath(redirectPath), "error");
        }
    }




    private void linkDiscordAccount(String code) {
        String accessToken = discordOAuthService.exchangeCodeForToken(code);
        String discordId = discordOAuthService.getDiscordUserId(accessToken);
        userService.linkDiscordId(discordId);
    }




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




    private ResponseEntity<Void> buildDiscordRedirectResponse(String redirectPath, String result) {
        return ResponseEntity.status(302)
                .header("Location", frontendBaseUrl + redirectPath + "?discord=" + result)
                .build();
    }
}
