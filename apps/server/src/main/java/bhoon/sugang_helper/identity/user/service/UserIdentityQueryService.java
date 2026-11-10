package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserAccountQuery;
import bhoon.sugang_helper.identity.api.users.UserAccountStatus;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.api.users.UserStatistics;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIdentityQueryService implements CurrentUserQuery, UserAccountQuery, UserStatistics {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public UserIdentity requireCurrentUser() {
        return findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<UserIdentity> findCurrentUser() {
        return Optional.ofNullable(SecurityUtil.getCurrentUserEmailOrNull()).flatMap(this::findByEmail);
    }

    private Optional<UserIdentity> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .map(user -> new UserIdentity(user.getId(), user.getRole() == Role.ADMIN));
    }

    @Override
    public Optional<UserIdentity> findById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserIdentity(user.getId(), user.getRole() == Role.ADMIN));
    }

    public Optional<AuthenticatedUser> findAuthenticatedUser(Long userId, String email) {
        return userRepository.findByIdAndEmailAndDeletedAtIsNull(userId, email)
                .map(user -> new AuthenticatedUser(user.getId(), user.getEmail(), user.getRoleKey()));
    }

    @Override
    public List<UserAccountStatus> findAccountStatuses(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserAccountStatus(user.getId(), user.isDeleted()))
                .toList();
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }
}
