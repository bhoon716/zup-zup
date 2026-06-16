package bhoon.sugang_helper.user.infra;

import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
    Optional<User> findByEmail(String email);
}
