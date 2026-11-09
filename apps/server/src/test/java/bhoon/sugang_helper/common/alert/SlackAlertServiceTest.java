package bhoon.sugang_helper.common.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.alerts.SlackAlertProperties;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class SlackAlertServiceTest {

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/test";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CORRELATION_ID = "request-123";
    private static final String SERVER_ERROR_CODE = "G001";
    private static final String UNEXPECTED_CODE = "UNEXPECTED";
    private static final String ENVIRONMENT = "TEST";
    private static final String CRAWLER_FILE = "Crawler.java";
    private static final String CRAWLER_CONNECTION_CODE = "C001";

    @Mock
    private SlackWebhookClient webhookClient;

    private SlackAlertService service;
    private SimpleMeterRegistry registry;
    private AtomicLong now;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        now = new AtomicLong();
        executor = mock(ExecutorService.class);
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        service = new SlackAlertService(
                new SlackAlertProperties(WEBHOOK_URL, ENVIRONMENT, 60, 2_000), webhookClient, registry, now::get, executor);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("오류 알림은 상황과 확인 순서를 포함한 한국어 안내와 추적 ID를 남긴다")
    void sendsConciseKoreanAlertWithTraceIdsWithoutStackFrames() {
        // given
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);
        MDC.put("crawlerRunId", "crawl-456");
        CustomException exception = new CustomException(
                ErrorCode.CRAWLER_CONNECTION_ERROR, "secret=must-not-leave-the-process");

        exception.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("Crawler", "fetch", CRAWLER_FILE, 10),
                new StackTraceElement("Crawler", "retry", CRAWLER_FILE, 20),
                new StackTraceElement("Crawler", "run", CRAWLER_FILE, 30),
                new StackTraceElement("HiddenFrame", "hidden", "HiddenFrame.java", 40)
        });

        // when
        service.alert(SlackAlertCategory.CRAWLER_FETCH, CRAWLER_CONNECTION_CODE, exception);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertThat(message)
                .contains("[줍줍 오류][TEST]", "영향:", "재시도:", "Grafana", "(한국 시간)")
                .contains("오류: C001 · CustomException")
                .contains("추적: 요청=" + CORRELATION_ID + " · 수집=crawl-456")
                .doesNotContain("category=", "stackTraceTop3=", "긴급도:", CRAWLER_FILE, "HiddenFrame")
                .doesNotContain("must-not-leave-the-process", "secret=");
        assertThat(message).contains("상태: 이번 강의 수집 실패 · 복구 여부 미확인", "다음 수집 결과",
                "Grafana 검색: {app=\"jbnu-sugang-helper\",job=\"app\"} | json | __error__=\"\" | crawlerRunId=\"crawl-456\"")
                .doesNotContain("P1", "P2", "전체 장애", "전체 사용자 영향");
        assertThat(message.lines().count()).isEqualTo(10);
        assertThat(message.lines().toList().get(3))
                .matches("감지: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\(한국 시간\\)");
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
                CRAWLER_CONNECTION_CODE,
                new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR),
                diagnostic);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .contains("실패 단계=학교 강의 API 호출(COURSE_API)",
                        "원인 유형=응답 대기 시간 초과(SocketTimeoutException)", "학교 응답=미확인",
                        "재시도 가능한 유형", "실제 예약·실행·성공 여부는 로그 확인")
                .doesNotContain("failureStage=", "failureType=", "upstreamStatus=", "retryable=")
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
                new SlackAlertProperties("", ENVIRONMENT, 60, 2_000), webhookClient, registry, now::get, executor);

        // when
        disabledService.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, new RuntimeException());

        // then
        verify(webhookClient, never()).send(anyString());
    }

    @Test
    @DisplayName("즉시 실행기를 주입해도 호출자의 추적 문맥을 유지한다")
    void directExecutorPreservesCallerCorrelationContext() {
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
                .contains("수집 결과 DB 저장 실패", "반영 지연 가능", "자동 재시도 대상 아님",
                        "학교 응답=503", "요청=NONE")
                .doesNotContain("payload-secret", "private-cookie", "private exception", "@everyone");
    }

    @Test
    @DisplayName("여러 줄의 진단 표시는 제외하고 확인되지 않은 서버 오류 범위를 설명한다")
    void rejectsMultilineLabelsAndExplainsUnknownServerFailureScope() {
        // given
        SlackAlertService untrustedLabels = new SlackAlertService(
                new SlackAlertProperties(WEBHOOK_URL, "TEST\n@everyone", 60, 2_000), webhookClient, registry, now::get, executor);
        MDC.put(CORRELATION_ID_KEY, "request\nforged-secret");

        // when
        untrustedLabels.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, new RuntimeException(), "unknown=secret");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        assertThat(captor.getValue())
                .contains("[INVALID]", "요청=INVALID", "해당 요청이 실패함", "사용자 영향은 미확인",
                        "재시도: 실행 여부 미확인", "추적 ID 없음")
                .doesNotContain("@everyone", "forged-secret", "unknown=secret");
    }
    @Test
    @DisplayName("현재 상태와 첫 대응을 기술 진단보다 먼저 보여준다")
    void showsStatusAndFirstResponseBeforeDiagnosticDetails() {
        // when
        service.alert(SlackAlertCategory.CRAWLER_FETCH, CRAWLER_CONNECTION_CODE, new IllegalStateException("private-token"));

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        String message = captor.getValue();
        assertThat(message.lines().limit(2).toList())
                .hasSize(2).last().asString().startsWith("상태:");
        assertThat(message.indexOf("첫 대응:")).isLessThan(message.indexOf("오류:"));
        assertThat(message).contains("강의 API 수집 실패", "오류: C001")
                .doesNotContain("private-token", "복구 완료", "재시도 성공");
    }

    @Test
    @DisplayName("재시도 분류만 있으면 빈 진단 줄 대신 추가 정보 없음을 표시한다")
    void keepsDiagnosticReadableWhenOnlyRetryMetadataExists() {
        // when
        service.alert(SlackAlertCategory.CRAWLER_PARSE, "C003", null, "retryable=false");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        assertThat(captor.getValue()).startsWith("[줍줍 오류][TEST] 강의 데이터 해석 실패")
                .contains("진단 단서: 추가 메타데이터 없음", "상태: 이번 학교 응답 해석 실패");
    }

    @Test
    @DisplayName("학교 API 접속 준비 실패의 원인 단서를 한국어로 설명한다")
    void explainsBootstrapConnectionFailureWithoutGuessingRootCause() {
        // when
        service.alert(SlackAlertCategory.CRAWLER_FETCH, CRAWLER_CONNECTION_CODE, null,
                "failureStage=JUMP_BOOTSTRAP failureType=ConnectException upstreamStatus=UNKNOWN retryable=true");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        assertThat(captor.getValue())
                .contains("학교 API 접속 준비(JUMP_BOOTSTRAP)", "연결 실패(ConnectException)",
                        "학교 응답=미확인", "실제 예약·실행·성공 여부는 로그 확인")
                .doesNotContain("학교 서버가 중단", "복구 완료");
    }

    @Test
    @DisplayName("수집 ID가 없으면 요청 ID로 바로 검색할 수 있는 로그 쿼리를 제공한다")
    void providesRequestLogQueryWhenCrawlerRunIdIsMissing() {
        // given
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);

        // when
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, new IllegalStateException());

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).send(captor.capture());
        assertThat(captor.getValue())
                .contains("| correlationId=\"" + CORRELATION_ID + "\"", "① 요청 ID로 실패 API 확인")
                .doesNotContain("| crawlerRunId=", "| correlationId=\"NONE\"");
    }

    @Test
    @DisplayName("서로 다른 실패 위치는 같은 오류 코드와 타입이어도 각각 알린다")
    void separatesFailureSitesWithoutUsingPrivateMessages() {
        // given
        RuntimeException first = failureAt(10);
        RuntimeException second = failureAt(20);

        // when
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, first);
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, second);
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, failureAt(10));

        // then
        verify(webhookClient, times(2)).send(anyString());
        assertThat(count(SlackAlertCategory.SERVER_5XX, "success")).isEqualTo(2);
        assertThat(count(SlackAlertCategory.SERVER_5XX, "suppressed")).isEqualTo(1);
    }

    @Test
    @DisplayName("실패와 생략을 계수하고 제한 시간이 지난 다음 발생은 다시 전송한다")
    void countsFailureAndAllowsNextOccurrenceAfterCooldown() {
        // given
        RuntimeException failure = failureAt(10);
        doThrow(new IllegalStateException("private webhook URL")).when(webhookClient).send(anyString());

        // when
        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, failure);
        now.set(TimeUnit.SECONDS.toNanos(60) - 1);
        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, failure);
        now.incrementAndGet();
        service.alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, failure);

        // then
        verify(webhookClient, times(2)).send(anyString());
        assertThat(count(SlackAlertCategory.DB_PERSIST, "failure")).isEqualTo(2);
        assertThat(count(SlackAlertCategory.DB_PERSIST, "suppressed")).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 키 한도에서는 활성 제한을 유지하고 만료 뒤 새 위치를 허용한다")
    void rejectsCapacityOverflowWithoutEvictingActiveCooldowns() {
        // given
        for (int site = 0; site < 256; site++) {
            service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, failureAt(site));
        }

        // when
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, failureAt(256));
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, failureAt(0));

        // then
        verify(webhookClient, times(256)).send(anyString());
        assertThat(count(SlackAlertCategory.SERVER_5XX, "rejected")).isEqualTo(1);
        assertThat(count(SlackAlertCategory.SERVER_5XX, "suppressed")).isEqualTo(1);

        // when
        now.set(TimeUnit.SECONDS.toNanos(60));
        service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, failureAt(256));

        // then
        verify(webhookClient, times(257)).send(anyString());
    }

    @Test
    @DisplayName("메트릭은 고정된 분류와 결과만 사용하고 민감한 진단을 라벨로 쓰지 않는다")
    void boundsMetricLabelsAndCountsDisabledDelivery() {
        // given
        SlackAlertService disabled = new SlackAlertService(
                new SlackAlertProperties("", ENVIRONMENT, 60, 2_000), webhookClient, registry, now::get, executor);

        // when
        disabled.alert(SlackAlertCategory.SERVER_5XX, "secret-code", new RuntimeException("private"));

        // then
        assertThat(count(SlackAlertCategory.SERVER_5XX, "disabled")).isEqualTo(1);
        assertThat(registry.getMeters()).hasSize(SlackAlertCategory.values().length * 5);
        registry.getMeters().forEach(meter -> {
            assertThat(meter.getId().getTags()).hasSize(2);
            assertThat(meter.getId().getTags().toString()).doesNotContain("secret-code", "private", ENVIRONMENT);
        });
    }

    @Test
    @DisplayName("전송 실패 로그는 이벤트와 분류를 독립 필드로 기록하고 비밀을 남기지 않는다")
    void recordsStructuredFailureWithoutWebhookDetails() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(SlackAlertService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        doThrow(new IllegalStateException("private-webhook-secret")).when(webhookClient).send(anyString());
        AtomicReference<Runnable> pending = new AtomicReference<>();
        doAnswer(invocation -> {
            pending.set(invocation.getArgument(0, Runnable.class));
            return null;
        }).when(executor).execute(any(Runnable.class));
        MDC.put(CORRELATION_ID_KEY, CORRELATION_ID);
        try {
            // when
            service.alert(SlackAlertCategory.SERVER_5XX, SERVER_ERROR_CODE, failureAt(10));
            MDC.clear();
            pending.get().run();

            // then
            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.getFirst();
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("event");
                assertThat(pair.value).isEqualTo("slack_alert_delivery_failed");
            }).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("category");
                assertThat(pair.value).isEqualTo(SlackAlertCategory.SERVER_5XX);
            }).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo(CORRELATION_ID_KEY);
                assertThat(pair.value).isEqualTo(CORRELATION_ID);
            });
            assertThat(event.getFormattedMessage()).doesNotContain("private-webhook-secret", WEBHOOK_URL);
            assertThat(event.getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private double count(SlackAlertCategory category, String outcome) {
        return registry.get("slack.alert.delivery").tags("category", category.name(), "outcome", outcome)
                .counter().count();
    }

    private RuntimeException failureAt(int line) {
        RuntimeException failure = new IllegalStateException("private exception message");
        failure.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("bhoon.sugang_helper.Example", "run", "Example.java", line)
        });
        return failure;
    }

}
