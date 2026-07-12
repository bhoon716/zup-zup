package bhoon.sugang_helper.user.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import bhoon.sugang_helper.notification.application.NotificationService;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.review.domain.CourseEmojiReviewRepository;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.timetable.domain.Timetable;
import bhoon.sugang_helper.timetable.domain.TimetableRepository;
import bhoon.sugang_helper.user.application.command.CompleteOnboardingCommand;
import bhoon.sugang_helper.user.application.command.SendVerificationCodeCommand;
import bhoon.sugang_helper.user.application.command.UpdateProfileCommand;
import bhoon.sugang_helper.user.application.command.UpdateSettingsCommand;
import bhoon.sugang_helper.user.application.command.VerifyEmailCommand;
import bhoon.sugang_helper.user.application.result.UserOnboardingResult;
import bhoon.sugang_helper.user.application.result.UserProfileResult;
import bhoon.sugang_helper.user.application.result.UserProfileUpdateResult;
import bhoon.sugang_helper.user.application.result.UserSettingsResult;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import bhoon.sugang_helper.wishlist.domain.WishlistRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스입니다. 가입 상태 조회, 프로필 수정, 온보딩 완료, 디스코드 연동 등을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final TimetableRepository timetableRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseEmojiReviewRepository courseEmojiReviewRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackAttachmentRepository feedbackAttachmentRepository;
    private final LocalFileUploadService fileUploadService;
    private final WishlistRepository wishlistRepository;
    private final EmailVerificationService emailVerificationService;
    private final NotificationService notificationService;

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     */
    public UserProfileResult getMyProfile() {
        User user = getCurrentUser();
        return UserProfileResult.from(user);
    }

    /**
     * 현재 인증된 사용자가 있으면 반환하고, 없으면 비워 둡니다.
     */
    public Optional<User> getCurrentUserOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }

    /**
     * 사용자의 이름을 수정하고 변경된 프로필 정보를 반환합니다.
     */
    @Transactional
    public UserProfileUpdateResult updateProfile(UpdateProfileCommand command) {
        User user = getCurrentUser();
        user.update(command.name());
        log.info("[User] Update profile: userId={}", user.getId());
        return UserProfileUpdateResult.from(user);
    }

    /**
     * 사용자의 알림 설정을 수정하고 변경된 정보를 반환합니다.
     */
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

    /**
     * 회원 탈퇴 처리를 진행하며, 사용자 소유 데이터를 함께 정리합니다.
     */
    @Transactional
    public void withdraw() {
        User user = getCurrentUser();
        Long userId = user.getId();

        deleteUserOwnedData(userId);
        userRepository.delete(user);
        log.info("[User] Delete account: userId={}, emailMasked={}", user.getId(),
                SensitiveDataRedactor.maskEmail(user.getEmail()));
    }

    /**
     * 회원 탈퇴 시 사용자 소유 데이터를 함께 정리합니다.
     */
    private void deleteUserOwnedData(Long userId) {
        subscriptionRepository.deleteAllByUserId(userId);
        userDeviceRepository.deleteAllByUserId(userId);
        courseReviewRepository.deleteAllByUserId(userId);
        courseEmojiReviewRepository.deleteAllByUserId(userId);
        notificationHistoryRepository.deleteAllByUserId(userId);
        fileUploadService.deleteFilesAfterTransactionCommit(feedbackAttachmentRepository.findAllByFeedbackUserId(userId)
                .stream()
                .map(FeedbackAttachment::getFileUrl)
                .toList());
        feedbackRepository.deleteAllByUserId(userId);
        wishlistRepository.deleteAllByUserId(userId);

        List<Timetable> timetables = timetableRepository.findByUserId(userId);
        for (Timetable timetable : timetables) {
            timetableRepository.delete(timetable);
        }
    }

    /**
     * 신규 가입 유저의 온보딩 절차를 완료합니다.
     */
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

    /**
     * 입령된 이메일로 인증 코드를 발송합니다.
     */
    @Transactional
    public void sendVerificationCode(SendVerificationCodeCommand command) {
        sendVerificationCode(command, null);
    }

    @Transactional
    public void sendVerificationCode(SendVerificationCodeCommand command, String clientIp) {
        User user = getCurrentUser();
        emailVerificationService.sendCode(user.getId(), command.email(), clientIp);
    }

    /**
     * 이메일 인증 코드를 검증합니다.
     */
    @Transactional
    public void verifyEmail(VerifyEmailCommand command) {
        User user = getCurrentUser();
        boolean verified = emailVerificationService.verifyCode(user.getId(), command.email(), command.code());
        if (!verified) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }

    /**
     * 디스코드 계정을 현재 사용자 계정과 연동합니다.
     */
    @Transactional
    public void linkDiscordId(String discordId) {
        User user = getCurrentUser();
        user.linkDiscord(discordId);
        log.info("[User] Link Discord: userId={}, discordIdFingerprint={}", user.getId(),
                SensitiveDataRedactor.fingerprint(discordId));
    }

    /**
     * 연동된 디스코드 계정을 해제합니다.
     */
    @Transactional
    public void unlinkDiscord() {
        User user = getCurrentUser();
        user.unlinkDiscord();
        log.info("[User] Unlink Discord: userId={}", user.getId());
    }

    /**
     * 현재 활성화된 채널로 테스트 알림을 발송합니다.
     */
    @Transactional
    public void sendTestNotification() {
        User user = getCurrentUser();
        List<NotificationChannel> channels = user.getEnabledNotificationChannels();
        if (channels.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.");
        }

        notificationService.sendUserTestNotification(user, channels);
        log.info("[User] Send test notification: userId={}, channels={}", user.getId(), channels);
    }

    /**
     * 현재 인증된 사용자 정보를 컨텍스트에서 조회합니다.
     */
    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
