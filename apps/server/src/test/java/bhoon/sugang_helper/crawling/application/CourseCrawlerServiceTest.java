package bhoon.sugang_helper.crawling.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import bhoon.sugang_helper.course.domain.SemesterType;
import bhoon.sugang_helper.course.domain.CourseRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CourseCrawlerServiceTest {

    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job crawlJob;
    @Mock
    private CourseCrawlerTargetService crawlerTargetService;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter metricCounter;

    @InjectMocks
    private CourseCrawlerService courseCrawlerService;

    @Test
    @DisplayName("crawlAndSaveCourses 실행 시 JobLauncher를 통해 배치 Job이 성공적으로 실행된다")
    void crawlAndSaveCourses_LaunchesJobSuccessfully() throws Exception {
        // given
        CrawlTargetInfo target = new CrawlTargetInfo("2026", SemesterType.FIRST_SEMESTER);
        given(crawlerTargetService.getCurrentTargetValue()).willReturn(target);
        given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(jobExecution);
        given(jobExecution.getStatus()).willReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
        given(meterRegistry.counter("crawler.runs", "status", "STARTED")).willReturn(metricCounter);
        given(meterRegistry.counter("crawler.runs", "status", "SUCCEEDED")).willReturn(metricCounter);

        // when
        boolean started = courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
        org.assertj.core.api.Assertions.assertThat(started).isTrue();
        verify(meterRegistry).counter("crawler.runs", "status", "STARTED");
        verify(meterRegistry).counter("crawler.runs", "status", "SUCCEEDED");
    }

    @Test
    @DisplayName("crawlAndSaveCourses가 이미 실행 중이면 타겟 크롤링 스킵 로그를 남긴다")
    void crawlAndSaveCourses_LogsTargetSkipMessageWhenAlreadyRunning() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            setCrawlingFlag(true);

            boolean started = courseCrawlerService.crawlAndSaveCourses("2026", "U211600010");

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains("[Crawler] Target crawl is already in progress. Skipping.");
            org.assertj.core.api.Assertions.assertThat(started).isFalse();
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("crawlRecentYears가 이미 실행 중이면 히스토리 크롤링 스킵 로그를 남긴다")
    void crawlRecentYears_LogsHistoricalSkipMessageWhenAlreadyRunning() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            setCrawlingFlag(true);

            courseCrawlerService.crawlRecentYears();

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains("[Crawler] Historical crawl is already in progress. Skipping.");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("마지막 크롤링 데이터가 stale threshold를 넘으면 관리자 경고를 남긴다")
    void staleDataEmitsAdminAlert() {
        ReflectionTestUtils.setField(courseCrawlerService, "staleThresholdMinutes", 15L);
        given(courseRepository.findMaxLastCrawledAt()).willReturn(Optional.of(LocalDateTime.now().minusMinutes(20)));
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            courseCrawlerService.checkDataFreshness();

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

        new CourseCrawlerService(jobLauncher, crawlJob, crawlerTargetService, courseRepository, registry);

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
