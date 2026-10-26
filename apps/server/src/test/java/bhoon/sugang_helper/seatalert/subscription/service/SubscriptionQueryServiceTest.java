package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(SubscriptionQueryService.class)
class SubscriptionQueryServiceTest {

    @Autowired
    private SubscriptionRepository repository;
    @Autowired
    private SubscriptionQueryService queryService;

    @Test
    void recipientsAndStatisticsExcludeCancelledSubscriptions() {
        // given
        Subscription cancelled = subscription(2L, "course-a");
        cancelled.cancel();
        repository.saveAllAndFlush(List.of(
                subscription(1L, "course-a"), cancelled, subscription(3L, "course-b")));

        // when & then
        assertThat(queryService.findActiveRecipientIds("course-a")).containsExactly(1L);
        assertThat(queryService.findActiveRecipientIds("missing-course")).isEmpty();
        assertThat(queryService.countActiveSubscriptions()).isEqualTo(2);
    }

    private Subscription subscription(Long userId, String courseKey) {
        return Subscription.builder().userId(userId).courseKey(courseKey).isActive(true).build();
    }
}
