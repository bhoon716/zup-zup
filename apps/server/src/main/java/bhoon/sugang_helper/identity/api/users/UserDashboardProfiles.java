package bhoon.sugang_helper.identity.api.users;

import java.util.Optional;

public interface UserDashboardProfiles {

    Optional<UserDashboardProfile> findCurrentUser();
}
