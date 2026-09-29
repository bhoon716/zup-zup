package bhoon.sugang_helper.identity.user.contract;

import java.util.Optional;
import java.util.List;

public interface UserIdentityQuery {

    Optional<UserIdentity> findByEmail(String email);

    Optional<UserIdentity> findById(Long userId);

    Optional<AuthenticatedUser> findAuthenticatedUser(Long userId, String email);

    List<UserAccountStatus> findAccountStatuses(List<Long> userIds);

    long countUsers();
}
