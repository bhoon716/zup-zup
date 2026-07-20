package bhoon.sugang_helper.user.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.Optional;

public interface UserRepository extends RepositoryContract<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndEmailAndDeletedAtIsNull(Long id, String email);

    boolean existsByIdAndEmailAndDeletedAtIsNull(Long id, String email);
}
