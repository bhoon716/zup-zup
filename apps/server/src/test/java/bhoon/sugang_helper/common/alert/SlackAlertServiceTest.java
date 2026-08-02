package bhoon.sugang_helper.common.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class SlackAlertServiceTest {

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/test";
    private static final String CORRELATION_ID = "request-123";

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
        MDC.put("correlationId", CORRELATION_ID);
        CustomException exception = new CustomException(
                ErrorCode.CRAWLER_CONNECTION_ERROR, "secret=must-not-leave-the-process");

        service.sendSynchronously(SlackAlertCategory.CRAWLER_FETCH, "C001", exception);

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
    void suppressesSameAlertDuringCooldown() {
        RuntimeException exception = new IllegalStateException("database unavailable");

        service.sendSynchronously(SlackAlertCategory.DB_PERSIST, "UNEXPECTED", exception);
        service.sendSynchronously(SlackAlertCategory.DB_PERSIST, "UNEXPECTED", exception);

        verify(webhookClient).send(anyString());
    }

    @Test
    void doesNotCallSlackWhenWebhookIsNotConfigured() {
        SlackAlertService disabledService = new SlackAlertService(
                new SlackAlertProperties("", "TEST", 60, 2_000), webhookClient);

        disabledService.sendSynchronously(SlackAlertCategory.SERVER_5XX, "G001", new RuntimeException());

        verify(webhookClient, never()).send(anyString());
    }

    @Test
    void publicAlertMethodIsAsynchronous() throws NoSuchMethodException {
        Method method = SlackAlertService.class.getMethod(
                "alert", SlackAlertCategory.class, String.class, Throwable.class);

        assertThat(method.isAnnotationPresent(Async.class)).isTrue();
    }
}
