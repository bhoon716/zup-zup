package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.user.response.UserResponse;
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
    public void withdraw() {
        User user = getCurrentUser();

        // 구독 정보 함께 삭제
        subscriptionRepository.deleteAllByUserId(user.getId());

        userRepository.delete(user);
        log.info("[User] Account withdrawn: userId={}, email={}", user.getId(), user.getEmail());
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
