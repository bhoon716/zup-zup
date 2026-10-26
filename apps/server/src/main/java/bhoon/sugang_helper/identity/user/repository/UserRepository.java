package bhoon.sugang_helper.identity.user.repository;

import bhoon.sugang_helper.identity.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(String oauthIssuer, String oauthSubject);

    Optional<User> findByIdAndEmailAndDeletedAtIsNull(Long id, String email);

    boolean existsByIdAndEmailAndDeletedAtIsNull(Long id, String email);
}
