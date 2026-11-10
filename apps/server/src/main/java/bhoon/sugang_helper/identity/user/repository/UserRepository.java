package bhoon.sugang_helper.identity.user.repository;

import bhoon.sugang_helper.identity.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Transactional(readOnly = true)
    Optional<User> findByEmail(String email);

    Optional<User> findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(String oauthIssuer, String oauthSubject);

    Optional<User> findByIdAndEmailAndDeletedAtIsNull(Long id, String email);

    boolean existsByIdAndEmailAndDeletedAtIsNull(Long id, String email);
}
