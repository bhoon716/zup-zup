package bhoon.sugang_helper.common.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpLoggingFilterTest {

    @ParameterizedTest
    @MethodSource("escapingFailures")
    void escapingFailuresAreNotLoggedAsSuccessfulResponses(Throwable failure) {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            assertThatThrownBy(() -> new HttpLoggingFilter().doFilter(
                    new MockHttpServletRequest("GET", "/api/test"), response, (request, result) -> {
                        if (failure instanceof IOException ioException) {
                            throw ioException;
                        }
                        if (failure instanceof ServletException servletException) {
                            throw servletException;
                        }
                        if (failure instanceof RuntimeException runtimeException) {
                            throw runtimeException;
                        }
                        throw (Error) failure;
                    })).isSameAs(failure);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage())
                        .contains("completed=false", "responseStatusAtExit=200", "exceptionType="
                                + failure.getClass().getSimpleName())
                        .doesNotContain("private-token", "private-cookie", "private@example.com");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static Stream<Throwable> escapingFailures() {
        String message = "Authorization=private-token Cookie=private-cookie email=private@example.com";
        return Stream.of(new IOException(message), new ServletException(message),
                new IllegalStateException(message), new AssertionError(message));
    }

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
