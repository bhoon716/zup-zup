package bhoon.sugang_helper.seatalert.subscription.repository;

import bhoon.sugang_helper.seatalert.subscription.entity.SubscriptionEnd;
import org.springframework.data.repository.Repository;

public interface SubscriptionEndRepository extends Repository<SubscriptionEnd, SubscriptionEnd.Key> {
    boolean existsById(SubscriptionEnd.Key id);

    SubscriptionEnd save(SubscriptionEnd end);
}
