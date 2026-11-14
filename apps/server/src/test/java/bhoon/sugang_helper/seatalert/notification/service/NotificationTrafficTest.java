package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.seatalert.api.NotificationTraffic.Bucket;
import bhoon.sugang_helper.seatalert.api.NotificationTraffic.Range;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationTrafficRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(showSql = false)
class NotificationTrafficTest {

    private static final Clock UTC_CLOCK = Clock.fixed(Instant.parse("2026-10-12T15:30:00Z"), ZoneOffset.UTC);
    private static final String SEOUL_MIDNIGHT_UTC = "2026-10-12T15:00";
    private static final String SENT = "SENT";

    @Autowired private NotificationTrafficRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("한국 시간 정각과 자정으로 성공한 외부 발송을 집계하고 빈 구간을 0으로 채운다")
    void countsSuccessfulDeliveriesByKoreanHourAndDay() {
        // given
        long feedback = inbox("FEEDBACK_REPLY", "2026-10-10T12:00");
        long announcement = inbox("ANNOUNCEMENT", "2026-10-10T12:00");
        long seatInbox = inbox("SEAT_OPENED", SEOUL_MIDNIGHT_UTC);
        inbox("ANNOUNCEMENT", SEOUL_MIDNIGHT_UTC);
        seat("2026-10-06T15:00");
        seat("2026-10-11T15:00");
        seat("2026-10-12T14:59:59.999999");
        seat(SEOUL_MIDNIGHT_UTC);
        seat("2026-10-12T15:31");
        delivery("seat-inbox", seatInbox, SENT, SEOUL_MIDNIGHT_UTC);
        delivery("feedback-email", feedback, SENT, "2026-10-12T14:59:59.999999");
        delivery("feedback-web", feedback, SENT, "2026-10-12T14:59:59.999999");
        delivery("feedback-skipped", feedback, "SKIPPED", "2026-10-12T15:01");
        delivery("feedback-failed", feedback, "FAILED", "2026-10-12T15:01");
        delivery("announcement-web", announcement, SENT, SEOUL_MIDNIGHT_UTC);
        var traffic = new NotificationTrafficService(repository, UTC_CLOCK);

        // when
        var hourly = traffic.findRecent(Range.LAST_24_HOURS);
        var daily = traffic.findRecent(Range.LAST_7_DAYS);

        // then
        assertThat(hourly).hasSize(24);
        assertThat(hourly.getFirst()).isEqualTo(bucket("01:00", "2026-10-12T01:00+09:00", 0, 0, 0));
        assertThat(hourly.get(22)).isEqualTo(bucket("23:00", "2026-10-12T23:00+09:00", 1, 2, 0));
        assertThat(hourly.getLast()).isEqualTo(bucket("00:00", "2026-10-13T00:00+09:00", 1, 0, 1));
        assertThat(hourly.subList(0, 22)).allMatch(point ->
                point.seatOpened() == 0 && point.feedbackReply() == 0 && point.announcement() == 0);
        assertThat(daily).hasSize(7);
        assertThat(daily.getFirst()).isEqualTo(bucket("10/7", "2026-10-07T00:00+09:00", 1, 0, 0));
        assertThat(daily.get(5)).isEqualTo(bucket("10/12", "2026-10-12T00:00+09:00", 2, 2, 0));
        assertThat(daily.getLast()).isEqualTo(bucket("10/13", "2026-10-13T00:00+09:00", 1, 0, 1));
    }

    @Test
    @DisplayName("공석 알림 발송 건수는 오래된 성공 이력을 포함하며 다른 알림 발송을 제외한다")
    void countsAllRecordedSeatNotifications() {
        // given
        seat("2025-01-03T12:00");
        seat(SEOUL_MIDNIGHT_UTC);
        long announcement = inbox("ANNOUNCEMENT", SEOUL_MIDNIGHT_UTC);
        long seatInbox = inbox("SEAT_OPENED", SEOUL_MIDNIGHT_UTC);
        delivery("announcement-web", announcement, SENT, SEOUL_MIDNIGHT_UTC);
        delivery("seat-inbox", seatInbox, SENT, SEOUL_MIDNIGHT_UTC);
        var traffic = new NotificationTrafficService(repository, UTC_CLOCK);

        // when
        long result = traffic.countSentSeatNotifications();

        // then
        assertThat(result).isEqualTo(2);
    }

    private void seat(String at) {
        jdbc.update("""
                insert into notification_histories (user_id, course_key, title, message, channel, created_at)
                values (1, 'course', 'title', 'message', 'WEB', ?)
                """, LocalDateTime.parse(at));
    }

    private long inbox(String type, String at) {
        jdbc.update("""
                insert into seat_inbox_items (user_id, type, available, occurred_at) values (1, ?, true, ?)
                """, type, LocalDateTime.parse(at));
        return jdbc.queryForObject("select max(id) from seat_inbox_items", Long.class);
    }

    private void delivery(String id, long inbox, String status, String at) {
        jdbc.update("""
                insert into inbox_channel_deliveries
                  (id, inbox_id, user_id, channel, title, message, status, attempts, next_attempt_at, finished_at, version)
                values (?, ?, 1, 'WEB', 'title', 'message', ?, 1, ?, ?, 0)
                """, id, inbox, status, LocalDateTime.parse(at), LocalDateTime.parse(at));
    }

    private static Bucket bucket(String label, String start, long seat, long feedback, long announcement) {
        return new Bucket(label, OffsetDateTime.parse(start), seat, feedback, announcement);
    }
}
