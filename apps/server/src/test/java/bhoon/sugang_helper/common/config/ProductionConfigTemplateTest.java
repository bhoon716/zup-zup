package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("운영 설정의 외부 값은 환경 변수로 주입한다")
    void productionConfigUsesEnvironmentPlaceholdersForExternalizedValues() throws IOException {
        // given
        String content = readClasspathResource("application-prod.yml");

        // when & then
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
        assertThat(content).contains("${APP_ADMIN_AUDIT_FINGERPRINT_SECRET:${JWT_SECRET}}");
        assertThat(content).contains("allowed-origins: ${APP_CORS_ALLOWED_ORIGINS}");
        assertThat(content).contains("success-redirect-uri: ${APP_OAUTH2_SUCCESS_REDIRECT_URI}");
        assertThat(content).contains("failure-redirect-uri: ${APP_OAUTH2_FAILURE_REDIRECT_URI}");
        assertThat(content).contains("authorized-redirect-uri: ${APP_OAUTH2_AUTHORIZED_REDIRECT_URI}");
        assertThat(content).contains("public-key: ${WEBPUSH_PUBLIC_KEY}");
        assertThat(content).contains("private-key: ${WEBPUSH_PRIVATE_KEY}");
        assertThat(content).contains("subject: ${WEBPUSH_SUBJECT}");
        assertThat(content).contains("bot-token: ${DISCORD_BOT_TOKEN}");
        assertThat(content).contains("client-id: ${DISCORD_CLIENT_ID}");
        assertThat(content).contains("client-secret: ${DISCORD_CLIENT_SECRET}");
        assertThat(content).contains("redirect-uri: ${DISCORD_REDIRECT_URI}");
    }

    @Test
    @DisplayName("운영 앱 실행 중에는 Flyway를 실행하지 않는다")
    void productionConfigDoesNotRunFlywayInsideTheRuntimeApplication() throws IOException {
        // given
        String content = readClasspathResource("application-prod.yml");

        // when & then
        assertThat(content).contains("enabled: false");
    }

    @Test
    @DisplayName("앱 시작 시 Spring Batch 작업을 자동 실행하지 않는다")
    void applicationDoesNotAutoLaunchSpringBatchJobsAtStartup() throws IOException {
        // given
        String content = readClasspathResource("application.yml");

        // when & then
        assertThat(content).contains("batch:\n    job:\n      enabled: false");
    }

    @Test
    @DisplayName("환경 변수 예시에 Google 콜백 주소를 선언한다")
    void environmentTemplateDeclaresGoogleRedirectUri() throws IOException {
        // given
        String content = Files.readString(Path.of(".env.example"));

        // when & then
        assertThat(content).contains("GOOGLE_REDIRECT_URI=");
    }

    @Test
    @DisplayName("운영 설정은 진단 엔드포인트와 개발 도구를 제한한다")
    void productionConfigRestrictsDiagnosticEndpointsAndDeveloperTools() throws IOException {
        // given
        String content = readClasspathResource("application-prod.yml");

        // when & then
        assertThat(content)
                .contains("h2:\n    console:\n      enabled: false")
                .contains("management:\n  server:\n    port: 8081")
                .contains("access:\n      default: none")
                .contains("include: health, prometheus")
                .contains("health:\n      access: read-only")
                .contains("probes:\n        enabled: true")
                .contains("group:\n        readiness:\n          include: readinessState,db,redis")
                .contains("prometheus:\n      access: read-only")
                .contains("require-https: ${APP_CORS_REQUIRE_HTTPS:true}")
                .contains("timeout: 30d")
                .contains("cookie:\n        secure: true")
                .doesNotContain("\njwt:\n")
                .doesNotContain("refresh-cookie-secure:")
                .doesNotContain("include: health, info, prometheus");
    }

    @Test
    @DisplayName("Redis 연결과 명령 실행에는 제한 시간을 적용한다")
    void redisClientUsesBoundedConnectionAndCommandTimeouts() throws IOException {
        // given
        String content = readClasspathResource("application.yml");

        // when & then
        assertThat(content)
                .contains("timeout: 2s")
                .contains("connect-timeout: 2s");
    }

}
