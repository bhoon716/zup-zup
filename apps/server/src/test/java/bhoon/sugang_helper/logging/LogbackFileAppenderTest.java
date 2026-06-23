package bhoon.sugang_helper.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter2;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LogbackFileAppenderTest {

    @Test
    void writesLogsToTheConfiguredHostFilePath() throws Exception {
        Path tempDir = Files.createTempDirectory("jbnu-logback-test");
        Path logFile = tempDir.resolve("application.log");

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        context.setName("logback-file-test");
        context.putProperty("LOG_FILE", logFile.toString());

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(
                LogbackFileAppenderTest.class.getClassLoader().getResource("logback-spring.xml"));

        Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.info("host-file-log-test");

        Thread.sleep(100);
        context.stop();
        new StatusPrinter2().printInCaseOfErrorsOrWarnings(context);

        assertTrue(Files.exists(logFile), "log file should be created");
        String contents = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(contents.contains("host-file-log-test"), "log file should contain emitted log line");
    }
}
