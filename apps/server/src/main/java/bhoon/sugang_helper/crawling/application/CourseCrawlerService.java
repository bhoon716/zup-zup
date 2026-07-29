package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.SemesterType;
import bhoon.sugang_helper.crawling.domain.CrawlerFailureStage;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 외부 강의 데이터를 모두 수집·검증한 뒤 하나의 DB 트랜잭션으로 동기화합니다.
 */
@Service
@Slf4j
public class CourseCrawlerService {

    private final CourseCrawlFetcher courseCrawlFetcher;
    private final CourseSynchronizationService courseSynchronizationService;
    private final CourseCrawlerTargetService crawlerTargetService;
    private final CrawlerRunStateService crawlerRunStateService;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean isCrawling = new AtomicBoolean(false);
    private final AtomicLong freshnessAgeSeconds = new AtomicLong(-1);
    private final AtomicLong staleGauge = new AtomicLong(1);
    private final AtomicBoolean staleAlertActive = new AtomicBoolean(false);

    @Value("${jbnu.crawler.stale-threshold-minutes:15}")
    private long staleThresholdMinutes;

    public CourseCrawlerService(CourseCrawlFetcher courseCrawlFetcher,
                                CourseSynchronizationService courseSynchronizationService,
                                CourseCrawlerTargetService crawlerTargetService,
                                CrawlerRunStateService crawlerRunStateService,
                                MeterRegistry meterRegistry) {
        this.courseCrawlFetcher = courseCrawlFetcher;
        this.courseSynchronizationService = courseSynchronizationService;
        this.crawlerTargetService = crawlerTargetService;
        this.crawlerRunStateService = crawlerRunStateService;
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            meterRegistry.gauge("crawler.data.freshness.age.seconds", freshnessAgeSeconds);
            meterRegistry.gauge("crawler.data.stale", staleGauge);
        }
    }

    public boolean crawlAndSaveCourses() {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        return crawlAndSaveCourses(target.year(), target.semester().getCode());
    }

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
                        executeCrawl(year, semester.getCode());
                    } catch (RuntimeException exception) {
                        failed = true;
                        incrementCounter("FAILED");
                        log.warn("[Crawler] Historical crawl failed. year={}, semester={}, failureCode={}, "
                                        + "exceptionType={}",
                                year, semester.getDescription(), SensitiveDataRedactor.failureCode(exception),
                                SensitiveDataRedactor.exceptionType(exception));
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
        if (crawlerRunStateService == null || meterRegistry == null) {
            return;
        }
        Optional<LocalDateTime> latest = crawlerRunStateService.findLastSuccessAt();
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

    private void executeCrawl(String year, String semester) {
        CrawlerFailureStage stage = CrawlerFailureStage.FETCH_PARSE;
        try {
            log.info("[Crawler] Fetching complete course crawl. year={}, semester={}", year, semester);
            List<ParsedCourseDto> parsedCourses = courseCrawlFetcher.fetch(year, semester);
            stage = CrawlerFailureStage.PERSIST;
            courseSynchronizationService.synchronize(parsedCourses);
            log.info("[Crawler] Completed course crawl. year={}, semester={}, courseCount={}",
                    year, semester, parsedCourses.size());
        } catch (RuntimeException exception) {
            recordFailureWithoutMasking(stage, exception);
            if (exception instanceof CustomException customException) {
                throw customException;
            }
            throw new CustomException(ErrorCode.FAILED_TO_CRAWL_COURSES);
        }
    }

    private void recordFailureWithoutMasking(CrawlerFailureStage stage, RuntimeException originalException) {
        try {
            crawlerRunStateService.recordFailure(stage, originalException);
        } catch (RuntimeException recordingException) {
            log.error("[Crawler] Failed to persist crawler failure. stage={}, originalType={}, recordingType={}",
                    stage, SensitiveDataRedactor.exceptionType(originalException),
                    SensitiveDataRedactor.exceptionType(recordingException));
        }
    }

    private void incrementCounter(String status) {
        if (meterRegistry != null) {
            io.micrometer.core.instrument.Counter counter = meterRegistry.counter("crawler.runs", "status", status);
            if (counter != null) {
                counter.increment();
            }
        }
    }
}
