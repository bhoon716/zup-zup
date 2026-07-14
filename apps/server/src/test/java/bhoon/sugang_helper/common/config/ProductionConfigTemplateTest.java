package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProductionConfigTemplateTest {

    private static String readClasspathResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void productionConfigUsesEnvironmentPlaceholdersForExternalizedValues() throws IOException {
        String content = readClasspathResource("application-prod.yml");

        assertThat(content).contains("password: ${SPRING_DATASOURCE_PASSWORD}");
        assertThat(content).contains("url: ${SPRING_DATASOURCE_URL}");
        assertThat(content).contains("username: ${SPRING_DATASOURCE_USERNAME}");
        assertThat(content).contains("host: ${REDIS_HOST}");
        assertThat(content).contains("port: ${REDIS_PORT}");
        assertThat(content).contains("password: ${REDIS_PASSWORD}");
        assertThat(content).contains("client-id: ${GOOGLE_CLIENT_ID}");
        assertThat(content).contains("client-secret: ${GOOGLE_CLIENT_SECRET}");
        assertThat(content).contains("redirect-uri: ${GOOGLE_REDIRECT_URI}");
        assertThat(content).contains("access-key: ${AWS_ACCESS_KEY_ID}");
        assertThat(content).contains("secret-key: ${AWS_SECRET_ACCESS_KEY}");
        assertThat(content).contains("secret: ${JWT_SECRET}");
        assertThat(content).contains("allowed-origins: ${APP_CORS_ALLOWED_ORIGINS}");
        assertThat(content).contains("success-redirect-uri: ${APP_OAUTH2_SUCCESS_REDIRECT_URI}");
        assertThat(content).contains("failure-redirect-uri: ${APP_OAUTH2_FAILURE_REDIRECT_URI}");
        assertThat(content).contains("authorized-redirect-uri: ${APP_OAUTH2_AUTHORIZED_REDIRECT_URI}");
        assertThat(content).contains("config-path: ${FIREBASE_CONFIG_PATH}");
        assertThat(content).contains("public-key: ${WEBPUSH_PUBLIC_KEY}");
        assertThat(content).contains("private-key: ${WEBPUSH_PRIVATE_KEY}");
        assertThat(content).contains("subject: ${WEBPUSH_SUBJECT}");
        assertThat(content).contains("bot-token: ${DISCORD_BOT_TOKEN}");
        assertThat(content).contains("client-id: ${DISCORD_CLIENT_ID}");
        assertThat(content).contains("client-secret: ${DISCORD_CLIENT_SECRET}");
        assertThat(content).contains("redirect-uri: ${DISCORD_REDIRECT_URI}");
    }

    @Test
    void productionConfigDoesNotRunFlywayInsideTheRuntimeApplication() throws IOException {
        String content = readClasspathResource("application-prod.yml");

        assertThat(content).contains("enabled: false");
    }

    @Test
    void environmentTemplateDeclaresGoogleRedirectUri() throws IOException {
        String content = Files.readString(Path.of(".env.example"));

        assertThat(content).contains("GOOGLE_REDIRECT_URI=");
    }

    @Test
    void productionConfigRestrictsDiagnosticEndpointsAndDeveloperTools() throws IOException {
        String content = readClasspathResource("application-prod.yml");

        assertThat(content)
                .contains("h2:\n    console:\n      enabled: false")
                .contains("management:\n  server:\n    port: 8081")
                .contains("access:\n      default: none")
                .contains("include: health, prometheus")
                .contains("health:\n      access: read-only")
                .contains("probes:\n        enabled: true")
                .contains("group:\n        readiness:\n          include: readinessState,db,redis")
                .contains("prometheus:\n      access: read-only")
                .contains("api-docs:\n    enabled: false")
                .contains("swagger-ui:\n    enabled: false")
                .contains("require-https: true")
                .doesNotContain("include: health, info, prometheus");
    }

    @Test
    void redisClientUsesBoundedConnectionAndCommandTimeouts() throws IOException {
        String content = readClasspathResource("application.yml");

        assertThat(content)
                .contains("timeout: 2s")
                .contains("connect-timeout: 2s");
    }

    @Test
    void localJwtFallbackMeetsJjwtHmacMinimum() throws IOException {
        String content = readClasspathResource("application.yml");
        Matcher matcher = Pattern.compile("secret: \\$\\{JWT_SECRET:([^}]+)\\}").matcher(content);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1).getBytes(StandardCharsets.UTF_8)).hasSizeGreaterThanOrEqualTo(32);
    }
}
