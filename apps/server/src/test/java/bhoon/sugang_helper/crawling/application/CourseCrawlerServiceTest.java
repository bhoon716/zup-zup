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
import java.util.concurrent.atomic.AtomicBoolean;
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
class CourseCrawlerServiceTest {

    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job crawlJob;
    @Mock
    private CourseCrawlerTargetService crawlerTargetService;
    @Mock
    private JobExecution jobExecution;

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

        // when
        courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("crawlAndSaveCourses가 이미 실행 중이면 타겟 크롤링 스킵 로그를 남긴다")
    void crawlAndSaveCourses_LogsTargetSkipMessageWhenAlreadyRunning() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            setCrawlingFlag(true);

            courseCrawlerService.crawlAndSaveCourses("2026", "U211600010");

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains("[Crawler] Target crawl is already in progress. Skipping.");
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
