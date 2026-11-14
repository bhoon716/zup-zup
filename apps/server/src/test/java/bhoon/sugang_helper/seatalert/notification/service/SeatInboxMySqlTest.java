package bhoon.sugang_helper.seatalert.notification.service;

import static bhoon.sugang_helper.seatalert.notification.enums.SeatInboxView.SEAT_ALERTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import bhoon.sugang_helper.identity.auth.service.SessionInvalidator;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.identity.user.service.EmailVerificationService;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.identity.user.service.UserService;
import bhoon.sugang_helper.seatalert.notification.dto.response.SeatInboxItemResponse;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxReadFilter;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxSort;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionAccountLifecycleService;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("migration")
@Testcontainers
@DataJpaTest(showSql = false, properties = {
    "spring.autoconfigure.exclude=",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SeatInboxService.class, SeatNotificationOutboxProcessor.class, SeatNotificationDeliveryStateService.class,
        NotificationChannelPolicy.class, UserIdentityQueryService.class, UserNotificationQueryService.class,
        UserService.class, SubscriptionQueryService.class, SubscriptionAccountLifecycleService.class,
        SeatInboxMySqlTest.TestConfig.class})
@MockitoBean(types = {NotificationService.class, InboxWebPushNotices.class,
    SessionInvalidator.class, EmailVerificationService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SeatInboxMySqlTest {

    private static final String OWNER = "inbox-owner@example.invalid";
    private static final String WITHDRAWING_OWNER = "withdrawing-inbox-owner@example.invalid";
    private static final String SEARCH_NAME = "SQL%_! 분석";
    private static final String DECOY_NAME = "SQLXYZ! 분석";
    private static final String SEARCH_TERM = "sql%_!";
    private static final String SEARCH_KEY = "seat-inbox-search";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired private UserRepository users;

    @Autowired private UserService userService;

    @Autowired private SubscriptionRepository subscriptions;

    @Autowired private SeatNotificationOutboxRepository outboxes;

    @Autowired private SeatNotificationOutboxProcessor processor;

    @Autowired private SeatInboxService inbox;

    @Autowired private JdbcTemplate jdbc;

    @MockitoSpyBean private SubscriptionQueryService subscriptionQuery;

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Test
    @WithMockUser(username = OWNER)
    @DisplayName("알림함은 오래된 발생 시각과 강의 정보를 보존하고 Outbox가 삭제된 뒤에도 조회된다")
    void snapshotSurvivesOutboxDeletion() {
        // given
        subscribe(OWNER, SEARCH_KEY);
        Long eventId = saveOutbox(SEARCH_KEY, SEARCH_NAME);
        Long decoyId = saveOutbox(SEARCH_KEY, DECOY_NAME);
        LocalDateTime occurredAt = LocalDateTime.of(2024, 3, 4, 10, 0);
        jdbc.update("update seat_notification_outbox set created_at = ? where id = ?", occurredAt, eventId);

        // when
        processor.materializePendingOutboxes();
        outboxes.deleteAllById(List.of(eventId, decoyId));
        Long newestId = inbox.getInbox(SEAT_ALERTS, null, null, null, "sql", SeatInboxSort.NEWEST, 0)
                .getContent().getFirst().id();
        inbox.markRead(newestId);
        var unreadFirst = inbox.getInbox(SEAT_ALERTS, null, null, null, "sql", SeatInboxSort.UNREAD_FIRST, 0).getContent();
        var unread = inbox.getInbox(SEAT_ALERTS, SeatInboxReadFilter.UNREAD, null, SEARCH_TERM, null, SeatInboxSort.NEWEST, 0)
                .getContent();
        assertThat(unread).hasSize(1);
        inbox.markRead(unread.getFirst().id());

        // then
        assertThat(unreadFirst).extracting(SeatInboxItemResponse::courseName).containsExactly(SEARCH_NAME, DECOY_NAME);
        assertThat(inbox.getInbox(SEAT_ALERTS, SeatInboxReadFilter.READ, null, SEARCH_TERM, null, SeatInboxSort.NEWEST, 0)
                .getContent())
                .singleElement().satisfies(item -> {
                    assertThat(item.courseKey()).isEqualTo(SEARCH_KEY);
                    assertThat(item.courseName()).isEqualTo(SEARCH_NAME);
                    assertThat(item.availableSeats()).isEqualTo(1);
                    assertThat(item.occurredAt()).isEqualTo(occurredAt);
                    assertThat(item.readAt()).isNotNull();
                });
        assertThatThrownBy(() -> jdbc.update("""
                insert into seat_inbox_items (outbox_id, user_id, course_key, course_name, available_seats, occurred_at)
                select outbox_id, user_id, course_key, course_name, available_seats, occurred_at
                from seat_inbox_items where id = ?
                """, unread.getFirst().id())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @WithMockUser(username = WITHDRAWING_OWNER)
    @DisplayName("구독을 읽은 뒤 탈퇴가 커밋된 경우 알림함에는 활성 계정만 기록한다")
    void withdrawalAfterSubscriptionSnapshotExcludesTheAccount() throws Exception {
        // given
        String courseKey = "seat-inbox-withdrawal";
        subscribe(WITHDRAWING_OWNER, courseKey);
        Long activeOwner = subscribe("active-inbox-owner@example.invalid", courseKey);
        Long eventId = saveOutbox(courseKey, "동시 탈퇴 검증");
        CountDownLatch selected = new CountDownLatch(1);
        CountDownLatch withdrawn = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object candidates = invocation.callRealMethod();
            selected.countDown();
            assertThat(withdrawn.await(10, TimeUnit.SECONDS)).isTrue();
            return candidates;
        }).when(subscriptionQuery).findActiveRecipientIdsByOutboxId(List.of(eventId));

        // when
        try (var executor = Executors.newSingleThreadExecutor()) {
            var materialized = executor.submit(processor::materializePendingOutboxes);
            try {
                assertThat(selected.await(10, TimeUnit.SECONDS)).isTrue();
                userService.withdraw();
            } finally {
                withdrawn.countDown();
            }
            materialized.get(10, TimeUnit.SECONDS);
        }

        // then
        assertThat(jdbc.queryForList("select user_id from seat_inbox_items where outbox_id = ?", Long.class, eventId))
                .containsExactly(activeOwner);
    }

    private Long subscribe(String email, String courseKey) {
        User user = users.save(User.builder().name("알림함 사용자").email(email).role(Role.USER).build());
        subscriptions.save(Subscription.builder().userId(user.getId()).courseKey(courseKey).isActive(true).build());
        return user.getId();
    }

    private Long saveOutbox(String courseKey, String courseName) {
        return outboxes.save(SeatNotificationOutbox.builder()
                .courseKey(courseKey).courseName(courseName).previousSeats(0).currentSeats(1).build()).getId();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        NotificationProperties notificationProperties() {
            return new NotificationProperties(false, false, false);
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
