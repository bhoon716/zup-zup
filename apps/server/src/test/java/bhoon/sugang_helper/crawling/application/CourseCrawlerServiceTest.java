package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.SemesterType;
import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceTest {

    private static final String YEAR = "2026";
    private static final String SEMESTER = "U211600010";

    @Mock
    private CourseCrawlFetcher courseCrawlFetcher;
    @Mock
    private CourseSynchronizationService courseSynchronizationService;
    @Mock
    private CourseCrawlerTargetService crawlerTargetService;
    @Mock
    private CrawlerRunStateService crawlerRunStateService;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter metricCounter;
    @Mock
    private JbnuCourseApiClient apiClient;
    @Mock
    private JbnuCourseParser courseParser;

    @InjectMocks
    private CourseCrawlerService courseCrawlerService;

    @Test
    @DisplayName("수집이 끝난 전체 결과를 Spring Batch 없이 동기화 서비스에 전달한다")
    void crawlAndSaveCourses_FetchesThenSynchronizes() {
        ParsedCourseDto course = mock(ParsedCourseDto.class);
        given(crawlerTargetService.getCurrentTargetValue())
                .willReturn(new CrawlTargetInfo(YEAR, SemesterType.FIRST_SEMESTER));
        given(courseCrawlFetcher.fetch(YEAR, SEMESTER)).willReturn(List.of(course));
        given(meterRegistry.counter("crawler.runs", "status", "STARTED")).willReturn(metricCounter);
        given(meterRegistry.counter("crawler.runs", "status", "SUCCEEDED")).willReturn(metricCounter);

        boolean started = courseCrawlerService.crawlAndSaveCourses();

        assertThat(started).isTrue();
        verify(courseSynchronizationService).synchronize(List.of(course));
    }

    @Test
    @DisplayName("외부 수집·파싱 실패는 FETCH_PARSE 단계로 별도 기록한다")
    void fetchFailure_IsRecordedWithFetchParseStage() {
        CustomException failure = new CustomException(ErrorCode.CRAWLER_PARSING_ERROR);
        given(courseCrawlFetcher.fetch(YEAR, SEMESTER)).willThrow(failure);

        assertThatThrownBy(() -> courseCrawlerService.crawlAndSaveCourses(YEAR, SEMESTER))
                .isSameAs(failure);
        verify(crawlerRunStateService).recordFailure(CrawlerFailureStage.FETCH_PARSE, failure);
    }

    @Test
    @DisplayName("필수 필드가 누락된 외부 행은 DB 트랜잭션 전에 거부하고 FETCH_PARSE로 기록한다")
    void missingRequiredField_IsRejectedBeforeSynchronization() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        ParsedCourseDto missingName = new ParsedCourseDto(
                "course-1", "COMP101", null, null, "01", null, 50, 20,
                null, YEAR, SEMESTER, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, List.of());
        CourseCrawlFetcher validatingFetcher = new CourseCrawlFetcher(apiClient, courseParser);
        ReflectionTestUtils.setField(validatingFetcher, "certDivisions", List.of("1", "2", "5", "7", "3", "4"));
        ReflectionTestUtils.setField(validatingFetcher, "interRequestDelayMs", 0L);
        CourseCrawlerService service = new CourseCrawlerService(
                validatingFetcher, courseSynchronizationService, crawlerTargetService, crawlerRunStateService,
                meterRegistry);
        given(apiClient.fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString())).willReturn(stream);
        given(courseParser.streamCourses(any(), eq(YEAR), eq(SEMESTER)))
                .willReturn(List.of(missingName).iterator())
                .willReturn(Collections.emptyIterator());

        assertThatThrownBy(() -> service.crawlAndSaveCourses(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLER_PARSING_ERROR));
        verify(courseSynchronizationService, never()).synchronize(any());
        verify(crawlerRunStateService)
                .recordFailure(eq(CrawlerFailureStage.FETCH_PARSE), any(CustomException.class));
    }

    @Test
    @DisplayName("DB 동기화 실패는 rollback 이후 PERSIST 단계로 별도 기록한다")
    void persistenceFailure_IsRecordedWithPersistStage() {
        ParsedCourseDto course = mock(ParsedCourseDto.class);
        RuntimeException failure = new IllegalStateException("database unavailable");
        given(courseCrawlFetcher.fetch(YEAR, SEMESTER)).willReturn(List.of(course));
        org.mockito.Mockito.doThrow(failure).when(courseSynchronizationService).synchronize(List.of(course));

        assertThatThrownBy(() -> courseCrawlerService.crawlAndSaveCourses(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.FAILED_TO_CRAWL_COURSES));
        verify(crawlerRunStateService).recordFailure(CrawlerFailureStage.PERSIST, failure);
    }

    @Test
    @DisplayName("이미 실행 중이면 중복 크롤링을 시작하지 않는다")
    void crawlAndSaveCourses_SkipsWhenAlreadyRunning() {
        setCrawlingFlag(true);

        assertThat(courseCrawlerService.crawlAndSaveCourses(YEAR, SEMESTER)).isFalse();
    }

    @Test
    @DisplayName("crawler_status의 마지막 성공 시각이 임계치를 넘으면 관리자 경고를 남긴다")
    void staleDataEmitsAdminAlert() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CourseCrawlerService service = new CourseCrawlerService(
                courseCrawlFetcher, courseSynchronizationService, crawlerTargetService, crawlerRunStateService,
                registry);
        ReflectionTestUtils.setField(service, "staleThresholdMinutes", 15L);
        given(crawlerRunStateService.findLastSuccessAt())
                .willReturn(Optional.of(LocalDateTime.now().minusMinutes(20)));
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            service.checkDataFreshness();

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("Data freshness is stale")
                            && message.contains("alert=ADMIN"));
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("freshness gauge는 서비스 생성 시 한 번 등록된다")
    void freshnessGaugesAreRegisteredAtConstruction() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new CourseCrawlerService(
                courseCrawlFetcher, courseSynchronizationService, crawlerTargetService, crawlerRunStateService,
                registry);

        assertThat(registry.find("crawler.data.freshness.age.seconds").gauges()).hasSize(1);
        assertThat(registry.find("crawler.data.stale").gauges()).hasSize(1);
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(CourseCrawlerService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        logger.addAppender(appender);
        appender.start();
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(CourseCrawlerService.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private void setCrawlingFlag(boolean crawling) {
        AtomicBoolean flag = (AtomicBoolean) ReflectionTestUtils.getField(courseCrawlerService, "isCrawling");
        assertThat(flag).isNotNull();
        flag.set(crawling);
    }
}
