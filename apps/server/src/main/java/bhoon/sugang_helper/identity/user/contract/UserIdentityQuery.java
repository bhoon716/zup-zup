package bhoon.sugang_helper.identity.user.contract;

import java.util.Optional;

public interface UserIdentityQuery {

    Optional<UserIdentity> findByEmail(String email);

    Optional<UserIdentity> findById(Long userId);

    Optional<AuthenticatedUser> findAuthenticatedUser(Long userId, String email);

    long countUsers();
}
