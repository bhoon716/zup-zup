package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.enums.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;

/** CI의 migrationTest에서 빈 컨테이너와 합성 데이터만 사용하는 최종 DB 호환 E2E. */
@Tag("migration")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RetiredFcmMysqlEndToEndTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 12, 0);
    private static final List<SeatNotificationDeliveryStatus> ACTIVE = List.of(
            SeatNotificationDeliveryStatus.PENDING, SeatNotificationDeliveryStatus.PROCESSING);

    @Test
    void migratedMySqlKeepsLegacyFcmReadableAndExcludedFromSupportedDeliveryWork() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("sugang_helper")
                .withUsername("fixture")
                .withPassword("fixture")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")) {
            // given: 기존 마이그레이션 전체로 빈 DB를 만들고 상태마다 별도 사용자·outbox를 사용한다.
            mysql.start();
            Flyway flyway = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("classpath:db/migration")
                    .load();
            flyway.migrate();
            flyway.validate();
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
            List<Fixture> fixtures = seed(jdbc);
            LegacySnapshot before = snapshot(jdbc);

            try (SessionFactory factory = entityFactory(mysql);
                 EntityManager entityManager = factory.createEntityManager()) {
                SeatNotificationDeliveryRepository repository = new JpaRepositoryFactory(entityManager)
                        .getRepository(SeatNotificationDeliveryRepository.class);
                assertLegacyMappings(entityManager, fixtures);
                EntityTransaction transaction = entityManager.getTransaction();
                transaction.begin();
                try {
                    // when: 실제 JPQL·파생 쿼리를 잠금·트랜잭션 안에서 실행한다.
                    List<SeatNotificationDelivery> ready = repository.findReadyForUpdate(
                            SeatNotificationDeliveryStatus.PENDING, SeatNotificationDeliveryStatus.PROCESSING,
                            NOW, NotificationChannel.FCM, PageRequest.of(0, 20));

                    // then: WEB 대조군의 준비 작업만 나오고 모든 FCM 상태는 집계에서 제외한다.
                    assertThat(ready).extracting(SeatNotificationDelivery::getId)
                            .containsExactlyElementsOf(fixtures.stream()
                                    .filter(fixture -> fixture.channel() == NotificationChannel.WEB
                                            && ACTIVE.contains(fixture.status()))
                                    .map(Fixture::id).toList());
                    for (Fixture fixture : fixtures) {
                        long expected = fixture.channel() == NotificationChannel.FCM ? 0L : 1L;
                        assertThat(repository.countByOutboxIdAndStatusAndChannelNot(
                                fixture.outboxId(), fixture.status(), NotificationChannel.FCM))
                                .isEqualTo(expected);
                        assertThat(repository.countByOutboxIdAndStatusInAndChannelNot(
                                fixture.outboxId(), ACTIVE, NotificationChannel.FCM))
                                .isEqualTo(ACTIVE.contains(fixture.status()) ? expected : 0L);
                    }
                    assertThat(repository.deleteByStatusAndDeadLetteredAtBeforeAndChannelNot(
                            SeatNotificationDeliveryStatus.DLQ, NOW.minusDays(30), NotificationChannel.FCM))
                            .isEqualTo(1L);
                    transaction.commit();
                } catch (RuntimeException | Error failure) {
                    if (transaction.isActive()) {
                        transaction.rollback();
                    }
                    throw failure;
                }
            }

            // then: FCM 사용자·기기·알림·작업의 원래 값은 그대로이며 WEB의 기존 보존 기간만 적용한다.
            assertThat(snapshot(jdbc)).isEqualTo(before);
            for (Fixture fixture : fixtures) {
                Long remaining = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM seat_notification_deliveries WHERE id = ?", Long.class, fixture.id());
                boolean expiredWeb = fixture.channel() == NotificationChannel.WEB
                        && fixture.status() == SeatNotificationDeliveryStatus.DLQ
                        && fixture.deadLetteredAt().isBefore(NOW.minusDays(30));
                assertThat(remaining).isEqualTo(expiredWeb ? 0L : 1L);
            }
        }
    }

    private static SessionFactory entityFactory(MySQLContainer<?> mysql) {
        return new Configuration()
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(UserDevice.class)
                .addAnnotatedClass(NotificationHistory.class)
                .addAnnotatedClass(SeatNotificationOutbox.class)
                .addAnnotatedClass(SeatNotificationDelivery.class)
                .setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy())
                .setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver")
                .setProperty("hibernate.connection.url", mysql.getJdbcUrl())
                .setProperty("hibernate.connection.username", mysql.getUsername())
                .setProperty("hibernate.connection.password", mysql.getPassword())
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .buildSessionFactory();
    }

    private static List<Fixture> seed(JdbcTemplate jdbc) {
        List<Fixture> fixtures = new ArrayList<>();
        for (NotificationChannel channel : List.of(NotificationChannel.FCM, NotificationChannel.WEB)) {
            for (SeatNotificationDeliveryStatus status : SeatNotificationDeliveryStatus.values()) {
                fixtures.add(new Fixture(fixtures.size() + 1L, channel, status,
                        status == SeatNotificationDeliveryStatus.DLQ ? NOW.minusDays(60) : null));
            }
        }
        fixtures.add(new Fixture(fixtures.size() + 1L, NotificationChannel.WEB,
                SeatNotificationDeliveryStatus.DLQ, NOW.minusDays(10)));
        for (Fixture fixture : fixtures) {
            seedDelivery(jdbc, fixture);
        }
        Fixture legacy = fixtures.get(0);
        Fixture web = fixtures.stream().filter(fixture -> fixture.channel() == NotificationChannel.WEB)
                .findFirst().orElseThrow();
        jdbc.update("""
                INSERT INTO user_devices (id, user_id, type, token, alias, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 1L, legacy.userId(), "FCM", "synthetic-legacy-token", "합성 앱 기기", NOW, NOW);
        jdbc.update("""
                INSERT INTO user_devices (id, user_id, type, token, p256dh, auth, alias, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 2L, web.userId(), "WEB", "https://example.invalid/standard-push",
                "synthetic-p256", "synthetic-auth", "합성 웹 기기", NOW, NOW);
        jdbc.update("""
                INSERT INTO notification_histories
                    (id, user_id, course_key, title, message, channel, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, 1L, legacy.userId(), "fixture-course", "합성 과거 알림", "FCM 읽기 호환 확인", "FCM", NOW, NOW);
        return fixtures;
    }

    private static void seedDelivery(JdbcTemplate jdbc, Fixture fixture) {
        boolean legacy = fixture.channel() == NotificationChannel.FCM;
        jdbc.update("""
                INSERT INTO users (id, created_at, updated_at, discord_enabled, email, email_enabled,
                    fcm_enabled, name, onboarding_completed, role, web_push_enabled, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, fixture.userId(), NOW, NOW, false, "fixture-" + fixture.id() + "@example.invalid",
                false, legacy, "합성 사용자", true, "USER", !legacy, 0L);
        jdbc.update("""
                INSERT INTO seat_notification_outbox (id, course_key, course_name, previous_seats,
                    current_seats, status, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, fixture.outboxId(), "fixture-course-" + fixture.id(), "합성 강의", 0, 1,
                "PROCESSING", 0L, NOW, NOW);
        jdbc.update("""
                INSERT INTO seat_notification_deliveries (id, outbox_id, user_id, idempotency_key,
                    channel, status, attempts, next_attempt_at, locked_until, claim_token, last_error,
                    dead_lettered_at, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, fixture.id(), fixture.outboxId(), fixture.userId(), fixture.key(),
                fixture.channel().name(), fixture.status().name(), 3, NOW.minusHours(2),
                fixture.status() == SeatNotificationDeliveryStatus.PROCESSING ? NOW.minusHours(1) : null,
                fixture.status() == SeatNotificationDeliveryStatus.PROCESSING ? "fixture-claim-" + fixture.id() : null,
                legacy ? "N002" : "N010", fixture.deadLetteredAt(), 0L, NOW, NOW);
    }

    private static void assertLegacyMappings(EntityManager entityManager, List<Fixture> fixtures) {
        for (Fixture fixture : fixtures) {
            SeatNotificationDelivery delivery = entityManager.find(SeatNotificationDelivery.class, fixture.id());
            assertThat(delivery.getChannel()).isEqualTo(fixture.channel());
            assertThat(delivery.getStatus()).isEqualTo(fixture.status());
            assertThat(delivery.getAttempts()).isEqualTo(3);
            assertThat(delivery.getIdempotencyKey()).isEqualTo(fixture.key());
            if (fixture.channel() == NotificationChannel.FCM) {
                assertThat(delivery.getLastError()).isEqualTo("N002");
                assertThat(entityManager.find(User.class, fixture.userId()).isFcmEnabled()).isTrue();
            }
        }
        assertThat(entityManager.find(UserDevice.class, 1L).getType()).isEqualTo(DeviceType.FCM);
        assertThat(entityManager.find(UserDevice.class, 2L).getType()).isEqualTo(DeviceType.WEB);
        assertThat(entityManager.find(NotificationHistory.class, 1L).getChannel()).isEqualTo(NotificationChannel.FCM);
    }

    private static LegacySnapshot snapshot(JdbcTemplate jdbc) {
        return new LegacySnapshot(
                jdbc.queryForList("SELECT * FROM seat_notification_deliveries WHERE channel = 'FCM' ORDER BY id"),
                jdbc.queryForList("SELECT * FROM user_devices WHERE type = 'FCM' ORDER BY id"),
                jdbc.queryForList("SELECT * FROM notification_histories WHERE channel = 'FCM' ORDER BY id"),
                jdbc.queryForList("""
                        SELECT id, CAST(fcm_enabled AS UNSIGNED) AS fcm_enabled, version, created_at, updated_at
                        FROM users WHERE fcm_enabled = 1 ORDER BY id
                        """));
    }

    private record Fixture(long id, NotificationChannel channel, SeatNotificationDeliveryStatus status,
                           LocalDateTime deadLetteredAt) {
        long outboxId() {
            return id + 1_000L;
        }

        long userId() {
            return id + 2_000L;
        }

        String key() {
            return new UUID(0L, id).toString();
        }
    }

    private record LegacySnapshot(List<Map<String, Object>> deliveries, List<Map<String, Object>> devices,
                                  List<Map<String, Object>> histories, List<Map<String, Object>> users) {
    }
}
