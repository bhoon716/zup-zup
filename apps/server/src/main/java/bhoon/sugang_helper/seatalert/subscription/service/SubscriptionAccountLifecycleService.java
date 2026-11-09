package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionAccountLifecycleService {

    private final SubscriptionRepository subscriptionRepository;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        subscriptionRepository.findByUserId(event.userId()).forEach(subscription -> subscription.cancel());
    }
}
