package bhoon.sugang_helper.identity.api.users;

import java.util.Optional;

/** 다른 모듈이 사용하는 UserDashboardProfiles 계약. */
public interface UserDashboardProfiles {

    Optional<UserDashboardProfile> findCurrentUser();
}
