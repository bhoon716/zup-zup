package bhoon.sugang_helper.identity.user.repository;

import bhoon.sugang_helper.identity.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Transactional(readOnly = true)
    Optional<User> findByEmail(String email);

    Optional<User> findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(String oauthIssuer, String oauthSubject);

    Optional<User> findByIdAndEmailAndDeletedAtIsNull(Long id, String email);

    boolean existsByIdAndEmailAndDeletedAtIsNull(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from User u where u.id in :userIds and u.deletedAt is null order by u.id")
    List<User> findActiveByIdsForShare(@Param("userIds") List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId and u.deletedAt is null")
    Optional<User> findActiveByIdForUpdate(@Param("userId") Long userId);

    @Query("select max(u.id) from User u where u.deletedAt is null")
    Optional<Long> findLastActiveId();

    @Query("""
            select u.id from User u
            where u.deletedAt is null and u.id > :afterId and u.id <= :throughId
            order by u.id
            """)
    List<Long> findActiveIdsBetween(@Param("afterId") Long afterId, @Param("throughId") Long throughId,
                                    Pageable pageable);
}
