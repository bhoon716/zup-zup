package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.dto.UserAccountStatus;
import bhoon.sugang_helper.identity.user.dto.UserIdentity;
import java.util.List;
import java.util.Optional;

public interface UserIdentityQuery {

    Optional<UserIdentity> findByEmail(String email);

    Optional<UserIdentity> findById(Long userId);

    Optional<AuthenticatedUser> findAuthenticatedUser(Long userId, String email);

    List<UserAccountStatus> findAccountStatuses(List<Long> userIds);

    long countUsers();
}
