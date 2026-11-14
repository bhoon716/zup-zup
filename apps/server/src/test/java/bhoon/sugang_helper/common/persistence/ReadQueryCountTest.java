package bhoon.sugang_helper.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.entity.CourseSchedule;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.course.service.CourseCatalogQueryService;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.academic.review.entity.CourseEmojiReview;
import bhoon.sugang_helper.academic.review.repository.CourseEmojiReviewRepository;
import bhoon.sugang_helper.academic.review.service.CourseEmojiReviewService;
import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableResponse;
import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import bhoon.sugang_helper.planning.timetable.entity.TimetableEntry;
import bhoon.sugang_helper.planning.timetable.repository.TimetableRepository;
import bhoon.sugang_helper.planning.timetable.service.TimetableService;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.response.AdminNotificationDeliveryResponse;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationOutboxStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatInboxRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.notification.service.AdminNotificationDeliveryService;
import bhoon.sugang_helper.seatalert.notification.service.NotificationChannelPolicy;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxProcessor;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import bhoon.sugang_helper.support.feedback.repository.FeedbackAttachmentRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(showSql = false)
class ReadQueryCountTest {

    private static final Logger log = LoggerFactory.getLogger(ReadQueryCountTest.class);
    private static final long USER_ID = 7L;
    private static final String COURSE_KEY = "query-course";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEmojiReviewRepository emojiRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackAttachmentRepository attachmentRepository;

    @Autowired
    private SeatNotificationDeliveryRepository deliveryRepository;

    @Autowired
    private SeatNotificationOutboxRepository outboxRepository;

    @Autowired
    private SeatInboxRepository inboxRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Test
    @DisplayName("이모지 통계는 반응 수와 내 반응을 정렬 순서대로 반환한다")
    void emojiStats() {
        // given
        saveCourse(COURSE_KEY);
        saveEmoji(USER_ID, "A");
        saveEmoji(8L, "A");
        saveEmoji(8L, "B");
        saveEmoji(9L, "C");
        CourseEmojiReviewService service = new CourseEmojiReviewService(
                emojiRepository, new CourseReviewCatalogService(courseRepository), currentUser());

        // when
        Statistics statistics = startCounting();
        var results = service.getCourseEmojiStats(COURSE_KEY);

        // then
        assertThat(results).extracting(CourseEmojiReviewResponse::emoji, CourseEmojiReviewResponse::count,
                CourseEmojiReviewResponse::isMine)
                .containsExactly(tuple("A", 2L, true), tuple("B", 1L, false), tuple("C", 1L, false));
        assertQueryCount(statistics, "emoji", 2);
    }

