package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionStatisticsQueryService implements SubscriptionStatisticsQuery {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public long countActiveSubscriptions() {
        return subscriptionRepository.countByIsActiveTrue();
    }
}
