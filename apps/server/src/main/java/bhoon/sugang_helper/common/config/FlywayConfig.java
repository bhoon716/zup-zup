package bhoon.sugang_helper.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (isH2()) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }

    private boolean isH2() {
        return datasourceUrl != null && datasourceUrl.startsWith("jdbc:h2:");
    }
}
