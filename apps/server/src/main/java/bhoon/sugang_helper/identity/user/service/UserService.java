package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.auth.service.SessionInvalidator;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.command.CompleteOnboardingCommand;
import bhoon.sugang_helper.identity.user.dto.command.SendVerificationCodeCommand;
import bhoon.sugang_helper.identity.user.dto.command.UpdateSettingsCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserOnboardingResult;
import bhoon.sugang_helper.identity.user.dto.result.UserProfileResult;
import bhoon.sugang_helper.identity.user.dto.result.UserSettingsResult;
import bhoon.sugang_helper.identity.user.dto.command.VerifyEmailCommand;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String EVENT_KEY = "event";
    private static final String USER_ID_KEY = "userId";

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserDeviceRepository userDeviceRepository;
    private final EmailVerificationService emailVerificationService;
    private final SessionInvalidator sessionInvalidator;

    public UserProfileResult getMyProfile() {
        User user = getCurrentUser();
        return UserProfileResult.from(user);
    }

    public Optional<User> getCurrentUserOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted());
    }

    @Transactional
    public UserSettingsResult updateSettings(UpdateSettingsCommand command) {
        User user = getCurrentUser();

        String newEmail = command.notificationEmail();
        if (newEmail != null && !newEmail.equals(user.getEmail()) && !newEmail.equals(user.getNotificationEmail())) {
            if (!emailVerificationService.isVerified(user.getId(), newEmail)) {
                throw new CustomException(ErrorCode.UNVERIFIED_EMAIL);
            }
        }

        user.updateSettings(
                command.notificationEmail(),
                command.emailEnabled(),
                command.webPushEnabled(),
                command.discordEnabled());
        logUserNotificationSettingsUpdated(user.getId(), command.emailEnabled(), command.webPushEnabled());
        return UserSettingsResult.from(user);
    }

    @Transactional
    public void withdraw() {
        User user = getCurrentUser();
        Long userId = user.getId();
        String email = user.getEmail();
        String notificationEmail = user.getNotificationEmail();

        user.withdraw();
        userRepository.saveAndFlush(user);
        sessionInvalidator.invalidateAll(userId, email);
        emailVerificationService.clearVerificationState(userId, email, notificationEmail);
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
        userDeviceRepository.deleteAllByUserId(userId);
        logUserWithdrawn(userId);
    }

    @Transactional
    public UserOnboardingResult completeOnboarding(CompleteOnboardingCommand command) {
        User user = getCurrentUser();
        String newEmail = command.notificationEmail();

        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (!emailVerificationService.isVerified(user.getId(), newEmail)) {
                throw new CustomException(ErrorCode.UNVERIFIED_EMAIL);
            }
        }

        user.completeOnboarding(
                newEmail,
                command.emailEnabled(),
                command.webPushEnabled(),
                command.discordEnabled());
        logUserOnboardingCompleted(user.getId(), SensitiveDataRedactor.fingerprint(user.getEmail()));
        return UserOnboardingResult.from(user);
    }

    @Transactional
    public void sendVerificationCode(SendVerificationCodeCommand command) {
        sendVerificationCode(command, null);
    }

    @Transactional
    public void sendVerificationCode(SendVerificationCodeCommand command, String clientIp) {
        User user = getCurrentUser();
        emailVerificationService.sendCode(user.getId(), command.email(), clientIp);
    }

    @Transactional
    public void verifyEmail(VerifyEmailCommand command) {
        User user = getCurrentUser();
        boolean verified = emailVerificationService.verifyCode(user.getId(), command.email(), command.code());
        if (!verified) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }

    @Transactional
    public void linkDiscordId(String discordId) {
        User user = getCurrentUser();
        user.linkDiscord(discordId);
        logUserDiscordLinked(user.getId(), SensitiveDataRedactor.fingerprint(discordId));
    }

    @Transactional
    public void unlinkDiscord() {
        User user = getCurrentUser();
        user.unlinkDiscord();
        logUserDiscordUnlinked(user.getId());
    }

    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private void logUserNotificationSettingsUpdated(Long userId, boolean emailEnabled, boolean webPushEnabled) {
        log.atInfo().addKeyValue(EVENT_KEY, "user_notification_settings_updated")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("emailEnabled", emailEnabled)
                .addKeyValue("webPushEnabled", webPushEnabled)
                .log("[User] 알림 설정 변경.");
    }

    private void logUserWithdrawn(Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "user_withdrawn")
                .addKeyValue(USER_ID_KEY, userId)
                .log("[User] 회원 탈퇴 처리.");
    }

    private void logUserOnboardingCompleted(Long userId, String emailFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "user_onboarding_completed")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("emailFingerprint", emailFingerprint)
                .log("[User] 초기 설정 완료.");
    }

    private void logUserDiscordLinked(Long userId, String discordIdFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "user_discord_linked")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("discordIdFingerprint", discordIdFingerprint)
                .log("[User] Discord 계정 연결.");
    }

    private void logUserDiscordUnlinked(Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "user_discord_unlinked")
                .addKeyValue(USER_ID_KEY, userId)
                .log("[User] Discord 계정 연결 해제.");
    }

}
