package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.identity.user.service.UserNotificationQueryService;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionQueryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("performance")
@Testcontainers
@DataJpaTest(showSql = false, properties = {
    "spring.autoconfigure.exclude=",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=none",
    "app.notification.outbox.batch-size=1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = SeatNotificationOutboxProcessorBenchmarkTest.Config.class)
@MockitoBean(types = {NotificationService.class, InboxWebPushNotices.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SeatNotificationOutboxProcessorBenchmarkTest {

    private static final String DATABASE = "sugang_helper";
    private static final String MYSQL_IMAGE = "mysql:8.4";
    private static final int CHANNELS = 3;
    private static final int TRIALS = benchmarkSetting("OUTBOX_BENCH_TRIALS", 3);
    private static final int SAMPLES = benchmarkSetting("OUTBOX_BENCH_SAMPLES", 100);
    private static final int WARMUP = benchmarkSetting("OUTBOX_BENCH_WARMUP", 10);

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName(DATABASE)
            .withUsername("benchmark")
            .withPassword("benchmark")
            .withCommand("--performance-schema=ON");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SeatNotificationOutboxProcessor processor;

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @BeforeAll
    static void seedSubscribers(@Autowired JdbcTemplate jdbc) {
        List<Integer> users = IntStream.rangeClosed(1, 1_000).boxed().toList();
        jdbc.batchUpdate("""
                insert into users (id, email, name, role, discord_id, discord_enabled, email_enabled,
                    web_push_enabled, onboarding_completed)
                values (?, ?, 'Benchmark', 'USER', ?, true, true, true, true)
                """, users, 1_000, (statement, id) -> {
                    statement.setInt(1, id);
                    statement.setString(2, "subscriber-" + id + "@example.invalid");
                    statement.setString(3, "discord-" + id);
                });
        jdbc.update("""
                insert into user_devices (user_id, token, p256dh, auth)
                select id, concat('test-endpoint-', id), 'test-public-key', 'test-auth' from users
                """);
        for (int subscribers : List.of(100, 1_000)) {
            jdbc.update("""
                    insert into subscriptions (course_key, user_id, is_active)
                    select ?, id, true from users where id <= ?
                    """, courseKey(subscribers), subscribers);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1_000})
    @DisplayName("실제 MySQL에서 빈자리 이벤트의 발송 작업 생성 시간과 SQL 실행 횟수를 측정한다")
    void measureMaterialization(int subscribers) throws IOException, InterruptedException {
        // given
        resetOutboxes(WARMUP, courseKey(subscribers));
        for (int iteration = 0; iteration < WARMUP; iteration++) {
            processor.materializePendingOutboxes();
        }

        // when & then
        for (int trial = 1; trial <= TRIALS; trial++) {
            resetOutboxes(SAMPLES, courseKey(subscribers));
            Map<String, SqlStats> before = sqlStats();
            long[] durations = measureCommits();
            Map<String, SqlStats> after = sqlStats();
            assertAllOutboxesMaterialized(SAMPLES, subscribers, subscribers * CHANNELS);
            reportTrial(subscribers, trial, durations, before, after);
        }
    }

    @Test
    @DisplayName("기존 작업과 비활성 채널을 건너뛰고 같은 이벤트 재처리에서도 작업을 한 번만 생성한다")
    void preservesExistingDeliveriesAndChannelPolicy() {
        // given
        resetOutboxes(1, courseKey(100));
        Long outboxId = jdbc.queryForObject("select id from seat_notification_outbox", Long.class);
        jdbc.update("""
                insert into seat_notification_deliveries (outbox_id, user_id, channel, status, attempts,
                    next_attempt_at, idempotency_key)
                values (?, 1, 'EMAIL', 'SENT', 1, now(), uuid())
                """, outboxId);
        List<Map<String, Object>> existing = jdbc.queryForList("""
                select id, status, attempts, idempotency_key
                from seat_notification_deliveries where user_id = 1 and channel = 'EMAIL'
                """);
        jdbc.update("update users set email_enabled = false where id = 2");
        try {
            // when
            processor.materializePendingOutboxes();
            jdbc.update("update seat_inbox_items set read_at = now() where user_id = 1");
            jdbc.update("update seat_inbox_items set deleted_at = now() where user_id = 2");
            jdbc.update("update seat_notification_outbox set status = 'PENDING' where id = ?", outboxId);
            processor.materializePendingOutboxes();

            // then
            assertAllOutboxesMaterialized(1, 100, 299);
            assertThat(jdbc.queryForObject("""
                    select count(*) from seat_inbox_items where user_id = 1 and read_at is not null
                    """, Long.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("""
                    select count(*) from seat_inbox_items where user_id = 2 and deleted_at is not null
                    """, Long.class)).isEqualTo(1);
            assertThat(jdbc.queryForList("""
                    select id, status, attempts, idempotency_key
                    from seat_notification_deliveries where user_id = 1 and channel = 'EMAIL'
                    """)).isEqualTo(existing);
            assertThat(jdbc.queryForList("""
                    select channel from seat_notification_deliveries where user_id = 2
                    """, String.class)).containsExactlyInAnyOrder("WEB", "DISCORD");
        } finally {
            jdbc.update("update users set email_enabled = true where id = 2");
        }
    }

    private long[] measureCommits() {
        long[] durations = new long[SAMPLES];
        for (int iteration = 0; iteration < durations.length; iteration++) {
            long startedAt = System.nanoTime();
            processor.materializePendingOutboxes();
            durations[iteration] = System.nanoTime() - startedAt;
        }
        return durations;
    }

    private void resetOutboxes(int events, String courseKey) {
        jdbc.update("delete from seat_inbox_items");
        jdbc.update("delete from seat_notification_deliveries");
        jdbc.update("delete from seat_notification_outbox");
        List<String> courses = IntStream.range(0, events).mapToObj(index -> courseKey).toList();
        jdbc.batchUpdate("""
                insert into seat_notification_outbox
                    (course_key, course_name, previous_seats, current_seats, status, version, created_at)
                values (?, 'Benchmark course', 0, 1, 'PENDING', 0, now())
                """, courses, events, (statement, key) -> statement.setString(1, key));
    }

    private void assertAllOutboxesMaterialized(int expectedEvents, int subscribers, int deliveriesPerEvent) {
        assertThat(jdbc.queryForObject("""
                select count(*) from seat_notification_outbox where status = 'PROCESSING'
                """, Long.class)).isEqualTo(expectedEvents);
        assertThat(jdbc.queryForList("""
                select count(*) from seat_notification_deliveries group by outbox_id
                """, Long.class)).hasSize(expectedEvents).containsOnly((long) deliveriesPerEvent);
        assertThat(jdbc.queryForList("""
                select count(*) from seat_inbox_items group by outbox_id
                """, Long.class)).hasSize(expectedEvents).containsOnly((long) subscribers);
    }

    private Map<String, SqlStats> sqlStats() throws IOException, InterruptedException {
        ExecResult result = MYSQL.execInContainer("sh", "-c",
                "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql -uroot --batch --skip-column-names -e \"$1\"",
                "statement-digests", """
                        select count_star, sum_timer_wait, digest_text
                        from performance_schema.events_statements_summary_by_digest
                        where schema_name = 'sugang_helper'
                          and digest_text regexp '^(SELECT|INSERT|UPDATE|DELETE) '
                        """);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        Map<String, SqlStats> statements = new LinkedHashMap<>();
        result.getStdout().lines().forEach(line -> {
            String[] fields = line.split("\t", 3);
            statements.put(fields[2], new SqlStats(Long.parseLong(fields[0]), Long.parseLong(fields[1])));
        });
        assertThat(statements).isNotEmpty();
        return statements;
    }

    private void reportTrial(int subscribers, int trial, long[] durations,
                             Map<String, SqlStats> before, Map<String, SqlStats> after) {
        long statements = 0;
        long deliveryLookups = 0;
        for (Map.Entry<String, SqlStats> entry : after.entrySet()) {
            SqlStats previous = before.getOrDefault(entry.getKey(), new SqlStats(0, 0));
            long count = entry.getValue().count() - previous.count();
            if (count == 0) {
                continue;
            }
            statements += count;
            if (isDeliveryLookup(entry.getKey())) {
                deliveryLookups += count;
            }
            long picoseconds = entry.getValue().picoseconds() - previous.picoseconds();
            System.out.printf(Locale.ROOT, "OUTBOX_SQL users=%d trial=%d count=%d db_ms=%.3f %s%n",
                    subscribers, trial, count, picoseconds / 1_000_000_000.0, entry.getKey());
        }
        assertThat(deliveryLookups).isPositive();
        assertThat(statements).isGreaterThan(deliveryLookups);
        double p50 = percentileMs(durations, 0.50);
        double p95 = percentileMs(durations, 0.95);
        System.out.printf(Locale.ROOT,
                "OUTBOX_TRIAL users=%d trial=%d samples=%d lookups=%d statements=%d p50_ms=%.3f p95_ms=%.3f%n",
                subscribers, trial, SAMPLES, deliveryLookups, statements, p50, p95);
        System.out.printf("OUTBOX_SAMPLES users=%d trial=%d nanoseconds=%s%n",
                subscribers, trial, Arrays.toString(durations));
    }

    private static boolean isDeliveryLookup(String digest) {
        String sql = digest.toLowerCase(Locale.ROOT);
        return sql.startsWith("select ") && !sql.startsWith("select count")
                && sql.contains("seat_notification_deliveries") && sql.contains("outbox_id");
    }

    private static double percentileMs(long[] durations, double quantile) {
        long[] sorted = Arrays.stream(durations).sorted().toArray();
        return sorted[(int) Math.ceil(sorted.length * quantile) - 1] / 1_000_000.0;
    }

    private static String courseKey(int subscribers) {
        return "benchmark-" + subscribers;
    }

    private static int benchmarkSetting(String name, int defaultValue) {
        return Integer.parseInt(System.getenv().getOrDefault(name, Integer.toString(defaultValue)));
    }

    private record SqlStats(long count, long picoseconds) {
    }

    @TestComponent
    @Configuration(proxyBeanMethods = false)
    @EnableJpaAuditing
    @EntityScan("bhoon.sugang_helper")
    @EnableJpaRepositories(basePackageClasses = {
        SeatNotificationOutboxRepository.class, SubscriptionRepository.class, UserRepository.class
    })
    @Import({SeatNotificationOutboxProcessor.class, SeatNotificationDeliveryStateService.class,
        SubscriptionQueryService.class, UserNotificationQueryService.class, NotificationChannelPolicy.class})
    static class Config {

        @Bean
        NotificationProperties notificationProperties() {
            return new NotificationProperties(true, true, true);
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
