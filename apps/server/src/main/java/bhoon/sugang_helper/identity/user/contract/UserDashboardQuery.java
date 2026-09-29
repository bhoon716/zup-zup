package bhoon.sugang_helper.identity.user.contract;

import java.util.Optional;

public interface UserDashboardQuery {

    Optional<UserDashboardProfile> findCurrentUser();
}
