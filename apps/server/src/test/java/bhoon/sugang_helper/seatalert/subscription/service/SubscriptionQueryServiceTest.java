package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemester;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemesterQuery;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.dto.request.SubscriptionRequest;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({SubscriptionQueryService.class, SubscriptionService.class})
class SubscriptionQueryServiceTest {

    private static final String COURSE_KEY = "subscription-query-test-course";

    @Autowired
    private SubscriptionRepository repository;

    @Autowired
    private SubscriptionQueryService queryService;

    @Autowired
    private SeatNotificationOutboxRepository outboxRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CourseCatalog courseCatalog;

    @MockitoBean
    private CurrentUserQuery currentUserQuery;

    @MockitoBean
    private TrackedSemesterQuery trackedSemesterQuery;

    @Test
    @DisplayName("수신 대상은 취소된 구독을 제외하고 통계는 지정 학기의 활성 구독만 집계한다")
    void countsActiveSubscriptionsForRequestedTerm() {
        // given
        String courseKey = "2026:U211600020:SUBJECT:01";
        Subscription cancelled = subscription(2L, courseKey);
        cancelled.cancel();
        repository.saveAllAndFlush(List.of(
                subscription(1L, courseKey),
                cancelled,
                subscription(3L, "2026:U211600020:SUBJECT:02"),
                subscription(4L, "2026:U211600010:SUBJECT:01"),
                subscription(5L, "2025:U211600020:SUBJECT:01")));

        Long subscribed = saveOutbox(courseKey);
        Long empty = saveOutbox("missing-course");

        // when & then
        assertThat(queryService.findActiveRecipientIdsByOutboxId(List.of(subscribed, empty)))
                .containsOnlyKeys(subscribed).containsEntry(subscribed, List.of(1L));
        assertThat(queryService.countActiveSubscriptions("2026", "U211600020")).isEqualTo(2);
    }

    @ParameterizedTest(name = "전체 해지={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("최초 구독은 이전 여석도 받고 재구독은 시작 이후 여석만 받는다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {
            "delete from subscriptions where course_key = '" + COURSE_KEY + "'",
            "delete from subscription_ends where course_key = '" + COURSE_KEY + "'",
            "delete from seat_notification_outbox where course_key = '" + COURSE_KEY + "'"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void onlyFirstSubscriptionReceivesEarlierOpenings(boolean unsubscribeAll) {
        // given
        long userId = 1L;
        TrackedSemester trackedSemester = new TrackedSemester("2026", "U211600010");
        CourseSummary course = new CourseSummary(COURSE_KEY, COURSE_KEY, "1", "자료구조", "교수", 30, 0,
                trackedSemester.year(), trackedSemester.semesterCode(), null, "3", "", "", List.of());
        given(currentUserQuery.requireCurrentUser()).willReturn(new UserIdentity(userId, false));
        given(courseCatalog.findWithoutSchedulesByCourseKey(COURSE_KEY)).willReturn(Optional.of(course));
        given(trackedSemesterQuery.currentTarget()).willReturn(trackedSemester);

        LocalDateTime subscribedAt = LocalDateTime.of(2026, 10, 1, 12, 0);
        Long earlier = saveOutboxAt(subscribedAt.minusMinutes(1));
        Long sameTime = saveOutboxAt(subscribedAt);
        Long later = saveOutboxAt(subscribedAt.plusMinutes(1));
        SubscriptionRequest request = new SubscriptionRequest(COURSE_KEY);
        Long firstId = subscriptionService.subscribe(request).getId();
        setSubscriptionCreatedAt(firstId, subscribedAt.minusSeconds(1));
        assertThat(queryService.findActiveRecipientIdsByOutboxId(List.of(earlier))).containsEntry(earlier, List.of(userId));

        // when
        if (unsubscribeAll) {
            subscriptionService.unsubscribeAll();
        } else {
            subscriptionService.unsubscribe(firstId);
        }
        Long nextId = subscriptionService.subscribe(request).getId();
        setSubscriptionCreatedAt(nextId, subscribedAt);

        // then
        assertThat(nextId).isNotEqualTo(firstId);
        assertThatThrownBy(() -> subscriptionService.unsubscribe(firstId))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(queryService.findActiveRecipientIdsByOutboxId(List.of(earlier, sameTime, later)))
                .containsOnlyKeys(later)
                .containsEntry(later, List.of(userId));

        subscriptionService.unsubscribe(nextId);
        assertThat(queryService.findActiveRecipientIdsByOutboxId(List.of(later))).isEmpty();
    }

    private Long saveOutboxAt(LocalDateTime createdAt) {
        Long id = saveOutbox(COURSE_KEY);
        jdbcTemplate.update("update seat_notification_outbox set created_at = ? where id = ?", createdAt, id);
        return id;
    }

    private void setSubscriptionCreatedAt(Long subscriptionId, LocalDateTime createdAt) {
        jdbcTemplate.update("update subscriptions set created_at = ? where id = ?", createdAt, subscriptionId);
    }

    private Long saveOutbox(String courseKey) {
        return outboxRepository.save(SeatNotificationOutbox.builder().courseKey(courseKey).courseName(courseKey)
                .previousSeats(0).currentSeats(1).build()).getId();
    }

    private Subscription subscription(Long userId, String courseKey) {
        return Subscription.builder().userId(userId).courseKey(courseKey).isActive(true).build();
    }
}
