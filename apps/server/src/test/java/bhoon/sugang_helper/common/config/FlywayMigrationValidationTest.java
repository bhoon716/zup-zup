package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

@Tag("migration")
class FlywayMigrationValidationTest {

    private static final String DATABASE_NAME = "sugang_helper";
    private static final String DATABASE_USER = "test";
    private static final String DATABASE_PASSWORD = "test";
    private static final String MYSQL_IMAGE = "mysql:8.4";
    private static final String MIGRATION_LOCATION = "classpath:db/migration";
    private static final String DELIVERY_TABLE_NAME = "seat_notification_deliveries";
    private static final String COURSES_TABLE_NAME = "courses";
    private static final String USERS_TABLE_NAME = "users";
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

    private static void assertAllCourseKeyCollationsMatchCourses(JdbcTemplate jdbcTemplate) {
        String coursesCollation = columnCollation(jdbcTemplate, COURSES_TABLE_NAME, COURSE_KEY_COLUMN_NAME);
        Map<String, String> courseKeyCollations = jdbcTemplate.query("""
                        SELECT TABLE_NAME, COLLATION_NAME
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND column_name = ?
                        ORDER BY table_name
                        """,
                resultSet -> {
                    Map<String, String> collations = new java.util.LinkedHashMap<>();
                    while (resultSet.next()) {
                        collations.put(resultSet.getString("TABLE_NAME"), resultSet.getString("COLLATION_NAME"));
                    }
                    return collations;
                },
                COURSE_KEY_COLUMN_NAME);

        assertThat(courseKeyCollations).isNotEmpty();
        assertThat(courseKeyCollations).allSatisfy((tableName, collation) ->
                assertThat(collation)
                        .as("%s.%s collation", tableName, COURSE_KEY_COLUMN_NAME)
                        .isEqualTo(coursesCollation));
    }

