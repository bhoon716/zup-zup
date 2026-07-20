package bhoon.sugang_helper.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
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
        request.setQueryString("code=" + authorizationCode + "&state=" + state);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            new HttpLoggingFilter().doFilter(request, response, new MockFilterChain());

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages)
                    .doesNotContain(token, authorizationCode, state)
                    .contains("/api/v1/users/devices/token/[REDACTED]");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
