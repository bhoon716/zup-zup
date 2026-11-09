package bhoon.sugang_helper.academic.crawling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.academic.crawling.dto.diagnostic.CrawlerFailureSummary;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerRunFailure;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerStatus;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerRunStatus;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerRunFailureRepository;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerStatusRepository;
import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class CrawlerRunStateServiceTest {

    private static final String ILLEGAL_STATE_TYPE = "IllegalStateException";
    private static final String ORIGINAL_MESSAGE = "original";
    private static final String DATABASE_FAILURE = "database unavailable";
    private static final String UNEXPECTED_CODE = "UNEXPECTED";

    @TempDir
    Path tempDir;

    @Mock
    private CrawlerStatusRepository crawlerStatusRepository;

    @Mock
    private CrawlerRunFailureRepository crawlerRunFailureRepository;

    @Mock
    private SlackAlertService slackAlertService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final TransactionStatus transactionStatus = new SimpleTransactionStatus();
    private CrawlerRunStateService service;

    @BeforeEach
    void setUp() {
        CrawlerFailurePersistenceService persistence = new CrawlerFailurePersistenceService(
                crawlerStatusRepository, crawlerRunFailureRepository);
        ProxyFactory factory = new ProxyFactory(persistence);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        factory.addAdvice(interceptor);
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        service = new CrawlerRunStateService(crawlerStatusRepository,
                (CrawlerFailurePersistenceService) factory.getProxy(), slackAlertService);
    }

    @Test
    @DisplayName("성공 시 singleton crawler_status의 마지막 성공 시각을 갱신한다")
    void markSuccess_UpdatesSingletonStatus() {
        // given
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());

        // when
        service.markSuccess();

        // then
        ArgumentCaptor<CrawlerStatus> captor = ArgumentCaptor.forClass(CrawlerStatus.class);
        verify(crawlerStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(CrawlerStatus.SINGLETON_ID);
        assertThat(captor.getValue().getLatestStatus()).isEqualTo(CrawlerRunStatus.SUCCESS);
        assertThat(captor.getValue().getLastSuccessAt()).isNotNull();
    }

    @Test
    @DisplayName("실패 시 상태와 실행별 영구 실패 행에 비민감 요약을 함께 기록한다")
    void recordFailure_SavesStatusAndFailureRow() {
        // given
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());
        CustomException exception = new CustomException(
                ErrorCode.CRAWLER_CONNECTION_ERROR, "외부 응답 원문 secret=do-not-store");

        // when
        service.recordFailure(CrawlerFailureStage.FETCH_PARSE, exception);

        // then
        ArgumentCaptor<CrawlerStatus> statusCaptor = ArgumentCaptor.forClass(CrawlerStatus.class);
        ArgumentCaptor<CrawlerRunFailure> failureCaptor = ArgumentCaptor.forClass(CrawlerRunFailure.class);
        verify(crawlerStatusRepository).save(statusCaptor.capture());
        verify(crawlerRunFailureRepository).save(failureCaptor.capture());
        assertThat(statusCaptor.getValue().getLatestStatus()).isEqualTo(CrawlerRunStatus.FAILURE);
        assertThat(failureCaptor.getValue().getFailureMessage())
                .contains(ErrorCode.CRAWLER_CONNECTION_ERROR.getCode())
                .doesNotContain("secret", "do-not-store");
        verify(slackAlertService).alert(SlackAlertCategory.CRAWLER_FETCH, "C001", exception);
    }

    @Test
    @DisplayName("실패 상태 조회가 실패해도 원래 수집 오류의 Slack 알림을 시도한다")
    void recordFailure_AlertsWhenStatusReadFails() {
        // given
        RuntimeException original = new IllegalStateException(ORIGINAL_MESSAGE);
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID))
                .willThrow(new IllegalStateException(DATABASE_FAILURE));

        // when & then
        assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                .doesNotThrowAnyException();
        verify(slackAlertService).alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
    }

    @Test
    @DisplayName("영구 실패 행 저장이 실패해도 원래 수집 오류의 Slack 알림을 시도한다")
    void recordFailure_AlertsWhenFailureSaveFails() {
        // given
        RuntimeException original = new IllegalStateException(ORIGINAL_MESSAGE);
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());
        given(crawlerRunFailureRepository.save(any(CrawlerRunFailure.class)))
                .willThrow(new IllegalStateException(DATABASE_FAILURE));

        // when & then
        assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                .doesNotThrowAnyException();
        verify(slackAlertService).alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
    }

    @Test
    @DisplayName("실패 기록 트랜잭션을 시작하지 못해도 Slack 알림을 시도한다")
    void recordFailure_AlertsWhenTransactionBeginFails() {
        // given
        RuntimeException original = new IllegalStateException(ORIGINAL_MESSAGE);
        given(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .willThrow(new CannotCreateTransactionException(DATABASE_FAILURE));

        // when & then
        assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                .doesNotThrowAnyException();
        verifyNoInteractions(crawlerStatusRepository, crawlerRunFailureRepository);
        verify(slackAlertService).alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
    }

    @Test
    @DisplayName("실패 기록 트랜잭션 커밋이 실패해도 Slack 알림을 시도한다")
    void recordFailure_AlertsWhenCommitFails() {
        // given
        RuntimeException original = new IllegalStateException(ORIGINAL_MESSAGE);
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());
        doThrow(new TransactionSystemException(DATABASE_FAILURE))
                .when(transactionManager).commit(transactionStatus);

        // when & then
        assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                .doesNotThrowAnyException();
        verify(transactionManager).commit(transactionStatus);
        verify(slackAlertService).alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
    }

    @Test
    @DisplayName("Slack 알림 실패는 이미 커밋한 영구 실패 기록이나 원래 오류를 변경하지 않는다")
    void recordFailure_PersistsBeforeIsolatedAlertFailure() {
        // given
        RuntimeException original = new IllegalStateException(ORIGINAL_MESSAGE);
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());
        doThrow(new IllegalStateException("webhook unavailable")).when(slackAlertService)
                .alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);

        // when & then
        assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                .doesNotThrowAnyException();
        var order = inOrder(crawlerRunFailureRepository, transactionManager, slackAlertService);
        order.verify(crawlerRunFailureRepository).save(any(CrawlerRunFailure.class));
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(slackAlertService).alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
        assertThat(original.getSuppressed()).isEmpty();
    }

    @Test
    @DisplayName("실패 상태 저장이 실패해도 Slack 알림을 시도하고 기록 트랜잭션을 롤백한다")
    void recordFailure_AlertsWhenStatusSaveFails() {
        // given
        RuntimeException original = new IllegalStateException(ORIGINAL_MESSAGE);
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID)).willReturn(Optional.empty());
        given(crawlerStatusRepository.save(any(CrawlerStatus.class)))
                .willThrow(new IllegalStateException(DATABASE_FAILURE));

        // when & then
        assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                .doesNotThrowAnyException();
        verify(transactionManager).rollback(transactionStatus);
        verifyNoInteractions(crawlerRunFailureRepository);
        verify(slackAlertService).alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
    }

    @Test
    @DisplayName("DB와 알림이 모두 실패해도 예외 원문 없이 각각 기록하고 호출자에게 전파하지 않는다")
    void recordFailure_IsolatesBothFailuresWithoutLoggingSecrets() throws Exception {
        // given
        RuntimeException original = new IllegalStateException("original-password=hidden");
        given(crawlerStatusRepository.findById(CrawlerStatus.SINGLETON_ID))
                .willThrow(new IllegalStateException("database-password=hidden"));
        doThrow(new IllegalStateException("webhook-token=hidden")).when(slackAlertService)
                .alert(SlackAlertCategory.DB_PERSIST, UNEXPECTED_CODE, original);
        Logger logger = (Logger) LoggerFactory.getLogger(CrawlerRunStateService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            // when
            assertThatCode(() -> service.recordFailure(CrawlerFailureStage.PERSIST, original))
                    .doesNotThrowAnyException();

            // then
            assertThat(appender.list).hasSize(2).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("stage=PERSIST", ILLEGAL_STATE_TYPE)
                        .doesNotContain("hidden", "password", "token");
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getKeyValuePairs()).extracting(pair -> pair.key, pair -> pair.value)
                        .contains(tuple("stage", "PERSIST"), tuple("originalType", ILLEGAL_STATE_TYPE));
            });
            List<JsonNode> events = renderJsonEvents(appender.list);
            assertThat(events.getFirst().path("event").asText()).isEqualTo("crawler_failure_record_failed");
            assertThat(events.getFirst().path("recordingType").asText()).isEqualTo(ILLEGAL_STATE_TYPE);
            assertThat(events.getLast().path("event").asText()).isEqualTo("crawler_failure_alert_failed");
            assertThat(events.getLast().path("alertType").asText()).isEqualTo(ILLEGAL_STATE_TYPE);
            assertThat(events).allSatisfy(event -> {
                assertThat(event.path("stage").asText()).isEqualTo("PERSIST");
                assertThat(event.path("originalType").asText()).isEqualTo(ILLEGAL_STATE_TYPE);
                assertThat(event.toString()).doesNotContain("hidden", "password", "token");
            });
            assertThat(original.getSuppressed()).isEmpty();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private List<JsonNode> renderJsonEvents(List<ILoggingEvent> events) throws Exception {
        Path file = tempDir.resolve("crawler-failure.jsonl");
        LoggerContext context = new LoggerContext();
        context.putProperty("LOG_FILE", file.toString());
        context.putObject(Environment.class.getName(), new MockEnvironment());
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(Objects.requireNonNull(getClass().getClassLoader()
                    .getResource("logback-spring.xml")));
            var appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE");
            events.forEach(appender::doAppend);
        } finally {
            context.stop();
        }
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> result = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            result.add(mapper.readTree(line));
        }
        return result;
    }

    @Test
    @DisplayName("예상하지 못한 예외 메시지 원문도 실패 DB 기록에 저장하지 않는다")
    void unexpectedFailure_DoesNotPersistRawMessage() {
        // given
        CrawlerFailureSummary summary = CrawlerFailureSummary.from(
                CrawlerFailureStage.PERSIST, new IllegalStateException("password=secret"));

        // when & then
        assertThat(summary.failureType()).isEqualTo(ILLEGAL_STATE_TYPE);
        assertThat(summary.failureMessage()).isEqualTo("Unexpected crawler failure");
    }

    @Test
    @DisplayName("실패 기록은 rollback된 크롤러 트랜잭션과 분리된 새 트랜잭션에서 저장한다")
    void recordFailure_RequiresNewTransaction() throws NoSuchMethodException {
        // given
        Transactional transactional = CrawlerFailurePersistenceService.class
                .getMethod("recordFailure", LocalDateTime.class, CrawlerFailureSummary.class)
                .getAnnotation(Transactional.class);

        // when & then
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(CrawlerRunStateService.class
                .getMethod("recordFailure", CrawlerFailureStage.class, RuntimeException.class)
                .getAnnotation(Transactional.class)).isNull();
    }
}
