package bhoon.sugang_helper.seatalert.subscription.contract;

import java.util.List;

public interface SubscriptionRecipientQuery {

    List<Long> findActiveRecipientIds(String courseKey);
}
