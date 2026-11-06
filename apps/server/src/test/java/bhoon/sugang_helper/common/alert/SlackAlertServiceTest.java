package bhoon.sugang_helper.common.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.alerts.SlackAlertProperties;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    private static final String SERVER_ERROR_CODE = "G001";
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
    @DisplayName("오류 알림은 민감정보를 가리고 추적 ID와 주요 스택을 포함한다")
    void sendsRedactedAlertWithCorrelationAndTopStackFrames() {
        // given
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);
        MDC.put("crawlerRunId", "crawl-456");
        CustomException exception = new CustomException(
                ErrorCode.CRAWLER_CONNECTION_ERROR, "secret=must-not-leave-the-process");

        exception.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("Crawler", "fetch", "Crawler.java", 10),
                new StackTraceElement("Crawler", "retry", "Crawler.java", 20),
                new StackTraceElement("Crawler", "run", "Crawler.java", 30),
                new StackTraceElement("HiddenFrame", "hidden", "HiddenFrame.java", 40)
        });

        // when
        service.alert(SlackAlertCategory.CRAWLER_FETCH, "C001", exception);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertThat(message)
                .contains("환경: TEST", "영향:", "긴급도: 확인 필요", "재시도:", "Grafana")
                .contains("category=CRAWLER_FETCH")
                .contains("errorCode=C001")
                .contains("exceptionType=CustomException")
                .contains("correlationId=" + CORRELATION_ID)
                .contains("stackTraceTop3=", "crawlerRunId=crawl-456", "Crawler.fetch(Crawler.java:10)")
                .doesNotContain("HiddenFrame")
                .doesNotContain("must-not-leave-the-process", "secret=");
    }

    @Test
    @DisplayName("안전한 수집 진단 정보를 오류 알림에 포함한다")
    void sendsSafeCrawlerDiagnosticWithAlert() {
        // given
        String diagnostic = "failureStage=COURSE_API failureType=SocketTimeoutException "
                + "upstreamStatus=UNKNOWN retryable=true";

        // when
        service.alert(
                SlackAlertCategory.CRAWLER_FETCH,
                "C001",
                new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR),
                diagnostic);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .contains(diagnostic, "재시도 가능한 유형", "예약 여부는 로그 확인 필요")
                .doesNotContain("http", "cookie", "secret");
    }

    @Test
    @DisplayName("재알림 제한 시간에는 같은 오류 알림을 생략한다")
    void suppressesSameAlertDuringCooldown() {
        // given
        RuntimeException exception = new IllegalStateException("database unavailable");

        // when
        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, exception);
        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, exception);

        // then
        verify(webhookClient).send(anyString());
    }

    @Test
    @DisplayName("웹훅이 없으면 Slack을 호출하지 않는다")
    void doesNotCallSlackWhenWebhookIsNotConfigured() {
        // given
        SlackAlertService disabledService = new SlackAlertService(
                new SlackAlertProperties("", "TEST", 60, 2_000), webhookClient);

        // when
        disabledService.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, new RuntimeException());

        // then
        verify(webhookClient, never()).send(anyString());
    }

    @Test
    @DisplayName("공개 알림 호출은 호출자 스레드와 추적 문맥에서 실행한다")
    void publicAlertRunsOnTheCallerThreadWithItsCorrelationContext() {
        // given
        Thread callerThread = Thread.currentThread();
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);

        // when & then
        doAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(callerThread);
            assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo(CORRELATION_ID);
            return null;
        }).when(webhookClient).send(anyString());

        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, new RuntimeException());

        verify(webhookClient).send(anyString());
        assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo(CORRELATION_ID);
    }

    @Test
    @DisplayName("웹훅 실패는 호출자에게 전파되거나 재알림 제한을 우회하지 않는다")
    void webhookFailureDoesNotEscapeOrBypassCooldown() {
        // given
        RuntimeException originalFailure = new IllegalStateException("database unavailable");
        doThrow(new IllegalStateException("webhook unavailable")).when(webhookClient).send(anyString());

        // when & then
        assertThatCode(() -> service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, originalFailure))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, originalFailure))
                .doesNotThrowAnyException();
        verify(webhookClient).send(anyString());
    }

    @Test
    @DisplayName("허용된 진단만 포함하고 미확인 영향이나 재시도 성공을 단정하지 않는다")
    void includesOnlyAllowedDiagnosticsAndDoesNotClaimUnverifiedImpactOrRetrySuccess() {
        // when
        service.alert(SlackAlertCategory.DB_PERSIST, SERVER_ERROR_CODE, new IllegalStateException("private exception"),
                "failureStage=PERSIST retryable=false token=payload-secret cookie=private-cookie "
                        + "failureType=<@everyone> upstreamStatus=503");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        assertThat(captor.getValue())
                .contains("수집 결과 DB 저장 실패", "반영 여부 확인 필요", "자동 재시도 대상이 아닌 유형",
                        "retryable=false", "upstreamStatus=503", "correlationId=NONE")
                .doesNotContain("payload-secret", "private-cookie", "private exception", "@everyone");
    }

    @Test
    @DisplayName("여러 줄의 진단 표시는 제외하고 확인되지 않은 서버 오류 범위를 설명한다")
    void rejectsMultilineLabelsAndExplainsUnknownServerFailureScope() {
        // given
        SlackAlertService untrustedLabels = new SlackAlertService(
                new SlackAlertProperties(WEBHOOK_URL, "TEST\n@everyone", 60, 2_000), webhookClient);
        MDC.put(CORRELATION_ID_KEY, "request\nforged-secret");

        // when
        untrustedLabels.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, new RuntimeException(), "unknown=secret");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        assertThat(captor.getValue())
                .contains("환경: INVALID", "correlationId=INVALID", "해당 요청이 실패함", "사용자 영향은 확인 필요",
                        "이 알림 자체는 작업을 재실행하지 않음")
                .doesNotContain("@everyone", "forged-secret", "unknown=secret");
    }
}
