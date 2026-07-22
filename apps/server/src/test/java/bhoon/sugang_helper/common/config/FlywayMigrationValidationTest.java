package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Tag("migration")
class FlywayMigrationValidationTest {

    private static final String DATABASE_NAME = "sugang_helper";
    private static final String DATABASE_USER = "test";
    private static final String DATABASE_PASSWORD = "test";
    private static final String MIGRATION_LOCATION = "classpath:db/migration";
    private static final String DELIVERY_TABLE_NAME = "seat_notification_deliveries";
    private static final String COURSES_TABLE_NAME = "courses";
    private static final String COURSE_KEY_COLUMN_NAME = "course_key";
    private static final Map<String, Integer> APPLIED_MIGRATION_CHECKSUMS = Map.ofEntries(
            Map.entry("1", 372551896),
            Map.entry("2", 2027923810),
            Map.entry("3", -753672566),
            Map.entry("5", -1294637235),
            Map.entry("6", 1614306126),
            Map.entry("7", 134496679),
            Map.entry("8", 1711173547),
            Map.entry("9", -1848533307),
            Map.entry("10", -878486489),
            Map.entry("11", -1102080157),
            Map.entry("12", -336202670)
    );

    @BeforeAll
    static void requireDocker() {
        requireDocker(DockerClientFactory.instance()::isDockerAvailable);
    }

    static void requireDocker(BooleanSupplier dockerAvailable) {
        if (!dockerAvailable.getAsBoolean()) {
            throw new IllegalStateException("Docker is required for migrationTest; Testcontainers cannot validate Flyway migrations");
        }
    }

