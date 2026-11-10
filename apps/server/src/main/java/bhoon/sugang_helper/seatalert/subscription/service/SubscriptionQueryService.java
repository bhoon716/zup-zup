package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.seatalert.api.Subscriptions;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository.OutboxRecipient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService implements Subscriptions {

    private final SubscriptionRepository subscriptionRepository;

    public Map<Long, List<Long>> findActiveRecipientIdsByOutboxId(List<Long> outboxIds) {
        return subscriptionRepository.findActiveRecipientsByOutboxIds(outboxIds).stream()
                .collect(Collectors.groupingBy(OutboxRecipient::getOutboxId,
                        Collectors.mapping(OutboxRecipient::getUserId, Collectors.toList())));
    }

    public long countActiveSubscriptions() {
        return subscriptionRepository.countByIsActiveTrue();
    }
}
