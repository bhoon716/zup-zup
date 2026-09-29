package bhoon.sugang_helper.identity.user.application;

import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.contract.UserDashboardProfile;
import bhoon.sugang_helper.identity.user.contract.UserDashboardQuery;
import bhoon.sugang_helper.identity.user.domain.User;
import bhoon.sugang_helper.identity.user.domain.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDashboardQueryService implements UserDashboardQuery {

    private final UserRepository userRepository;

    @Override
    public Optional<UserDashboardProfile> findCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .map(UserDashboardQueryService::toProfile);
    }

    private static UserDashboardProfile toProfile(User user) {
        return new UserDashboardProfile(
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
}