    private static Integer tableCount(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                        """,
                Integer.class,
                tableName);
    }

    private static Integer indexCount(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        return jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName);
    }

    private static String columnDataType(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                        SELECT DATA_TYPE
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                String.class,
                tableName,
                columnName);
    }

    private static String columnCollation(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                        SELECT COLLATION_NAME
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                String.class,
                tableName,
                columnName);
    }

    @Test
    void freshMySqlSchemaMigratesAndSeedsCrawlerSettings() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USER)
                .withPassword(DATABASE_PASSWORD)
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")) {
            mysql.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations(MIGRATION_LOCATION)
                    .load();

            flyway.migrate();
            assertThat(flyway.info().current()).isNotNull();
            flyway.validate();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    mysql.getJdbcUrl(),
                    mysql.getUsername(),
                    mysql.getPassword()));

            assertThat(tableCount(jdbcTemplate, "users")).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, COURSES_TABLE_NAME)).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "crawler_settings")).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT target_year FROM crawler_settings LIMIT 1", String.class))
                    .isEqualTo("2026");
            assertThat(jdbcTemplate.queryForObject("SELECT target_semester FROM crawler_settings LIMIT 1", String.class))
                    .isEqualTo("U211600010");
            assertThat(columnDataType(jdbcTemplate, COURSES_TABLE_NAME, "course_direction")).isEqualTo("text");
            String longCourseDirection = "가".repeat(501);
            jdbcTemplate.update("""
                            INSERT INTO courses (
                                course_key, subject_code, name, class_number, academic_year, semester,
                                capacity, `current`, course_direction, last_crawled_at, average_rating, review_count
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(6), ?, ?)
                            """,
                    "2026:U211600010:LONG_DIRECTION:01", "LONG_DIRECTION", "긴 수업운영방향 강의", "01",
                    "2026", "U211600010", 30, 0, longCourseDirection, 0.0f, 0);
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT CHAR_LENGTH(course_direction)
                            FROM courses
                            WHERE course_key = ?
                            """, Integer.class, "2026:U211600010:LONG_DIRECTION:01")).isEqualTo(501);
            assertThat(columnCollation(jdbcTemplate, "course_emoji_reviews", COURSE_KEY_COLUMN_NAME))
                    .isEqualTo(columnCollation(jdbcTemplate, COURSES_TABLE_NAME, COURSE_KEY_COLUMN_NAME));
            assertThat(columnCollation(jdbcTemplate, "course_reviews", COURSE_KEY_COLUMN_NAME))
                    .isEqualTo(columnCollation(jdbcTemplate, COURSES_TABLE_NAME, COURSE_KEY_COLUMN_NAME));
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM course_emoji_reviews emoji_review
                            JOIN courses course ON course.course_key = emoji_review.course_key
                            """, Long.class)).isZero();
        }
    }

    @Test
    void existingSchemaHistoryValidatesBeforeApplyingNewMigrations() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USER)
                .withPassword(DATABASE_PASSWORD)) {
            mysql.start();

            Flyway appliedMigrations = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations(MIGRATION_LOCATION)
                    .target("12")
                    .load();
            appliedMigrations.migrate();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
            Map<String, Integer> recordedChecksums = jdbcTemplate.query("""
                            SELECT version, checksum
                            FROM flyway_schema_history
                            WHERE version IS NOT NULL
                            """,
                    resultSet -> {
                        Map<String, Integer> checksums = new java.util.HashMap<>();
                        while (resultSet.next()) {
                            checksums.put(resultSet.getString("version"), resultSet.getInt("checksum"));
                        }
                        return checksums;
                    });

            assertThat(recordedChecksums).containsAllEntriesOf(APPLIED_MIGRATION_CHECKSUMS);

            String existingUserEmail = "existing-user@example.com";
            jdbcTemplate.update("""
                            INSERT INTO users (
                                discord_enabled, email, email_enabled, fcm_enabled,
                                name, onboarding_completed, role, web_push_enabled
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    false, existingUserEmail, false, false, "Existing User", false, "USER", false);

            Long existingUserId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class,
                    existingUserEmail);
            jdbcTemplate.update("""
                            INSERT INTO admin_action_logs (admin_id, action_type, target_type, target_id, meta_data)
                            VALUES (?, 'REPLY_UPDATE', 'REPLY', ?, JSON_OBJECT('oldContent', 'legacy private reply'))
                            """,
                    existingUserId, 101L);
            jdbcTemplate.update("""
                            INSERT INTO admin_action_logs (admin_id, action_type, target_type, target_id, meta_data)
                            VALUES (?, 'STATUS_CHANGE', 'FEEDBACK', ?,
                                    JSON_OBJECT('oldStatus', 'PENDING', 'newStatus', 'COMPLETED'))
                            """,
                    existingUserId, 102L);
            jdbcTemplate.update("""
                            INSERT INTO admin_action_logs (admin_id, action_type, target_type, target_id, meta_data)
                            VALUES (?, 'REPLY_DELETE', 'REPLY', ?,
                                    JSON_OBJECT(
                                        'schema', 'admin-action',
                                        'version', 1,
                                        'event', 'LEGACY_SANITIZED',
                                        'data', JSON_OBJECT('reason', 'already_safe')
                                    ))
                            """,
                    existingUserId, 103L);

            Flyway throughV18 = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations(MIGRATION_LOCATION)
                    .target("18")
                    .load();

            assertThat(throughV18.migrate().migrationsExecuted).isEqualTo(6);
            throughV18.validate();
            assertThat(throughV18.info().current().getVersion().getVersion()).isEqualTo("18");
            assertThat(jdbcTemplate.queryForObject("SELECT version FROM users WHERE email = ?", Long.class,
                    existingUserEmail)).isZero();
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.event'))
                            FROM admin_action_logs
                            WHERE target_id = 101
                            """, String.class)).isEqualTo("LEGACY_SANITIZED");
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.oldContent'))
                            FROM admin_action_logs
                            WHERE target_id = 101
                            """, String.class)).isNull();
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.event'))
                            FROM admin_action_logs
                            WHERE target_id = 102
                            """, String.class)).isEqualTo("STATUS_CHANGED");
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.data.previousStatus'))
                            FROM admin_action_logs
                            WHERE target_id = 102
                            """, String.class)).isEqualTo("PENDING");
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.data.newStatus'))
                            FROM admin_action_logs
                            WHERE target_id = 102
                            """, String.class)).isEqualTo("COMPLETED");
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.data.reason'))
                            FROM admin_action_logs
                            WHERE target_id = 103
                    """, String.class)).isEqualTo("already_safe");
            assertThat(indexCount(jdbcTemplate, "admin_action_logs", "idx_admin_action_logs_created_at_id")).isEqualTo(2);

            jdbcTemplate.update("""
                            INSERT INTO seat_notification_outbox (
                                course_key, course_name, previous_seats, current_seats, status, version
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    "delivery-migration-course", "Delivery Migration Course", 0, 1, "DLQ", 0L);
            Long outboxId = jdbcTemplate.queryForObject(
                    "SELECT id FROM seat_notification_outbox WHERE course_key = ?", Long.class,
                    "delivery-migration-course");
            jdbcTemplate.update("""
                            INSERT INTO seat_notification_deliveries (
                                outbox_id, user_id, channel, status, attempts, next_attempt_at, locked_until, last_error
                            ) VALUES (?, ?, ?, ?, ?, NOW(6), ?, ?)
                            """,
                    outboxId, existingUserId, "EMAIL", "DLQ", 5, null, "N001");
            Long deliveryId = jdbcTemplate.queryForObject(
                    "SELECT id FROM seat_notification_deliveries WHERE outbox_id = ?", Long.class, outboxId);

            Flyway head = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations(MIGRATION_LOCATION)
                    .load();

            assertThat(head.migrate().migrationsExecuted).isEqualTo(6);
            head.validate();
            assertThat(head.info().current().getVersion().getVersion()).isEqualTo("24");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT idempotency_key FROM seat_notification_deliveries WHERE id = ?", String.class, deliveryId))
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT dead_lettered_at IS NOT NULL FROM seat_notification_deliveries WHERE id = ?",
                    Boolean.class, deliveryId)).isTrue();
            assertThat(indexCount(jdbcTemplate, DELIVERY_TABLE_NAME,
                    "uk_seat_notif_delivery_idempotency_key")).isEqualTo(1);
            assertThat(indexCount(jdbcTemplate, DELIVERY_TABLE_NAME,
                    "idx_seat_notif_delivery_dlq_retention")).isEqualTo(2);
            assertThat(columnDataType(jdbcTemplate, DELIVERY_TABLE_NAME, "idempotency_key"))
                    .isEqualTo("varchar");
            assertThat(columnDataType(jdbcTemplate, DELIVERY_TABLE_NAME, "claim_token"))
                    .isEqualTo("varchar");
        }
    }
}
