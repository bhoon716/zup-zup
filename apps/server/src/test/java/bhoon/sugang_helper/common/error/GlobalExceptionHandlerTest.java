package bhoon.sugang_helper.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.response.ErrorResponse;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void customExceptionDoesNotExposeSecretDetailOrTokenPath() {
        String token = "fcm-token-should-not-appear";
        String email = "recipient@example.com";
        String detail = "recipient=" + email + ", token=" + token;
        MockHttpServletRequest request = new MockHttpServletRequest(
                "DELETE", "/api/v1/users/devices/token/" + token);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                    .handleCustomException(request, new CustomException(ErrorCode.EMAIL_SEND_ERROR, detail));

            assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.EMAIL_SEND_ERROR.getMessage());
            assertThat(response.getBody().getDetails()).isNull();
            assertThat(response.getBody().getPath()).doesNotContain(token);
            assertThat(response.getBody().getCorrelationId()).isNotBlank();
            assertThat(response.getHeaders().getFirst("X-Error-Id"))
                    .isEqualTo(response.getBody().getCorrelationId());
            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain(token, email);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
