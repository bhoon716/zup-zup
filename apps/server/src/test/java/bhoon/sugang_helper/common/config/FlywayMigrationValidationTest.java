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

    @Test
    void freshMySqlSchemaMigratesAndSeedsCrawlerSettings() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USER)
                .withPassword(DATABASE_PASSWORD)) {
            mysql.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();
            assertThat(flyway.info().current()).isNotNull();
            flyway.validate();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    mysql.getJdbcUrl(),
                    mysql.getUsername(),
                    mysql.getPassword()));

            assertThat(tableCount(jdbcTemplate, "users")).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "courses")).isEqualTo(1);
            assertThat(tableCount(jdbcTemplate, "crawler_settings")).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT target_year FROM crawler_settings LIMIT 1", String.class))
                    .isEqualTo("2026");
            assertThat(jdbcTemplate.queryForObject("SELECT target_semester FROM crawler_settings LIMIT 1", String.class))
                    .isEqualTo("U211600010");
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
                    .locations("classpath:db/migration")
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

            Flyway head = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            assertThat(head.migrate().migrationsExecuted).isEqualTo(6);
            head.validate();
            assertThat(head.info().current().getVersion().getVersion()).isEqualTo("18");
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
        }
    }
}
