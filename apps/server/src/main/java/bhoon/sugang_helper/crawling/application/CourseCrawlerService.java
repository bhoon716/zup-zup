package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.course.domain.SemesterType;
import bhoon.sugang_helper.course.domain.CourseRepository;
import java.time.Year;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 전북대학교 오아시스 시스템으로부터 강의 정보를 크롤링하고 데이터베이스에 동기화하는 서비스입니다. Spring Batch의 JobLauncher를 통해 배치 작업을 수행합니다.
 */
@Service
@Slf4j
public class CourseCrawlerService {

    private final JobLauncher jobLauncher;
    private final Job crawlJob;
    private final CourseCrawlerTargetService crawlerTargetService;
    private final CourseRepository courseRepository;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean isCrawling = new AtomicBoolean(false);
    private final AtomicLong freshnessAgeSeconds = new AtomicLong(-1);
    private final AtomicLong staleGauge = new AtomicLong(1);
    private final AtomicBoolean staleAlertActive = new AtomicBoolean(false);
    @Value("${jbnu.crawler.stale-threshold-minutes:15}")
    private long staleThresholdMinutes;

    public CourseCrawlerService(JobLauncher jobLauncher, Job crawlJob,
                                CourseCrawlerTargetService crawlerTargetService,
                                CourseRepository courseRepository, MeterRegistry meterRegistry) {
        this.jobLauncher = jobLauncher;
        this.crawlJob = crawlJob;
        this.crawlerTargetService = crawlerTargetService;
        this.courseRepository = courseRepository;
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            meterRegistry.gauge("crawler.data.freshness.age.seconds", freshnessAgeSeconds);
            meterRegistry.gauge("crawler.data.stale", staleGauge);
        }
    }

    /**
     * 강의 크롤링 및 저장 수행 (중복 실행 방지 적용)
     */
    public boolean crawlAndSaveCourses() {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        return crawlAndSaveCourses(target.year(), target.semester().getCode());
    }

    /**
     * 특정 년도와 학기를 지정하여 강의 크롤링 및 저장을 수행합니다.
     */
    public boolean crawlAndSaveCourses(String year, String semester) {
        if (!isCrawling.compareAndSet(false, true)) {
            incrementCounter("SKIPPED");
            log.warn("[Crawler] Target crawl is already in progress. Skipping.");
            return false;
        }

        incrementCounter("STARTED");
        try {
            executeCrawl(year, semester);
            incrementCounter("SUCCEEDED");
            return true;
        } catch (RuntimeException exception) {
            incrementCounter("FAILED");
            throw exception;
        } finally {
            isCrawling.set(false);
        }
    }

    /**
     * 최근 3개년의 모든 학기에 대해 강의 데이터를 크롤링합니다.
     */
    public boolean crawlRecentYears() {
        if (!isCrawling.compareAndSet(false, true)) {
            incrementCounter("SKIPPED");
            log.warn("[Crawler] Historical crawl is already in progress. Skipping.");
            return false;
        }

        incrementCounter("STARTED");
        boolean failed = false;
        try {
            int currentYear = Year.now().getValue();
            for (int y = currentYear; y > currentYear - 3; y--) {
                String year = String.valueOf(y);
                for (SemesterType semester : SemesterType.values()) {
                    try {
                        log.info("[Crawler] Automatic crawling: year={}, semester={}", year, semester.getDescription());
                        executeCrawl(year, semester.getCode());
                    } catch (Exception e) {
                        failed = true;
                        incrementCounter("FAILED");
                        log.warn("[Crawler] Historical crawl failed. year={}, semester={}, failureCode={}, exceptionType={}",
                                year, semester.getDescription(), SensitiveDataRedactor.failureCode(e),
                                SensitiveDataRedactor.exceptionType(e));
                    }
                }
            }
            if (!failed) {
                incrementCounter("SUCCEEDED");
            }
            return true;
        } finally {
            isCrawling.set(false);
        }
    }

    @Scheduled(fixedDelayString = "${jbnu.crawler.freshness-check-ms:60000}")
    public void checkDataFreshness() {
        if (courseRepository == null || meterRegistry == null) {
            return;
        }
        Optional<LocalDateTime> latest = courseRepository.findMaxLastCrawledAt();
        long ageSeconds = latest.map(value -> Math.max(0, Duration.between(value, LocalDateTime.now()).toSeconds()))
                .orElse(-1L);
        freshnessAgeSeconds.set(ageSeconds);
        boolean stale = ageSeconds < 0 || ageSeconds > Duration.ofMinutes(staleThresholdMinutes).toSeconds();
        staleGauge.set(stale ? 1 : 0);
        if (stale && staleAlertActive.compareAndSet(false, true)) {
            log.error("[Crawler] Data freshness is stale. ageSeconds={}, thresholdMinutes={}, alert=ADMIN", ageSeconds,
                    staleThresholdMinutes);
        } else if (!stale) {
            staleAlertActive.set(false);
        }
    }

    private void incrementCounter(String status) {
        if (meterRegistry != null) {
            io.micrometer.core.instrument.Counter counter = meterRegistry.counter(
                    "crawler.runs", "status", status);
            if (counter != null) {
                counter.increment();
            }
        }
    }

    /**
     * Spring Batch Job을 실행하여 실제 크롤링 로직을 수행합니다.
     */
    private void executeCrawl(String year, String semester) {
        log.info("[Crawler] Starting Spring Batch course crawl. year={}, semester={}", year, semester);
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("year", year)
                    .addString("semester", semester)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(crawlJob, jobParameters);

            if (execution.getStatus().isUnsuccessful()) {
                log.error("[Crawler] Batch crawl job failed. status={}, failureCode={}",
                        execution.getStatus(), ErrorCode.CRAWLER_CONNECTION_ERROR.getCode());
                throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR, "크롤링 배치 작업 실패");
            }

            log.info("[Crawler] Completed Spring Batch course crawl. year={}, semester={}", year, semester);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Crawler] Batch crawl failed. failureCode={}, exceptionType={}",
                    ErrorCode.CRAWLER_CONNECTION_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }
}
