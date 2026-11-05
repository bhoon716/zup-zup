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
import bhoon.sugang_helper.identity.user.dto.command.UpdateProfileCommand;
import bhoon.sugang_helper.identity.user.dto.command.UpdateSettingsCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserOnboardingResult;
import bhoon.sugang_helper.identity.user.dto.result.UserProfileResult;
import bhoon.sugang_helper.identity.user.dto.result.UserProfileUpdateResult;
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
    public UserProfileUpdateResult updateProfile(UpdateProfileCommand command) {
        User user = getCurrentUser();
        user.update(command.name());
        log.info("[User] 프로필 수정. userId={}", user.getId());
        return UserProfileUpdateResult.from(user);
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
        log.info("[User] 알림 설정 변경. userId={}, emailEnabled={}, webPushEnabled={}",
                user.getId(), command.emailEnabled(), command.webPushEnabled());
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
        log.info("[User] 회원 탈퇴 처리. userId={}", userId);
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
        log.info("[User] 초기 설정 완료. userId={}, emailFingerprint={}", user.getId(),
                SensitiveDataRedactor.fingerprint(user.getEmail()));
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
        log.info("[User] Discord 계정 연결. userId={}, discordIdFingerprint={}", user.getId(),
                SensitiveDataRedactor.fingerprint(discordId));
    }

    @Transactional
    public void unlinkDiscord() {
        User user = getCurrentUser();
        user.unlinkDiscord();
        log.info("[User] Discord 계정 연결 해제. userId={}", user.getId());
    }

    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

}
