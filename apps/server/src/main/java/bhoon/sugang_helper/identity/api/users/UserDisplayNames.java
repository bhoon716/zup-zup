package bhoon.sugang_helper.identity.api.users;

import java.util.List;
import java.util.Optional;

/** 다른 모듈이 사용하는 UserDisplayNames 계약. */
public interface UserDisplayNames {

    Optional<UserDisplayName> findById(Long userId);

    List<UserDisplayName> findByIds(List<Long> userIds);
}
