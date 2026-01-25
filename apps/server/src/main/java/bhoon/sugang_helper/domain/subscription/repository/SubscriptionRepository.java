package bhoon.sugang_helper.domain.subscription.repository;

import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserIdAndIsActiveTrue(Long userId);

    long countByIsActiveTrue();

    void deleteAllByUserId(Long userId);
}
