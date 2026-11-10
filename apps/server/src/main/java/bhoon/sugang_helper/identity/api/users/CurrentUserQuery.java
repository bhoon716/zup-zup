package bhoon.sugang_helper.identity.api.users;

import java.util.Optional;

public interface CurrentUserQuery {

    UserIdentity requireCurrentUser();

    Optional<UserIdentity> findCurrentUser();
}