    private static String columnNullable(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                        SELECT IS_NULLABLE
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
        try (MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE)
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

            assertThat(tableCount(jdbcTemplate, USERS_TABLE_NAME)).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, COURSES_TABLE_NAME)).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "crawler_settings")).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "crawler_status")).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "crawler_run_failures")).isEqualTo(1);
            assertThat(columnNullable(jdbcTemplate, USERS_TABLE_NAME, "oauth_issuer")).isEqualTo("YES");
            assertThat(columnNullable(jdbcTemplate, USERS_TABLE_NAME, "oauth_subject")).isEqualTo("YES");
            assertThat(indexCount(jdbcTemplate, USERS_TABLE_NAME, "uk_users_oauth_identity")).isEqualTo(2);
            jdbcTemplate.update("""
                            INSERT INTO users (
                                discord_enabled, email, email_enabled, fcm_enabled, name,
                                onboarding_completed, role, web_push_enabled, oauth_issuer, oauth_subject
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    false, "oauth-user@example.com", false, false, "OAuth User", false, "USER", false,
                    "https://accounts.google.com", "unique-subject");
            assertThatThrownBy(() -> jdbcTemplate.update("""
                            INSERT INTO users (
                                discord_enabled, email, email_enabled, fcm_enabled, name,
                                onboarding_completed, role, web_push_enabled, oauth_issuer, oauth_subject
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    false, "other-oauth-user@example.com", false, false, "Other OAuth User", false, "USER", false,
                    "https://accounts.google.com", "unique-subject"))
                    .hasMessageContaining("uk_users_oauth_identity");
            assertThat(columnNullable(jdbcTemplate, COURSES_TABLE_NAME, "last_crawled_at")).isEqualTo("YES");
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
            assertAllCourseKeyCollationsMatchCourses(jdbcTemplate);
            assertOnlyRequiredNonUniqueIndexes(jdbcTemplate);
            assertThat(columnCollation(jdbcTemplate, "course_emoji_reviews", "emoji"))
                    .isEqualTo("utf8mb4_0900_bin");
            jdbcTemplate.update("""
                            INSERT INTO course_emoji_reviews (course_key, user_id, emoji)
                            VALUES (?, ?, ?)
                            """,
                    "emoji-collation-course", 4242L, "😀");
            jdbcTemplate.update("""
                            INSERT INTO course_emoji_reviews (course_key, user_id, emoji)
                            VALUES (?, ?, ?)
                            """,
                    "emoji-collation-course", 4242L, "🤫");
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM course_emoji_reviews
                            WHERE course_key = ?
                              AND user_id = ?
                            """,
                    Long.class, "emoji-collation-course", 4242L)).isEqualTo(2L);
            assertThat(jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM course_emoji_reviews emoji_review
                            JOIN courses course ON course.course_key = emoji_review.course_key
                            """, Long.class)).isZero();
        }
    }

    @Test
    void existingSchemaHistoryValidatesBeforeApplyingNewMigrations() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE)
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

            jdbcTemplate.update("""
                    INSERT INTO schedules (id, schedule_type, start_date, end_date)
                    VALUES (501, ?, '2026-10-26', '2026-10-27')
                    """, "보존 일정");
            jdbcTemplate.update("""
                    INSERT INTO dday_settings (id, title, target_date)
                    VALUES (502, ?, '2026-10-26')
                    """, "옛 디데이");

            Flyway throughV29 = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations(MIGRATION_LOCATION)
                    .target("29")
                    .load();

            assertThat(throughV29.migrate().migrationsExecuted).isEqualTo(11);
            throughV29.validate();
            assertThat(throughV29.info().current().getVersion().getVersion()).isEqualTo("29");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT oauth_issuer IS NULL FROM users WHERE email = ?", Boolean.class, existingUserEmail))
                    .isTrue();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT oauth_subject IS NULL FROM users WHERE email = ?", Boolean.class, existingUserEmail))
                    .isTrue();
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
            assertThat(tableCount(jdbcTemplate, "crawler_status")).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "crawler_run_failures")).isEqualTo(1);
            assertThat(columnNullable(jdbcTemplate, COURSES_TABLE_NAME, "last_crawled_at")).isEqualTo("YES");
            assertAllCourseKeyCollationsMatchCourses(jdbcTemplate);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT schedule_type FROM schedules WHERE id = 501", String.class)).isEqualTo("보존 일정");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT dday_enabled FROM schedules WHERE id = 501", Boolean.class)).isFalse();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT deleted_at FROM schedules WHERE id = 501", Object.class)).isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT title FROM dday_settings WHERE id = 502", String.class)).isEqualTo("옛 디데이");
            String expandedScheduleTitle = "가".repeat(255);
            jdbcTemplate.update("UPDATE schedules SET schedule_type = ? WHERE id = 501", expandedScheduleTitle);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT CHAR_LENGTH(schedule_type) FROM schedules WHERE id = 501", Integer.class)).isEqualTo(255);
            assertPerformanceIndexRemoval(jdbcTemplate, mysql, existingUserId, outboxId, deliveryId);

        }
    }
    private static void assertOnlyRequiredNonUniqueIndexes(JdbcTemplate jdbc) {
        List<String> optionalIndexes = jdbc.queryForList("""
                SELECT DISTINCT CONCAT(s.TABLE_NAME, '.', s.INDEX_NAME)
                FROM information_schema.statistics s
                WHERE s.TABLE_SCHEMA = DATABASE() AND s.NON_UNIQUE = 1
                  AND s.TABLE_NAME <> 'flyway_schema_history'
                  AND NOT EXISTS (
                      SELECT 1 FROM information_schema.key_column_usage k
                      WHERE k.CONSTRAINT_SCHEMA = s.TABLE_SCHEMA AND k.TABLE_NAME = s.TABLE_NAME
                        AND k.REFERENCED_TABLE_NAME IS NOT NULL AND k.ORDINAL_POSITION = 1
                        AND k.COLUMN_NAME = s.COLUMN_NAME AND s.SEQ_IN_INDEX = 1
                  )
                  AND s.SEQ_IN_INDEX = 1
                """, String.class);
        assertThat(optionalIndexes).as("성능 전용 비유니크 인덱스가 남지 않음").isEmpty();
    }

    private static Map<String, String> nonUniqueIndexes(JdbcTemplate jdbc) {
        return jdbc.query("""
                SELECT TABLE_NAME, INDEX_NAME,
                       GROUP_CONCAT(CONCAT('`', COLUMN_NAME, '`', IF(COLLATION = 'D', ' DESC', ''))
                                    ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns_sql
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = DATABASE() AND NON_UNIQUE = 1
                  AND TABLE_NAME <> 'flyway_schema_history'
                GROUP BY TABLE_NAME, INDEX_NAME ORDER BY TABLE_NAME, INDEX_NAME
                """, resultSet -> {
                    Map<String, String> indexes = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        String key = resultSet.getString("TABLE_NAME") + "." + resultSet.getString("INDEX_NAME");
                        indexes.put(key, resultSet.getString("columns_sql"));
                    }
                    return indexes;
                });
    }

    private static List<Map<String, Object>> constraints(JdbcTemplate jdbc) {
        return jdbc.queryForList("""
                SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, ORDINAL_POSITION,
                       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
                FROM information_schema.key_column_usage
                WHERE CONSTRAINT_SCHEMA = DATABASE()
                ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION
                """);
    }

    private static void assertPerformanceIndexRemoval(JdbcTemplate jdbc, MySQLContainer<?> mysql,
                                                       Long userId, Long outboxId, Long deliveryId) {
        String historyCourse = "index-removal-history";
        jdbc.update("INSERT INTO subscriptions (user_id, course_key, is_active) VALUES (?, ?, 1)", userId, historyCourse);
        jdbc.update("INSERT INTO wishlists (user_id, course_key) VALUES (?, ?)", userId, historyCourse);
        for (int i = 0; i < 32; i++) {
            jdbc.update("""
                    INSERT INTO notification_histories (user_id, course_key, title, message, channel, created_at)
                    VALUES (?, ?, '검증 알림', '동일 시각 정렬', 'EMAIL', '2026-10-26 12:00:00')
                    """, userId, historyCourse);
            jdbc.update("""
                    INSERT INTO course_seat_histories (course_key, capacity, `current`, created_at)
                    VALUES (?, 30, ?, '2026-10-26 12:00:00')
                    """, historyCourse, i);
        }
        Map<String, String> beforeIndexes = nonUniqueIndexes(jdbc);
        List<Map<String, Object>> beforeConstraints = constraints(jdbc);
        List<Long> beforeNotifications = notificationPage(jdbc, userId, 0);
        List<Long> beforeSeats = seatHistoryPage(jdbc, historyCourse, 0);
        String idempotencyKey = jdbc.queryForObject(
                "SELECT idempotency_key FROM seat_notification_deliveries WHERE id = ?", String.class, deliveryId);
        Flyway head = Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations(MIGRATION_LOCATION).load();
        assertThat(head.migrate().migrationsExecuted).isEqualTo(1);
        head.validate();
        assertThat(head.info().current().getVersion().getVersion()).isEqualTo("30");
        Map<String, String> afterIndexes = nonUniqueIndexes(jdbc);
        Set<String> removed = new TreeSet<>(beforeIndexes.keySet());
        removed.removeAll(afterIndexes.keySet());
        assertThat(removed).hasSize(22);
        assertOnlyRequiredNonUniqueIndexes(jdbc);
        assertThat(constraints(jdbc)).containsExactlyElementsOf(beforeConstraints);
        assertThat(notificationPage(jdbc, userId, 0)).containsExactlyElementsOf(beforeNotifications).hasSize(30);
        assertThat(notificationPage(jdbc, userId, 30)).hasSize(2).doesNotContainAnyElementsOf(beforeNotifications);
        assertThat(notificationPage(jdbc, userId + 1, 0)).isEmpty();
        assertThat(seatHistoryPage(jdbc, historyCourse, 0)).containsExactlyElementsOf(beforeSeats).hasSize(30);
        assertThat(seatHistoryPage(jdbc, historyCourse, 30)).hasSize(2).doesNotContainAnyElementsOf(beforeSeats);
        assertThat(jdbc.queryForObject("SELECT idempotency_key FROM seat_notification_deliveries WHERE id = ?",
                String.class, deliveryId)).isEqualTo(idempotencyKey);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO subscriptions (user_id, course_key, is_active) VALUES (?, ?, 1)", userId, historyCourse))
                .hasMessageContaining("uk_subscription_user_course");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO wishlists (user_id, course_key) VALUES (?, ?)", userId, historyCourse))
                .hasMessageContaining("uk_wishlist_user_course");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO seat_notification_deliveries
                    (outbox_id, user_id, idempotency_key, channel, status, attempts, next_attempt_at)
                VALUES (?, ?, UUID(), 'EMAIL', 'PENDING', 0, NOW(6))
                """, outboxId, userId)).hasMessageContaining("uk_seat_notif_delivery_outbox_user_channel");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO seat_notification_deliveries
                    (outbox_id, user_id, idempotency_key, channel, status, attempts, next_attempt_at)
                VALUES (?, ?, ?, 'WEB_PUSH', 'PENDING', 0, NOW(6))
                """, outboxId, userId, idempotencyKey)).hasMessageContaining("uk_seat_notif_delivery_idempotency_key");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO seat_notification_deliveries
                    (outbox_id, user_id, idempotency_key, channel, status, attempts, next_attempt_at)
                VALUES (?, ?, UUID(), 'EMAIL', 'PENDING', 0, NOW(6))
                """, outboxId + 9999, userId)).hasMessageContaining("fk_seat_notif_delivery_outbox");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM seat_notification_outbox WHERE id = ?", outboxId))
                .hasMessageContaining("fk_seat_notif_delivery_outbox");
        jdbc.update("""
                UPDATE seat_notification_deliveries SET status = 'PENDING', next_attempt_at = NOW(6), locked_until = NULL
                WHERE id = ?
                """, deliveryId);
        assertThat(jdbc.queryForList("""
                SELECT id FROM seat_notification_deliveries
                WHERE (status = 'PENDING' AND next_attempt_at <= NOW(6))
                   OR (status = 'PROCESSING' AND locked_until < NOW(6))
                ORDER BY next_attempt_at, id LIMIT 30 FOR UPDATE
                """, Long.class)).contains(deliveryId);
        jdbc.update("UPDATE seat_notification_deliveries SET status = 'PROCESSING', claim_token = 'current-claim' WHERE id = ?",
                deliveryId);
        assertThat(jdbc.update("""
                UPDATE seat_notification_deliveries SET status = 'SENT'
                WHERE id = ? AND status = 'PROCESSING' AND claim_token = 'stale-claim'
                """, deliveryId)).isZero();
        assertThat(jdbc.update("""
                UPDATE seat_notification_deliveries SET status = 'SENT'
                WHERE id = ? AND status = 'PROCESSING' AND claim_token = 'current-claim'
                """, deliveryId)).isEqualTo(1);
        for (String key : removed) {
            String[] names = key.split("\\.", 2);
            jdbc.execute("CREATE INDEX `" + names[1] + "` ON `" + names[0] + "` (" + beforeIndexes.get(key) + ")");
        }
        assertThat(nonUniqueIndexes(jdbc)).isEqualTo(beforeIndexes);
        assertThat(constraints(jdbc)).containsExactlyElementsOf(beforeConstraints);
    }

    private static List<Long> notificationPage(JdbcTemplate jdbc, Long userId, int offset) {
        return jdbc.queryForList("""
                SELECT id FROM notification_histories WHERE user_id = ?
                ORDER BY created_at DESC, id DESC LIMIT 30 OFFSET ?
                """, Long.class, userId, offset);
    }

    private static List<Long> seatHistoryPage(JdbcTemplate jdbc, String courseKey, int offset) {
        return jdbc.queryForList("""
                SELECT id FROM course_seat_histories WHERE course_key = ?
                ORDER BY created_at DESC, id DESC LIMIT 30 OFFSET ?
                """, Long.class, courseKey, offset);
    }
}
