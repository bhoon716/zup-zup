package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourse;
import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.crawling.dto.diagnostic.CrawlerFailureSummary;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.enums.CrawlerFailureStage;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CourseCrawlerService {

    private static final String EVENT_KEY = "event";
    private static final String RESULT_KEY = "result";
    private static final String YEAR_KEY = "year";
    private static final String SEMESTER_KEY = "semester";
    private static final String STAGE_KEY = "stage";
    private static final String COURSE_COUNT_KEY = "courseCount";
    private static final String ELAPSED_MS_KEY = "elapsedMs";
    private static final String CRAWLER_RUN_ID_KEY = "crawlerRunId";
    private static final String RESULT_SUCCESS = "success";

    private static final String METRIC_STATUS_SKIPPED = "SKIPPED";
    private static final String METRIC_STATUS_STARTED = "STARTED";
    private static final String METRIC_STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String METRIC_STATUS_FAILED = "FAILED";

    private final CourseCrawlFetcher courseCrawlFetcher;
    private final CourseCrawlCompletionService courseCrawlCompletionService;
    private final CourseCrawlerTargetService crawlerTargetService;
    private final CrawlerRunStateService crawlerRunStateService;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean isCrawling = new AtomicBoolean(false);

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
    }

    public boolean crawlAndSaveCourses() {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        return crawlAndSaveCourses(target.year(), target.semester().getCode());
    }

    public boolean crawlAndSaveCourses(String year, String semester) {
        if (!isCrawling.compareAndSet(false, true)) {
            incrementCounter(METRIC_STATUS_SKIPPED);
            logCrawlSkipped("current");
            return false;
        }

        incrementCounter(METRIC_STATUS_STARTED);
        try {
            executeCrawl(year, semester);
            incrementCounter(METRIC_STATUS_SUCCEEDED);
            return true;
        } catch (RuntimeException exception) {
            incrementCounter(METRIC_STATUS_FAILED);
            throw exception;
        } finally {
            isCrawling.set(false);
        }
    }

    public boolean crawlRecentYears() {
        if (!isCrawling.compareAndSet(false, true)) {
            incrementCounter(METRIC_STATUS_SKIPPED);
            logCrawlSkipped("recent-years");
            return false;
        }

        incrementCounter(METRIC_STATUS_STARTED);
        try {
            if (crawlRecentTargets()) {
                incrementCounter(METRIC_STATUS_SUCCEEDED);
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
                incrementCounter(METRIC_STATUS_FAILED);
            }
        }
        return !failed;
    }

    private void logCrawlSkipped(String targetScope) {
        log.atWarn().addKeyValue(EVENT_KEY, "run_skipped").addKeyValue("targetScope", targetScope)
                .addKeyValue(RESULT_KEY, "skipped").log("[Crawler] 다른 수집이 실행 중이므로 중복 실행 생략.");
    }

    private void executeCrawl(String year, String semester) {
        CrawlerFailureStage stage = CrawlerFailureStage.FETCH_PARSE;
        String runId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(CRAWLER_RUN_ID_KEY, runId);
        try {
            logCrawlStarted(runId, year, semester, stage);
            List<ParsedCourse> parsedCourses = courseCrawlFetcher.fetch(year, semester);
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
            MDC.remove(CRAWLER_RUN_ID_KEY);
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
                .addKeyValue(RESULT_KEY, RESULT_SUCCESS).addKeyValue(COURSE_COUNT_KEY, courseCount)
                .addKeyValue(ELAPSED_MS_KEY, elapsedMs(startedAt))
                .log("[Crawler] 수집 단계 완료. stage_completed runId={} year={} semester={} stage={} result=success "
                                + "courseCount={} elapsedMs={}",
                        runId, year, semester, stage, courseCount, elapsedMs(startedAt));
    }

    private void logCrawlSucceeded(String runId, String year, String semester, CrawlerFailureStage stage,
                                   int courseCount, long startedAt) {
        log.atInfo().addKeyValue(EVENT_KEY, "run_succeeded").addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester).addKeyValue(STAGE_KEY, stage.name())
                .addKeyValue(RESULT_KEY, RESULT_SUCCESS).addKeyValue(COURSE_COUNT_KEY, courseCount)
                .addKeyValue(ELAPSED_MS_KEY, elapsedMs(startedAt))
                .log("[Crawler] 수집 완료. run_succeeded runId={} year={} semester={} stage={} result=success "
                        + "courseCount={} elapsedMs={}",
                runId, year, semester, stage, courseCount, elapsedMs(startedAt));
    }

    private void logCrawlFailed(String runId, String year, String semester, CrawlerFailureSummary summary,
                                RuntimeException exception, long startedAt) {
        log.atError().addKeyValue(EVENT_KEY, "run_failed").addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester).addKeyValue(STAGE_KEY, summary.stage().name())
                .addKeyValue(RESULT_KEY, "failed").addKeyValue(ELAPSED_MS_KEY, elapsedMs(startedAt))
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
