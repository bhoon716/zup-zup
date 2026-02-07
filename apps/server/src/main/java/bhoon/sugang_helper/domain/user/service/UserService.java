package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import bhoon.sugang_helper.domain.user.request.EmailRequest;
import bhoon.sugang_helper.domain.user.request.EmailVerificationRequest;
import bhoon.sugang_helper.domain.user.request.UserSettingsRequest;
import bhoon.sugang_helper.domain.user.request.OnboardingRequest;
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
    private final SubscriptionRepository subscriptionRepository;
    private final EmailVerificationService emailVerificationService;

    public UserResponse getMyProfile() {
        User user = getCurrentUser();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(String name) {
        User user = getCurrentUser();
        user.update(name);
        log.info("[User] Profile updated: userId={}, newName={}", user.getId(), name);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateSettings(UserSettingsRequest request) {
        User user = getCurrentUser();

        String newEmail = request.getNotificationEmail();
        if (newEmail != null && !newEmail.equals(user.getEmail()) && !newEmail.equals(user.getNotificationEmail())) {
            if (!emailVerificationService.isVerified(user.getId(), newEmail)) {
                throw new CustomException(ErrorCode.UNVERIFIED_EMAIL);
            }
        }

        user.updateSettings(
                request.getNotificationEmail(),
                request.isEmailEnabled(),
                request.isWebPushEnabled(),
                request.isFcmEnabled());
        log.info("[User] Settings updated: userId={}, emailEnabled={}, webPushEnabled={}",
                user.getId(), request.isEmailEnabled(), request.isWebPushEnabled());
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw() {
        User user = getCurrentUser();

        // 구독 정보 함께 삭제
        subscriptionRepository.deleteAllByUserId(user.getId());

        userRepository.delete(user);
        log.info("[User] Account withdrawn: userId={}, email={}", user.getId(), user.getEmail());
    }

    @Transactional
    public UserResponse completeOnboarding(OnboardingRequest request) {
        User user = getCurrentUser();
        String newEmail = request.getNotificationEmail();

        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (!emailVerificationService.isVerified(user.getId(), newEmail)) {
                throw new CustomException(ErrorCode.UNVERIFIED_EMAIL);
            }
        }

        user.completeOnboarding(
                newEmail,
                request.isEmailEnabled(),
                request.isWebPushEnabled());
        log.info("[User] Onboarding completed: userId={}, email={}", user.getId(), user.getEmail());
        return UserResponse.from(user);
    }

    @Transactional
    public void sendVerificationCode(EmailRequest request) {
        User user = getCurrentUser();
        emailVerificationService.sendCode(user.getId(), request.getEmail());
    }

    @Transactional
    public void verifyEmail(EmailVerificationRequest request) {
        User user = getCurrentUser();
        boolean verified = emailVerificationService.verifyCode(user.getId(), request.getEmail(), request.getCode());
        if (!verified) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
