package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.identity.user.service.UserSubscriptionCleanup;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionAccountLifecycleService implements UserSubscriptionCleanup {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public void cancelAllForUser(Long userId) {
        subscriptionRepository.findByUserId(userId).forEach(subscription -> subscription.cancel());
    }
}
