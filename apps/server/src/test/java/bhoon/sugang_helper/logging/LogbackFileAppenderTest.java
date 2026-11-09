package bhoon.sugang_helper.logging;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class LogbackFileAppenderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("콘솔과 파일에 추적 필드와 안전한 예외를 한 행 JSON으로 기록한다")
    void writesSearchableJsonLinesToConsoleAndConfiguredFile() throws Exception {
        // given
        Path logFile = tempDir.resolve("application.log");
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(MDC.getMDCAdapter());
        context.putProperty("LOG_FILE", logFile.toString());
        context.putObject(Environment.class.getName(), new MockEnvironment()
                .withProperty("logging.structured.json.add.environment", "TEST"));
        MDC.put("correlationId", "request-json-123");
        MDC.put("crawlerRunId", "crawl-json-456");
        String message = "한글과 \"따옴표\"\n두 번째 줄";
        Exception failure = new IllegalStateException("token=private-token",
                new IllegalArgumentException("cookie=private-cookie"));
        failure.addSuppressed(new IllegalArgumentException("email=private@example.invalid"));

        try (PrintStream capturedOut = new PrintStream(console, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            URL resource = Objects.requireNonNull(getClass().getClassLoader().getResource("logback-spring.xml"));
            configurator.doConfigure(resource);
            Logger logger = context.getLogger("json-output-test");

            // when
            logger.atError().addKeyValue("event", "json_output_test").addKeyValue("errorCode", "G001")
                    .addKeyValue("exceptionType", failure.getClass().getSimpleName())
                    .setCause(SensitiveDataRedactor.safeStackTrace(failure)).log(message);
            logger.info("다음 이벤트");
            context.stop();

            // then
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            assertThat(lines).hasSize(2);
            assertThat(console.toString(StandardCharsets.UTF_8).lines().toList()).isEqualTo(lines);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode first = mapper.readTree(lines.getFirst());
            assertThat(first.path("@timestamp").asText()).isNotBlank();
            assertThat(first.path("level").asText()).isEqualTo("ERROR");
            assertThat(first.path("message").asText()).isEqualTo(message);
            assertThat(first.path("environment").asText()).isEqualTo("TEST");
            assertThat(first.path("event").asText()).isEqualTo("json_output_test");
            assertThat(first.path("errorCode").asText()).isEqualTo("G001");
            assertThat(first.path("exceptionType").asText()).isEqualTo("IllegalStateException");
            assertThat(first.path("correlationId").asText()).isEqualTo("request-json-123");
            assertThat(first.path("crawlerRunId").asText()).isEqualTo("crawl-json-456");
            assertThat(first.path("stack_trace").asText()).contains("Caused by:", "Suppressed:", "원문 메시지 생략");
            assertThat(String.join("\n", lines)).doesNotContain("private-token", "private-cookie", "private@example.invalid");
            assertThat(mapper.readTree(lines.getLast()).path("message").asText()).isEqualTo("다음 이벤트");
        } finally {
            System.setOut(originalOut);
            context.stop();
            MDC.clear();
        }
    }
}
