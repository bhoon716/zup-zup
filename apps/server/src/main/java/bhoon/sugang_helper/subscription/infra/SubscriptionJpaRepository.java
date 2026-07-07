package bhoon.sugang_helper.subscription.infra;

import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionJpaRepository extends JpaRepository<Subscription, Long>, SubscriptionRepository {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserId(Long userId);

    long countByIsActiveTrue();

    void deleteAllByUserId(Long userId);
}
