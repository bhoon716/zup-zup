package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.UserDisplayName;
import java.util.List;
import java.util.Optional;

public interface UserDisplayNameQuery {

    Optional<UserDisplayName> findById(Long userId);

    List<UserDisplayName> findByIds(List<Long> userIds);
}
