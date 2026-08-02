package bhoon.sugang_helper.common.config;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.common.health.HealthController;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = DatabaseReadinessHealthTest.TestApplication.class)
@AutoConfigureMockMvc
@Import(HealthController.class)
@TestPropertySource(properties = {
        "management.endpoint.health.probes.enabled=true",
        "management.endpoint.health.group.readiness.include=readinessState,db",
        "management.endpoint.health.show-details=never",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class DatabaseReadinessHealthTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetDataSource() {
        reset(dataSource);
    }

    @Test
    void externalHealthIsDownWhenDatabaseCannotBeReached() throws Exception {
        given(dataSource.getConnection())
                .willThrow(new SQLException("Database is unavailable"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.status").value("DOWN"))
                .andExpect(jsonPath("$.data.details").doesNotExist());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            SessionAutoConfiguration.class,
            OAuth2ClientWebSecurityAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class,
            MultipartAutoConfiguration.class,
            ErrorMvcAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        BuildProperties buildProperties() {
            Properties properties = new Properties();
            properties.setProperty("version", "test-version");
            return new BuildProperties(properties);
        }

        @Bean
        DataSource dataSource() {
            return mock(DataSource.class);
        }
    }
}
