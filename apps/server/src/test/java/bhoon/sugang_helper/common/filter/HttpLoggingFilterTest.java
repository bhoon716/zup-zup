package bhoon.sugang_helper.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpLoggingFilterTest {

    @Test
    void doesNotLogOAuthQueryValuesOrTokenPathSegments() throws Exception {
        String token = "fcm-token-should-not-appear";
        String authorizationCode = "oauth-code-should-not-appear";
        String state = "oauth-state-should-not-appear";
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/users/devices/token/" + token);
        String queryString = "code=" + authorizationCode + "&state=" + state;
        request.setQueryString(queryString);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        Level previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);

        try {
            new HttpLoggingFilter().doFilter(request, response, new MockFilterChain());

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages)
                    .contains("queryPresent=true")
                    .doesNotContain(queryString, token, authorizationCode, state)
                    .contains("/api/v1/users/devices/token/[REDACTED]");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previous);
        }
    }
    @Test
    void logsUnreportedFailuresAndAvoidsDuplicateWarningsForHandledErrors() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            for (int status : new int[]{200, 403, 500}) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                response.setStatus(status);
                new HttpLoggingFilter().doFilter(new MockHttpServletRequest("GET", "/api/test"), response,
                        new MockFilterChain());
            }
            MockHttpServletResponse handled = new MockHttpServletResponse();
            handled.setStatus(500);
            handled.setHeader("X-Error-Id", "handled-error");
            new HttpLoggingFilter().doFilter(new MockHttpServletRequest("GET", "/api/test"), handled,
                    new MockFilterChain());
            MockHttpServletResponse health = new MockHttpServletResponse();
            health.setStatus(503);
            new HttpLoggingFilter().doFilter(new MockHttpServletRequest("GET", "/health"), health,
                    new MockFilterChain());
            assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                    .containsExactly(Level.DEBUG, Level.WARN, Level.ERROR, Level.DEBUG, Level.DEBUG);
            assertThat(appender.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("요청 처리 결과", "elapsedMs="));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previous);
        }
    }
}
