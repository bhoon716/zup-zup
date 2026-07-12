package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import java.util.Map;

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

    @Test
    void freshMySqlSchemaMigratesAndSeedsCrawlerSettings() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available, skipping test"
        );

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
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available, skipping test"
        );

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

            Flyway head = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            assertThat(head.migrate().migrationsExecuted).isEqualTo(4);
            head.validate();
            assertThat(head.info().current().getVersion().getVersion()).isEqualTo("16");
        }
    }
}
