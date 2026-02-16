package bhoon.sugang_helper.domain.subscription.repository;

import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserId(Long userId);

    long countByIsActiveTrue();

    void deleteAllByUserId(Long userId);
}
