package bhoon.sugang_helper.common.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class SlackAlertServiceTest {

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/test";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CORRELATION_ID = "request-123";
    private static final String UNEXPECTED_CODE = "UNEXPECTED";

    @Mock
    private SlackWebhookClient webhookClient;

    private SlackAlertService service;

    @BeforeEach
    void setUp() {
        service = new SlackAlertService(
                new SlackAlertProperties(WEBHOOK_URL, "TEST", 60, 2_000), webhookClient);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void sendsRedactedAlertWithCorrelationAndTopStackFrames() {
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);
        CustomException exception = new CustomException(
                ErrorCode.CRAWLER_CONNECTION_ERROR, "secret=must-not-leave-the-process");

        service.alert(SlackAlertCategory.CRAWLER_FETCH, "C001", exception);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertThat(message)
                .contains("environment=TEST")
                .contains("category=CRAWLER_FETCH")
                .contains("errorCode=C001")
                .contains("exceptionType=CustomException")
                .contains("correlationId=" + CORRELATION_ID)
                .contains("stackTraceTop3=")
                .doesNotContain("must-not-leave-the-process", "secret=");
    }

    @Test
    void sendsSafeCrawlerDiagnosticWithAlert() {
        String diagnostic = "failureStage=COURSE_API failureType=SocketTimeoutException "
                + "upstreamStatus=UNKNOWN retryable=true";

        service.alert(
                SlackAlertCategory.CRAWLER_FETCH,
                "C001",
                new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR),
                diagnostic);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .contains(diagnostic)
                .doesNotContain("http", "cookie", "secret");
    }

    @Test
    void suppressesSameAlertDuringCooldown() {
        RuntimeException exception = new IllegalStateException("database unavailable");

        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, exception);
        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, exception);

        verify(webhookClient).send(anyString());
    }

    @Test
    void doesNotCallSlackWhenWebhookIsNotConfigured() {
        SlackAlertService disabledService = new SlackAlertService(
                new SlackAlertProperties("", "TEST", 60, 2_000), webhookClient);

        disabledService.alert(SlackAlertCategory.SERVER_5XX, "G001", new RuntimeException());

        verify(webhookClient, never()).send(anyString());
    }

    @Test
    void publicAlertRunsOnTheCallerThreadWithItsCorrelationContext() {

        Thread callerThread = Thread.currentThread();
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);
        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(callerThread);
            assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo(CORRELATION_ID);
            return null;
        }).when(webhookClient).send(anyString());


        service.alert(SlackAlertCategory.SERVER_5XX, "G001", new RuntimeException());


        verify(webhookClient).send(anyString());
        assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo(CORRELATION_ID);
    }

    @Test
    void webhookFailureDoesNotEscapeOrBypassCooldown() {

        RuntimeException originalFailure = new IllegalStateException("database unavailable");
        doThrow(new IllegalStateException("webhook unavailable")).when(webhookClient).send(anyString());


        assertThatCode(() -> service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, originalFailure))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, originalFailure))
                .doesNotThrowAnyException();
        verify(webhookClient).send(anyString());
    }
}
