package bhoon.sugang_helper.identity.api.users;

import java.util.List;
import java.util.Optional;

/** 다른 모듈이 사용하는 Users 계약. */
public interface Users {

    Optional<UserIdentity> findByEmail(String email);

    Optional<UserIdentity> findById(Long userId);

    List<UserAccountStatus> findAccountStatuses(List<Long> userIds);

    long countUsers();
}
