package bhoon.sugang_helper.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.response.ErrorResponse;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @Test
    void optimisticLockConflictReturnsRetryableConflictResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/users/me");

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleOptimisticLock(request, new ObjectOptimisticLockingFailureException("User", 1L));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.getCode());
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void unreadableJsonReturnsInvalidInputResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/feedbacks");

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleUnreadableMessage(request, new HttpMessageNotReadableException(
                        "invalid JSON", org.mockito.Mockito.mock(HttpInputMessage.class)));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void multipartSizeLimitReturnsAttachmentSizeResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/feedbacks");

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleMaxUploadSize(request, new MaxUploadSizeExceededException(10_485_760L));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED.getCode());
        assertThat(response.getBody().getDetails()).isNull();
    }
}
