package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.dto.result.UserAccountStatus;
import bhoon.sugang_helper.identity.user.dto.result.UserIdentity;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIdentityQueryService {

    private final UserRepository userRepository;

    public Optional<UserIdentity> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .map(user -> new UserIdentity(user.getId(), user.getRole() == Role.ADMIN));
    }

    public Optional<UserIdentity> findById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserIdentity(user.getId(), user.getRole() == Role.ADMIN));
    }

    public Optional<AuthenticatedUser> findAuthenticatedUser(Long userId, String email) {
        return userRepository.findByIdAndEmailAndDeletedAtIsNull(userId, email)
                .map(user -> new AuthenticatedUser(user.getId(), user.getEmail(), user.getRoleKey()));
    }

    public List<UserAccountStatus> findAccountStatuses(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserAccountStatus(user.getId(), user.isDeleted()))
                .toList();
    }

    public long countUsers() {
        return userRepository.count();
    }
}
