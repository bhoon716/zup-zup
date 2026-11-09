package bhoon.sugang_helper.identity.api.users;

import java.util.List;
import java.util.Optional;

public interface UserDisplayNames {

    Optional<UserDisplayName> findById(Long userId);

    List<UserDisplayName> findByIds(List<Long> userIds);
}
