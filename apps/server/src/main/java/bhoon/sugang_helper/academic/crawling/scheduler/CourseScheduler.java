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
            log.info("[Init] 시작 시 최근 3개년 강의 수집.");
            try {
                courseCrawlerService.crawlRecentYears();
            } catch (Exception e) {
                log.error("[Init] 시작 시 강의 수집 실패. exceptionType={}",
                        SensitiveDataRedactor.exceptionType(e));
            }
        }
    }




    @Scheduled(cron = "${jbnu.crawler.cron}")
    public void runCrawler() {
        log.info("[Scheduler] 주기 강의 수집 시작.");
        boolean started = courseCrawlerService.crawlAndSaveCourses();
        log.info(started ? "[Scheduler] 주기 강의 수집 완료."
                : "[Scheduler] 다른 수집이 실행 중이므로 중복 실행 생략.");
    }
}
