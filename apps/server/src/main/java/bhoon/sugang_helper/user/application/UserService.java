package bhoon.sugang_helper.user.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.application.NotificationService;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.review.domain.CourseEmojiReviewRepository;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.timetable.domain.Timetable;
import bhoon.sugang_helper.timetable.domain.TimetableRepository;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import bhoon.sugang_helper.wishlist.domain.WishlistRepository;
import bhoon.sugang_helper.user.presentation.EmailRequest;
import bhoon.sugang_helper.user.presentation.EmailVerificationRequest;
import bhoon.sugang_helper.user.presentation.OnboardingRequest;
import bhoon.sugang_helper.user.presentation.UserSettingsRequest;
import bhoon.sugang_helper.user.presentation.UserResponse;
import java.util.ArrayList;
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
    private final WishlistRepository wishlistRepository;
    private final EmailVerificationService emailVerificationService;
    private final NotificationService notificationService;

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     */
    public UserResponse getMyProfile() {
        User user = getCurrentUser();
        return UserResponse.from(user);
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
    public UserResponse updateProfile(String name) {
        User user = getCurrentUser();
        user.update(name);
        log.info("[User] Update profile: userId={}, newName={}", user.getId(), name);
        return UserResponse.from(user);
    }

    /**
     * 사용자의 알림 설정을 수정하고 변경된 정보를 반환합니다.
     */
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
                request.isFcmEnabled(),
                request.isDiscordEnabled());
        log.info("[User] Change settings: userId={}, emailEnabled={}, webPushEnabled={}",
                user.getId(), request.isEmailEnabled(), request.isWebPushEnabled());
        return UserResponse.from(user);
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
        log.info("[User] Delete account: userId={}, email={}", user.getId(), user.getEmail());
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
                request.isWebPushEnabled(),
                request.isFcmEnabled(),
                request.isDiscordEnabled());
        log.info("[User] Onboarding complete: userId={}, email={}", user.getId(), user.getEmail());
        return UserResponse.from(user);
    }

    /**
     * 입령된 이메일로 인증 코드를 발송합니다.
     */
    @Transactional
    public void sendVerificationCode(EmailRequest request) {
        User user = getCurrentUser();
        emailVerificationService.sendCode(user.getId(), request.getEmail());
    }

    /**
     * 이메일 인증 코드를 검증합니다.
     */
    @Transactional
    public void verifyEmail(EmailVerificationRequest request) {
        User user = getCurrentUser();
        boolean verified = emailVerificationService.verifyCode(user.getId(), request.getEmail(), request.getCode());
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
        log.info("[User] Link Discord: userId={}, discordId={}", user.getId(), discordId);
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
        List<NotificationChannel> channels = getEnabledNotificationChannels(user);
        if (channels.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.");
        }

        notificationService.sendUserTestNotification(user, channels);
        log.info("[User] Send test notification: userId={}, channels={}", user.getId(), channels);
    }

    /**
     * 사용자가 활성화한 알림 채널 목록을 조회합니다.
     */
    private List<NotificationChannel> getEnabledNotificationChannels(User user) {
        List<NotificationChannel> channels = new ArrayList<>();
        if (user.isEmailEnabled()) {
            channels.add(NotificationChannel.EMAIL);
        }
        if (user.isFcmEnabled()) {
            channels.add(NotificationChannel.FCM);
        }
        if (user.isWebPushEnabled()) {
            channels.add(NotificationChannel.WEB);
        }
        if (user.isDiscordEnabled()) {
            channels.add(NotificationChannel.DISCORD);
        }
        return channels;
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
