package bhoon.sugang_helper.subscription.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends RepositoryContract<Subscription, Long> {
    List<Subscription> findByCourseKeyAndIsActiveTrue(String courseKey);

    Optional<Subscription> findByUserIdAndCourseKey(Long userId, String courseKey);

    long countByUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByUserId(Long userId);

    long countByIsActiveTrue();

    void deleteAllByUserId(Long userId);
}
