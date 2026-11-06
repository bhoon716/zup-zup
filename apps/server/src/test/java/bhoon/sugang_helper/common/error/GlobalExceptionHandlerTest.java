package bhoon.sugang_helper.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.response.ErrorResponse;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/v1/courses";
    private static final String STACK_TRACE_INCLUDED = "stackTraceIncluded=true";

    @Test
    @DisplayName("서버 오류 로그는 예외 스택과 추적 ID를 포함한다")
    void serverErrorLogIncludesThrowableStackTraceAndCorrelationId() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
        Exception exception = new IllegalStateException("internal failure");
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler().handleAny(request, exception);

            assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getStatus());
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage())
                        .contains("[API_ERROR] correlationId=" + response.getBody().getCorrelationId());
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getThrowableProxy().getMessage()).contains(exception.getClass().getName());
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("출력 스택은 민감한 메시지 없이 원래 프레임과 원인 타입을 유지한다")
    void renderedStackKeepsOriginalFramesAndCauseTypesWithoutSensitiveMessages() {
        // given
        Exception cause = new IllegalArgumentException("cookie=session-secret query=private-user@example.com");
        Exception exception = new IllegalStateException("authorization=Bearer top-secret", cause);
        exception.addSuppressed(new IllegalStateException("password=suppressed-secret"));
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            new GlobalExceptionHandler().handleAny(new MockHttpServletRequest("GET", REQUEST_PATH), exception);
            ILoggingEvent event = appender.list.getFirst();
            String rendered = event.getFormattedMessage() + ThrowableProxyUtil.asString(event.getThrowableProxy());
            assertThat(rendered)
                    .contains(IllegalStateException.class.getName(), IllegalArgumentException.class.getName(),
                            "Suppressed:", "Caused by:", "renderedStackKeepsOriginalFramesAndCauseTypes")
                    .doesNotContain("session-secret", "private-user@example.com", "top-secret", "suppressed-secret");
            assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()[0].getStackTraceElement())
                    .isEqualTo(exception.getStackTrace()[0]);
            assertThat(exception.getMessage()).contains("top-secret");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("반복 서버 오류는 스택을 중복 출력하지 않고 요약을 유지한다")
    void repeatedServerErrorsKeepErrorSummaryWithoutRepeatingStackTrace() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();

            handler.handleAny(request, new IllegalStateException("first failure",
                    new IllegalArgumentException("email=private@example.com")));
            handler.handleAny(request, new IllegalStateException("repeated failure",
                    new IllegalArgumentException("token=private-token")));

            assertThat(appender.list).hasSize(2);
            assertThat(appender.list.get(0).getThrowableProxy()).isNotNull();
            assertThat(appender.list.get(1).getThrowableProxy()).isNull();
            assertThat(appender.list.get(0).getFormattedMessage()).contains(STACK_TRACE_INCLUDED);
            assertThat(appender.list.get(1).getFormattedMessage()).contains("stackTraceIncluded=false");
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage())
                        .contains("rootCauseType=IllegalArgumentException")
                        .doesNotContain("private@example.com", "private-token");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("서로 다른 최종 원인은 각자의 스택을 남긴다")
    void differentRootCausesKeepTheirOwnStackTraces() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();

            handler.handleAny(request, new IllegalStateException(
                    "database failure", new IllegalArgumentException("database")));
            handler.handleAny(request, new IllegalStateException(
                    "redis failure", new UnsupportedOperationException("redis")));

            assertThat(appender.list).hasSize(2);
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getFormattedMessage()).contains(STACK_TRACE_INCLUDED);
            });
            assertThat(appender.list.get(0).getThrowableProxy().getCause().getMessage())
                    .contains(IllegalArgumentException.class.getName());
            assertThat(appender.list.get(1).getThrowableProxy().getCause().getMessage())
                    .contains(UnsupportedOperationException.class.getName());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("깊은 원인 체인의 차이도 별도 스택으로 남긴다")
    void deeplyDifferentRootCausesKeepTheirOwnStackTraces() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();

            handler.handleAny(request, nestedException(new IllegalArgumentException("database")));
            handler.handleAny(request, nestedException(new UnsupportedOperationException("redis")));

            assertThat(appender.list).hasSize(2);
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getFormattedMessage()).contains(STACK_TRACE_INCLUDED);
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("원인 체인이 순환해도 스택을 안전하게 기록한다")
    void cyclicCauseChainStillLogsStackTrace() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            IllegalStateException first = new IllegalStateException("first");
            IllegalStateException second = new IllegalStateException("second");
            first.initCause(second);
            second.initCause(first);

            new GlobalExceptionHandler().handleAny(request, first);

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getFormattedMessage()).contains(STACK_TRACE_INCLUDED);
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private Exception nestedException(Exception rootCause) {
        Exception exception = rootCause;
        for (int depth = 0; depth < 9; depth++) {
            exception = new IllegalStateException("wrapper", exception);
        }
        return exception;
    }

    @Test
    @DisplayName("업무 예외는 비밀 상세 정보와 토큰 경로를 노출하지 않는다")
    void customExceptionDoesNotExposeSecretDetailOrTokenPath() {
        // given
        String token = "device-token-should-not-appear";
        String email = "recipient@example.com";
        String detail = "recipient=" + email + ", token=" + token;
        MockHttpServletRequest request = new MockHttpServletRequest(
                "DELETE", "/api/v1/users/devices/token/" + token);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
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
    @DisplayName("낙관적 잠금 충돌은 재시도 가능한 충돌 응답을 반환한다")
    void optimisticLockConflictReturnsRetryableConflictResponse() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/users/me");

        // when
        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleOptimisticLock(request, new ObjectOptimisticLockingFailureException("User", 1L));

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.getCode());
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("읽을 수 없는 JSON은 잘못된 입력 응답을 반환한다")
    void unreadableJsonReturnsInvalidInputResponse() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/feedbacks");

        // when
        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleUnreadableMessage(request, new HttpMessageNotReadableException(
                        "invalid JSON", Mockito.mock(HttpInputMessage.class)));

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("멀티파트 크기 제한은 첨부 크기 오류 응답을 반환한다")
    void multipartSizeLimitReturnsAttachmentSizeResponse() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/feedbacks");

        // when
        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleMaxUploadSize(request, new MaxUploadSizeExceededException(10_485_760L));

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED.getCode());
        assertThat(response.getBody().getDetails()).isNull();
    }
}
