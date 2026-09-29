package bhoon.sugang_helper.identity.user.contract;

import java.util.List;
import java.util.Optional;

public interface UserDisplayNameQuery {

    Optional<UserDisplayName> findById(Long userId);

    List<UserDisplayName> findByIds(List<Long> userIds);
}
