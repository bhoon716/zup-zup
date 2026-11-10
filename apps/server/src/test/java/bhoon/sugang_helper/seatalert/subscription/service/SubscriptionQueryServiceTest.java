package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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

    @Autowired
    private SeatNotificationOutboxRepository outboxRepository;

    @Test
    @DisplayName("수신 대상과 통계는 취소된 구독을 제외한다")
    void recipientsAndStatisticsExcludeCancelledSubscriptions() {
        // given
        Subscription cancelled = subscription(2L, "course-a");
        cancelled.cancel();
        repository.saveAllAndFlush(List.of(
                subscription(1L, "course-a"), cancelled, subscription(3L, "course-b")));

        Long subscribed = saveOutbox("course-a");
        Long empty = saveOutbox("missing-course");

        // when & then
        assertThat(queryService.findActiveRecipientIdsByOutboxId(List.of(subscribed, empty)))
                .containsOnlyKeys(subscribed).containsEntry(subscribed, List.of(1L));
        assertThat(queryService.countActiveSubscriptions()).isEqualTo(2);
    }

    private Long saveOutbox(String courseKey) {
        return outboxRepository.save(SeatNotificationOutbox.builder().courseKey(courseKey).courseName(courseKey)
                .previousSeats(0).currentSeats(1).build()).getId();
    }

    private Subscription subscription(Long userId, String courseKey) {
        return Subscription.builder().userId(userId).courseKey(courseKey).isActive(true).build();
    }
}
