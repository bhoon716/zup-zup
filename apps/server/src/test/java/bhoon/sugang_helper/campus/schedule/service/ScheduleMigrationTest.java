package bhoon.sugang_helper.campus.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ScheduleMigrationTest {

    @Test
    void additiveMigrationPreservesScheduleAndLegacyDdayDataWithSelectionDisabled() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:schedule-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE schedules (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    schedule_type VARCHAR(50) NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    start_time TIME,
                    end_time TIME,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE dday_settings (id BIGINT PRIMARY KEY, title VARCHAR(255) NOT NULL)");
        jdbc.update("INSERT INTO schedules (schedule_type, start_date, end_date) VALUES (?, ?, ?)",
                "기존 일정", "2026-10-26", "2026-10-27");
        jdbc.update("INSERT INTO dday_settings (id, title) VALUES (1, ?)", "이전 디데이");
        Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("28").target("29").load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
        assertThat(jdbc.queryForObject("SELECT schedule_type FROM schedules WHERE id = 1", String.class))
                .isEqualTo("기존 일정");
        assertThat(jdbc.queryForObject("SELECT dday_enabled FROM schedules WHERE id = 1", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM schedules WHERE id = 1", Object.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT title FROM dday_settings WHERE id = 1", String.class))
                .isEqualTo("이전 디데이");
        String expandedTitle = "가".repeat(255);
        jdbc.update("UPDATE schedules SET schedule_type = ? WHERE id = 1", expandedTitle);
        assertThat(jdbc.queryForObject("SELECT schedule_type FROM schedules WHERE id = 1", String.class))
                .isEqualTo(expandedTitle);
        jdbc.execute("SHUTDOWN");
    }
}
