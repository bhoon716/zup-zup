package bhoon.sugang_helper.academic.crawling.scheduler;

import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerService;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
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

    private final CourseCrawlerService courseCrawlerService;

    @Value("${jbnu.crawler.run-on-startup:false}")
    private boolean runOnStartup;




    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (runOnStartup) {
            log.info("[Init] Crawling course data for the last 3 years on startup.");
            try {
                courseCrawlerService.crawlRecentYears();
            } catch (Exception e) {
                log.error("[Init] Initial crawl failed while starting service. exceptionType={}",
                        SensitiveDataRedactor.exceptionType(e));
            }
        }
    }




    @Scheduled(cron = "${jbnu.crawler.cron}")
    public void runCrawler() {
        log.info("[Scheduler] Starting course crawling task.");
        boolean started = courseCrawlerService.crawlAndSaveCourses();
        log.info(started ? "[Scheduler] Completed course crawling task."
                : "[Scheduler] Skipped course crawling task because another run is active.");
    }
}
