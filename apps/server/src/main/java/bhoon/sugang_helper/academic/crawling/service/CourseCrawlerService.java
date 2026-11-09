package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.crawling.dto.diagnostic.CrawlerFailureSummary;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CourseCrawlerService {

    private static final String EVENT_KEY = "event";
    private static final String RESULT_KEY = "result";
    private static final String YEAR_KEY = "year";
    private static final String SEMESTER_KEY = "semester";
    private static final String STAGE_KEY = "stage";

    private final CourseCrawlFetcher courseCrawlFetcher;
    private final CourseCrawlCompletionService courseCrawlCompletionService;
    private final CourseCrawlerTargetService crawlerTargetService;
    private final CrawlerRunStateService crawlerRunStateService;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean isCrawling = new AtomicBoolean(false);
    private final AtomicLong freshnessAgeSeconds = new AtomicLong(-1);
    private final AtomicLong staleGauge = new AtomicLong(1);
    private final AtomicLong lastSuccessTimestamp = new AtomicLong();
    private final AtomicLong freshnessCheckTimestamp = new AtomicLong();
    private final AtomicLong freshnessCheckHealthy = new AtomicLong();
    private final AtomicBoolean staleAlertActive = new AtomicBoolean(false);

    @Value("${jbnu.crawler.stale-threshold-minutes:15}")
    private long staleThresholdMinutes;

    public CourseCrawlerService(CourseCrawlFetcher courseCrawlFetcher,
                                CourseCrawlCompletionService courseCrawlCompletionService,
                                CourseCrawlerTargetService crawlerTargetService,
                                CrawlerRunStateService crawlerRunStateService,
                                MeterRegistry meterRegistry) {
        this.courseCrawlFetcher = courseCrawlFetcher;
        this.courseCrawlCompletionService = courseCrawlCompletionService;
        this.crawlerTargetService = crawlerTargetService;
        this.crawlerRunStateService = crawlerRunStateService;
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            meterRegistry.gauge("crawler.data.freshness.age.seconds", freshnessAgeSeconds);
            meterRegistry.gauge("crawler.data.stale", staleGauge);
            meterRegistry.gauge("crawler.data.last.success.timestamp.seconds", lastSuccessTimestamp);
            meterRegistry.gauge("crawler.data.freshness.check.last.success.timestamp.seconds", freshnessCheckTimestamp);
            meterRegistry.gauge("crawler.data.freshness.check.healthy", freshnessCheckHealthy);
            meterRegistry.gauge("crawler.data.freshness.threshold.seconds", this,
                    service -> Duration.ofMinutes(service.staleThresholdMinutes).toSeconds());
        }
    }

    public boolean crawlAndSaveCourses() {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        return crawlAndSaveCourses(target.year(), target.semester().getCode());
    }

    public boolean crawlAndSaveCourses(String year, String semester) {
        if (!isCrawling.compareAndSet(false, true)) {
            incrementCounter("SKIPPED");
            logCrawlSkipped("current");
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
            logCrawlSkipped("recent-years");
            return false;
        }

        incrementCounter("STARTED");
        try {
            if (crawlRecentTargets()) {
                incrementCounter("SUCCEEDED");
            }
            return true;
        } finally {
            isCrawling.set(false);
        }
    }

    private boolean crawlRecentTargets() {
        boolean failed = false;
        int currentYear = Year.now().getValue();
        for (int year = currentYear; year > currentYear - 3; year--) {
            if (!crawlYear(String.valueOf(year))) {
                failed = true;
            }
        }
        return !failed;
    }

    private boolean crawlYear(String year) {
        boolean failed = false;
        for (SemesterType semester : SemesterType.values()) {
            try {
                executeCrawl(year, semester.getCode());
            } catch (RuntimeException exception) {
                failed = true;
                incrementCounter("FAILED");
            }
        }
        return !failed;
    }

    @Scheduled(fixedDelayString = "${jbnu.crawler.freshness-check-ms:60000}", scheduler = "monitoringTaskScheduler")
    public void checkDataFreshness() {
        if (crawlerRunStateService == null || meterRegistry == null) {
            return;
        }
        try {
            refreshDataFreshness();
            freshnessCheckHealthy.set(1);
            freshnessCheckTimestamp.set(Instant.now().getEpochSecond());
        } catch (RuntimeException exception) {
            freshnessCheckHealthy.set(0);
            freshnessAgeSeconds.set(-1);
            staleGauge.set(1);
            logFreshnessCheckFailed(exception);
        }
    }

    private void refreshDataFreshness() {
        Optional<LocalDateTime> latest = crawlerRunStateService.findLastSuccessAt();
        lastSuccessTimestamp.set(latest.map(value -> value.atZone(ZoneId.systemDefault()).toEpochSecond()).orElse(0L));
        long ageSeconds = latest.map(value -> Math.max(0, Duration.between(value, LocalDateTime.now()).toSeconds()))
                .orElse(-1L);
        freshnessAgeSeconds.set(ageSeconds);
        boolean stale = ageSeconds < 0 || ageSeconds > Duration.ofMinutes(staleThresholdMinutes).toSeconds();
        if (!stale) {
            staleGauge.set(0);
            staleAlertActive.set(false);
            return;
        }
        staleGauge.set(1);
        if (!staleAlertActive.compareAndSet(false, true)) {
            return;
        }
        logDataStale(ageSeconds);
    }

    private void logCrawlSkipped(String targetScope) {
        log.atWarn().addKeyValue(EVENT_KEY, "run_skipped").addKeyValue("targetScope", targetScope)
                .addKeyValue(RESULT_KEY, "skipped").log("[Crawler] 다른 수집이 실행 중이므로 중복 실행 생략.");
    }

    private void logFreshnessCheckFailed(RuntimeException exception) {
        log.atError().addKeyValue(EVENT_KEY, "crawler_freshness_check_failed")
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(exception))
                .log("[Crawler] 강의 데이터 신선도 확인 실패.");
    }

    private void logDataStale(long ageSeconds) {
        log.atError().addKeyValue(EVENT_KEY, "crawler_data_stale").addKeyValue("ageSeconds", ageSeconds)
                .addKeyValue("thresholdMinutes", staleThresholdMinutes).addKeyValue("alert", "ADMIN")
                .log("[Crawler] 강의 데이터 신선도 기준 초과. ageSeconds={}, thresholdMinutes={}, alert=ADMIN",
                        ageSeconds, staleThresholdMinutes);
    }

    private void executeCrawl(String year, String semester) {
        CrawlerFailureStage stage = CrawlerFailureStage.FETCH_PARSE;
        String runId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put("crawlerRunId", runId);
        try {
            logCrawlStarted(runId, year, semester, stage);
            List<ParsedCourseDto> parsedCourses = courseCrawlFetcher.fetch(year, semester);
            logFetchCompleted(runId, year, semester, stage, parsedCourses.size(), startedAt);
            stage = CrawlerFailureStage.PERSIST;
            courseCrawlCompletionService.complete(parsedCourses);
            logCrawlSucceeded(runId, year, semester, stage, parsedCourses.size(), startedAt);
        } catch (RuntimeException exception) {
            CrawlerFailureSummary summary = CrawlerFailureSummary.from(stage, exception);
            logCrawlFailed(runId, year, semester, summary, exception, startedAt);
            recordFailureWithoutMasking(stage, exception);
            if (exception instanceof CustomException customException) {
                throw customException;
            }
            throw new CustomException(ErrorCode.FAILED_TO_CRAWL_COURSES);
        } finally {
            MDC.remove("crawlerRunId");
        }
    }

    private void logCrawlStarted(String runId, String year, String semester, CrawlerFailureStage stage) {
        log.atInfo().addKeyValue(EVENT_KEY, "run_started").addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester).addKeyValue(STAGE_KEY, stage.name())
                .addKeyValue(RESULT_KEY, "started")
                .log("[Crawler] 수집 시작. run_started runId={} year={} semester={} stage={} result=started elapsedMs=0",
                runId, year, semester, stage);
    }

    private void logFetchCompleted(String runId, String year, String semester, CrawlerFailureStage stage,
                                   int courseCount, long startedAt) {
        log.atDebug().addKeyValue(EVENT_KEY, "stage_completed").addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester).addKeyValue(STAGE_KEY, stage.name())
                .addKeyValue(RESULT_KEY, "success").addKeyValue("courseCount", courseCount)
                .addKeyValue("elapsedMs", elapsedMs(startedAt))
                .log("[Crawler] 수집 단계 완료. stage_completed runId={} year={} semester={} stage={} result=success "
                                + "courseCount={} elapsedMs={}",
                        runId, year, semester, stage, courseCount, elapsedMs(startedAt));
    }

    private void logCrawlSucceeded(String runId, String year, String semester, CrawlerFailureStage stage,
                                   int courseCount, long startedAt) {
        log.atInfo().addKeyValue(EVENT_KEY, "run_succeeded").addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester).addKeyValue(STAGE_KEY, stage.name())
                .addKeyValue(RESULT_KEY, "success").addKeyValue("courseCount", courseCount)
                .addKeyValue("elapsedMs", elapsedMs(startedAt))
                .log("[Crawler] 수집 완료. run_succeeded runId={} year={} semester={} stage={} result=success "
                        + "courseCount={} elapsedMs={}",
                runId, year, semester, stage, courseCount, elapsedMs(startedAt));
    }

    private void logCrawlFailed(String runId, String year, String semester, CrawlerFailureSummary summary,
                                RuntimeException exception, long startedAt) {
        log.atError().addKeyValue(EVENT_KEY, "run_failed").addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester).addKeyValue(STAGE_KEY, summary.stage().name())
                .addKeyValue(RESULT_KEY, "failed").addKeyValue("elapsedMs", elapsedMs(startedAt))
                .addKeyValue("errorCode", SensitiveDataRedactor.failureCode(exception))
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(exception))
                .addKeyValue("rootCauseType", summary.failureType())
                .addKeyValue("retryable", summary.retryableLabel())
                .addKeyValue("upstreamStatus", summary.upstreamStatusLabel())
                .log("[Crawler] 수집 실패. run_failed runId={} year={} semester={} stage={} result=failed elapsedMs={} "
                        + "failureCode={} exceptionType={} retryable={} upstreamStatus={}",
                runId, year, semester, summary.stage(), elapsedMs(startedAt),
                SensitiveDataRedactor.failureCode(exception), summary.failureType(),
                summary.retryableLabel(), summary.upstreamStatusLabel());
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void recordFailureWithoutMasking(CrawlerFailureStage stage, RuntimeException originalException) {
        try {
            crawlerRunStateService.recordFailure(stage, originalException);
        } catch (RuntimeException recordingException) {
            logFailureHandlingFailed(stage, originalException, recordingException);
        }
    }

    private void logFailureHandlingFailed(CrawlerFailureStage stage, RuntimeException originalException,
                                          RuntimeException recordingException) {
        log.atError().addKeyValue(EVENT_KEY, "crawler_failure_handling_failed").addKeyValue(STAGE_KEY, stage.name())
                .addKeyValue("originalType", SensitiveDataRedactor.exceptionType(originalException))
                .addKeyValue("recordingType", SensitiveDataRedactor.exceptionType(recordingException))
                .log("[Crawler] 수집 실패 이력 저장 실패. stage={}, originalType={}, recordingType={}",
                        stage, SensitiveDataRedactor.exceptionType(originalException),
                        SensitiveDataRedactor.exceptionType(recordingException));
    }

    private void incrementCounter(String status) {
        if (meterRegistry == null) {
            return;
        }
        Counter counter = meterRegistry.counter("crawler.runs", "status", status);
        if (counter == null) {
            return;
        }
        counter.increment();
    }
}
