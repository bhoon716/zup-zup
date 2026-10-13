package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.UserDashboardProfile;
import java.util.Optional;

public interface UserDashboardQuery {

    Optional<UserDashboardProfile> findCurrentUser();
}
