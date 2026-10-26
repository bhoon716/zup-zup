package bhoon.sugang_helper.seatalert.subscription.repository;

import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserId(Long userId);

    long countByIsActiveTrue();

    void deleteAllByUserId(Long userId);

    @Query("select s.userId from Subscription s where s.courseKey = :courseKey and s.isActive = true")
    List<Long> findUserIdByCourseKeyAndIsActiveTrue(@Param("courseKey") String courseKey);
}