    @Test
    @DisplayName("강좌 묶음 조회는 모든 강좌의 시간표를 반환한다")
    void courseSchedules() {
        // given
        List<String> keys = IntStream.range(0, 21).mapToObj(i -> "schedule-" + i).toList();
        for (String key : keys) {
            Course course = saveCourse(key);
            course.addSchedule(new CourseSchedule(CourseDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        }
        CourseCatalogQueryService service = new CourseCatalogQueryService(courseRepository);

        // when
        Statistics statistics = startCounting();
        List<CourseSummary> results = service.findByCourseKeyIn(keys);

        // then
        assertThat(results).hasSize(keys.size()).allSatisfy(course -> assertThat(course.schedules()).hasSize(1));
        assertQueryCount(statistics, "course-schedules", 1);
    }

    @Test
    @DisplayName("시간표 목록은 비어 있는 시간표와 등록 강좌 수를 반환한다")
    void timetableCounts() {
        // given
        Timetable filled = Timetable.builder().userId(USER_ID).name("수강 시간표").isPrimary(true).build();
        filled.addEntry(TimetableEntry.builder().courseKey(COURSE_KEY).build());
        entityManager.persist(filled);
        entityManager.persist(Timetable.builder().userId(USER_ID).name("빈 시간표").build());
        TimetableService service = new TimetableService(timetableRepository, null, null, null, currentUser());

        // when
        Statistics statistics = startCounting();
        var results = service.getMyTimetables();

        // then
        assertThat(results).extracting(TimetableResponse::getName, TimetableResponse::getCourseCount)
                .containsExactlyInAnyOrder(tuple("수강 시간표", 1L), tuple("빈 시간표", 0L));
        assertQueryCount(statistics, "timetable-list", 1);
    }

    @Test
    @DisplayName("문의 목록은 활성 답변 여부와 페이지 총건수를 유지한다")
    void feedbackList() {
        // given
        Feedback first = saveFeedback("답변 있음");
        Feedback second = saveFeedback("삭제된 답변만 있음");
        Feedback third = saveFeedback("답변 없음");
        entityManager.persist(new FeedbackReply(first, 8L, "답변"));
        FeedbackReply deleted = new FeedbackReply(second, 8L, "삭제 답변");
        deleted.delete();
        entityManager.persist(deleted);
        Feedback hidden = saveFeedback("삭제 문의");
        hidden.delete();
        FeedbackService service = new FeedbackService(
                feedbackRepository, null, currentUser(), null, null, null, null, null, null);

        // when
        Statistics statistics = startCounting();
        var results = service.getMyFeedbacksForCurrentUser(PageRequest.of(0, 3, Sort.by("id")));

        // then
        assertThat(results.getTotalElements()).isEqualTo(3);
        assertThat(results.getContent()).extracting(FeedbackResponse::id, FeedbackResponse::hasReplies)
                .containsExactly(tuple(first.getId(), true), tuple(second.getId(), false), tuple(third.getId(), false));
        assertQueryCount(statistics, "feedback-list", 2);
    }

    @Test
    @DisplayName("첨부파일은 현재 문의의 작성자만 한 번의 조회로 확인하고 삭제 문의는 제외한다")
    void attachmentJoinsOnlyItsActiveFeedback() {
        // given
        Feedback active = saveFeedback("공개 중인 문의");
        Feedback deleted = saveFeedback("삭제 문의");
        FeedbackAttachment activeAttachment = saveAttachment(active);
        FeedbackAttachment deletedAttachment = saveAttachment(deleted);
        deleted.delete();

        // when
        Statistics statistics = startCounting();
        var attachment = attachmentRepository.findByIdAndFeedbackId(activeAttachment.getId(), active.getId());

        // then
        assertThat(attachment).get().extracting(item -> item.getFeedback().getUserId()).isEqualTo(USER_ID);
        assertQueryCount(statistics, "feedback-attachment", 1);
        assertThat(attachmentRepository.findByIdAndFeedbackId(activeAttachment.getId(), deleted.getId())).isEmpty();
        assertThat(attachmentRepository.findByIdAndFeedbackId(deletedAttachment.getId(), deleted.getId())).isEmpty();
    }

    @Test
    @DisplayName("관리자 발송 실패 목록은 다른 대기 항목에 속한 강좌 정보를 페이지 순서대로 반환한다")
    void deliveryList() {
        // given
        saveDeadLetter("older-course", LocalDateTime.of(2026, 11, 10, 9, 0));
        saveDeadLetter("newer-course", LocalDateTime.of(2026, 11, 10, 10, 0));
        AdminNotificationDeliveryService service = new AdminNotificationDeliveryService(deliveryRepository, null, null);

        // when
        Statistics statistics = startCounting();
        var results = service.getDeadLetters(PageRequest.of(0, 2));

        // then
        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).extracting(AdminNotificationDeliveryResponse::courseKey)
                .containsExactly("newer-course", "older-course");
        assertQueryCount(statistics, "delivery-list", 2);
    }

    @Test
    @DisplayName("발송 작업 일괄 생성은 여러 이벤트의 기존 발송과 수신자가 없는 이벤트를 보존한다")
    void outboxBatch() {
        // given
        User user = userRepository.save(User.builder().name("수신자").email("recipient@example.com")
                .role(Role.USER).emailEnabled(true).build());
        subscriptionRepository.save(Subscription.builder().userId(user.getId()).courseKey(COURSE_KEY)
                .isActive(true).build());
        SeatNotificationOutbox first = saveOutbox(COURSE_KEY);
        SeatNotificationOutbox second = saveOutbox(COURSE_KEY);
        SeatNotificationOutbox noSubscribers = saveOutbox("no-subscribers");
        SeatNotificationDelivery sent = SeatNotificationDelivery.builder().outbox(first).userId(user.getId())
                .channel(NotificationChannel.EMAIL).build();
        sent.markSent();
        deliveryRepository.save(sent);
        SeatNotificationOutboxProcessor processor = new SeatNotificationOutboxProcessor(
                outboxRepository, deliveryRepository, inboxRepository, new SubscriptionQueryService(subscriptionRepository),
                new UserNotificationQueryService(userRepository, userDeviceRepository),
                new NotificationChannelPolicy(new NotificationProperties(true, false, false)), null, null, null, null);
        ReflectionTestUtils.setField(processor, "batchSize", 10);

        // when
        Statistics statistics = startCounting();
        processor.materializePendingOutboxes();
        entityManager.flush();
        assertQueryCount(statistics, "outbox-batch", 12);

        // then
        assertThat(outboxRepository.findAll()).extracting(SeatNotificationOutbox::getId, SeatNotificationOutbox::getStatus)
                .containsExactlyInAnyOrder(tuple(first.getId(), SeatNotificationOutboxStatus.COMPLETED),
                        tuple(second.getId(), SeatNotificationOutboxStatus.PROCESSING),
                        tuple(noSubscribers.getId(), SeatNotificationOutboxStatus.COMPLETED));
        assertThat(deliveryRepository.findAll()).extracting(delivery -> delivery.getOutbox().getId())
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    private Statistics startCounting() {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        return statistics;
    }

    private void assertQueryCount(Statistics statistics, String path, long expectedCount) {
        long count = statistics.getPrepareStatementCount();
        log.info("QUERY_COUNT {} {}", path, count);
        assertThat(count).as(path).isEqualTo(expectedCount);
    }

    private Course saveCourse(String key) {
        return courseRepository.save(Course.builder().courseKey(key).subjectCode(key).name(key)
                .classNumber("1").capacity(30).current(0).academicYear("2026").semester("U211600010")
                .build());
    }

    private void saveEmoji(long userId, String emoji) {
        entityManager.persist(CourseEmojiReview.builder().courseKey(COURSE_KEY).userId(userId).emoji(emoji).build());
    }

    private Feedback saveFeedback(String title) {
        Feedback feedback = Feedback.builder().userId(USER_ID).type(FeedbackType.BUG).title(title).content(title).build();
        entityManager.persist(feedback);
        return feedback;
    }

    private FeedbackAttachment saveAttachment(Feedback feedback) {
        return attachmentRepository.save(FeedbackAttachment.builder().feedback(feedback).fileUrl("/uploads/fixture.png")
                .originalName("fixture.png").build());
    }

    private void saveDeadLetter(String courseKey, LocalDateTime at) {
        SeatNotificationOutbox outbox = saveOutbox(courseKey);
        SeatNotificationDelivery delivery = SeatNotificationDelivery.builder().outbox(outbox).userId(USER_ID)
                .channel(NotificationChannel.EMAIL).build();
        delivery.markFailure("N001", 1, at, at);
        entityManager.persist(delivery);
    }

    private SeatNotificationOutbox saveOutbox(String courseKey) {
        return outboxRepository.save(SeatNotificationOutbox.builder().courseKey(courseKey).courseName(courseKey)
                .previousSeats(0).currentSeats(1).build());
    }

    private CurrentUserQuery currentUser() {
        CurrentUserQuery query = mock(CurrentUserQuery.class);
        UserIdentity user = new UserIdentity(USER_ID, false);
        when(query.requireCurrentUser()).thenReturn(user);
        when(query.findCurrentUser()).thenReturn(Optional.of(user));
        return query;
    }
}
