package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.support.feedback.service.FeedbackAccountLifecycleService;
import bhoon.sugang_helper.seatalert.notification.service.NotificationService;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionAccountLifecycleService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.common.security.jwt.UserAccessRevocationService;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.common.util.SecurityUtil;
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
    private final SubscriptionAccountLifecycleService subscriptionAccountLifecycle;
    private final UserDeviceRepository userDeviceRepository;
    private final FeedbackAccountLifecycleService feedbackAccountLifecycle;
    private final EmailVerificationService emailVerificationService;
    private final NotificationService notificationTestSender;
    private final JwtProvider jwtProvider;
    private final UserAccessRevocationService userAccessRevocationService;




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
        log.info("[User] Update profile: userId={}", user.getId());
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
                command.fcmEnabled(),
                command.discordEnabled());
        log.info("[User] Change settings: userId={}, emailEnabled={}, webPushEnabled={}",
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
        jwtProvider.revokeAllRefreshTokens(email);
        userAccessRevocationService.revoke(userId);
        emailVerificationService.clearVerificationState(userId, email, notificationEmail);
        subscriptionAccountLifecycle.cancelAllForUser(userId);
        userDeviceRepository.deleteAllByUserId(userId);
        feedbackAccountLifecycle.deleteAllForUser(userId);
        log.info("[User] Soft-deleted account: userId={}", userId);
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
                command.fcmEnabled(),
                command.discordEnabled());
        log.info("[User] Onboarding complete: userId={}, emailMasked={}", user.getId(),
                SensitiveDataRedactor.maskEmail(user.getEmail()));
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
        log.info("[User] Link Discord: userId={}, discordIdFingerprint={}", user.getId(),
                SensitiveDataRedactor.fingerprint(discordId));
    }




    @Transactional
    public void unlinkDiscord() {
        User user = getCurrentUser();
        user.unlinkDiscord();
        log.info("[User] Unlink Discord: userId={}", user.getId());
    }




    @Transactional
    public void sendTestNotification() {
        User user = getCurrentUser();
        if (!user.hasEnabledNotificationChannels()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.");
        }

        notificationTestSender.sendUserTestNotification(user.getId());
    }




    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
