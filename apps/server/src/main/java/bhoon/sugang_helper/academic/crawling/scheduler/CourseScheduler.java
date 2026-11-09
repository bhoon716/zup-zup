package bhoon.sugang_helper.academic.crawling.scheduler;

import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerService;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseScheduler {

    private static final String EVENT_KEY = "event";

    private final CourseCrawlerService courseCrawlerService;

    @Value("${jbnu.crawler.run-on-startup:false}")
    private boolean runOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (runOnStartup) {
            logStartupCrawlStarted();
            try {
                courseCrawlerService.crawlRecentYears();
            } catch (Exception e) {
                logStartupCrawlFailed(SensitiveDataRedactor.exceptionType(e));
            }
        }
    }

    @Scheduled(cron = "${jbnu.crawler.cron}")
    public void runCrawler() {
        logScheduledCrawlStarted();
        logScheduledCrawlResult(courseCrawlerService.crawlAndSaveCourses());
    }

    private void logStartupCrawlStarted() {
        log.atInfo().addKeyValue(EVENT_KEY, "crawler_startup_started")
                .log("[Init] 시작 시 최근 3개년 강의 수집.");
    }

    private void logStartupCrawlFailed(String exceptionType) {
        log.atError().addKeyValue(EVENT_KEY, "crawler_startup_failed")
                .addKeyValue("exceptionType", exceptionType)
                .log("[Init] 시작 시 강의 수집 실패.");
    }

    private void logScheduledCrawlStarted() {
        log.atInfo().addKeyValue(EVENT_KEY, "crawler_scheduled_started")
                .log("[Scheduler] 주기 강의 수집 시작.");
    }

    private void logScheduledCrawlResult(boolean started) {
        if (!started) {
            log.atInfo().addKeyValue(EVENT_KEY, "crawler_scheduled_skipped")
                    .log("[Scheduler] 다른 수집이 실행 중이므로 중복 실행 생략.");
            return;
        }
        log.atInfo().addKeyValue(EVENT_KEY, "crawler_scheduled_completed")
                .log("[Scheduler] 주기 강의 수집 완료.");
    }
}
