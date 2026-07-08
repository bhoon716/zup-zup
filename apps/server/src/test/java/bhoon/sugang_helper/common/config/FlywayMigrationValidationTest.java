package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationValidationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("sugang_helper")
            .withUsername("test")
            .withPassword("test");

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
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
        assertThat(flyway.info().current()).isNotNull();
        flyway.validate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()));

        assertThat(tableCount(jdbcTemplate, "users")).isEqualTo(1);
        assertThat(tableCount(jdbcTemplate, "courses")).isEqualTo(1);
        assertThat(tableCount(jdbcTemplate, "crawler_settings")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT target_year FROM crawler_settings LIMIT 1", String.class))
                .isEqualTo("2026");
        assertThat(jdbcTemplate.queryForObject("SELECT target_semester FROM crawler_settings LIMIT 1", String.class))
                .isEqualTo("U211600010");
    }
}
