package bhoon.sugang_helper.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DotenvConfig {

    private final ConfigurableEnvironment environment;

    public DotenvConfig(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> dotenvMap = new HashMap<>();
            for (DotenvEntry entry : dotenv.entries()) {
                dotenvMap.put(entry.getKey(), entry.getValue());
            }

            if (!dotenvMap.isEmpty()) {
                environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties", dotenvMap));
            }
        } catch (Exception ignore) {
        }
    }
}
