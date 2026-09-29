package bhoon.sugang_helper.identity.user.application;

import bhoon.sugang_helper.identity.user.contract.UserIdentity;
import bhoon.sugang_helper.identity.user.contract.UserIdentityQuery;
import bhoon.sugang_helper.identity.user.contract.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.domain.Role;
import bhoon.sugang_helper.identity.user.domain.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIdentityQueryService implements UserIdentityQuery {

    private final UserRepository userRepository;

    @Override
    public Optional<UserIdentity> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .map(user -> new UserIdentity(user.getId(), user.getRole() == Role.ADMIN));
    }

    @Override
    public Optional<UserIdentity> findById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserIdentity(user.getId(), user.getRole() == Role.ADMIN));
    }

    @Override
    public Optional<AuthenticatedUser> findAuthenticatedUser(Long userId, String email) {
        return userRepository.findByIdAndEmailAndDeletedAtIsNull(userId, email)
                .map(user -> new AuthenticatedUser(user.getId(), user.getEmail(), user.getRoleKey()));
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }
}
