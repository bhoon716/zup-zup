package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.api.users.UserDashboardProfiles;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.users.UserDashboardProfile;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDashboardQueryService implements UserDashboardProfiles {

    private final UserRepository userRepository;

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
                user.isOnboardingCompleted(),
                user.getDiscordId(),
                user.isDiscordEnabled());
    }
}
