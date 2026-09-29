package bhoon.sugang_helper.seatalert.subscription.application;

import bhoon.sugang_helper.seatalert.subscription.contract.SubscriptionRecipientQuery;
import bhoon.sugang_helper.seatalert.subscription.domain.SubscriptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionRecipientQueryService implements SubscriptionRecipientQuery {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public List<Long> findActiveRecipientIds(String courseKey) {
        return subscriptionRepository.findUserIdByCourseKeyAndIsActiveTrue(courseKey);
    }
}
