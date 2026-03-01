package bhoon.sugang_helper.domain.course.scheduler;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseScheduler {

    private final CourseCrawlerService courseCrawlerService;

    @Value("${jbnu.crawler.run-on-startup:false}")
    private boolean runOnStartup;

    /**
     * 애플리케이션 시작 시 설정에 따라 크롤링을 1회 즉시 실행합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (runOnStartup) {
            log.info("[초기화] 애플리케이션 시작 시 최근 3개년 강의 데이터를 크롤링합니다.");
            try {
                courseCrawlerService.crawlRecentYears();
            } catch (Exception e) {
                log.error("[초기화] 초기 크롤링 작업 중 오류가 발생했으나 서비스를 시작합니다. 사유: {}", e.getMessage());
            }
        }
    }

    /**
     * 설정 시간마다 강의 크롤링 작업을 실행합니다.
     */
    @Scheduled(cron = "${jbnu.crawler.cron}")
    public void runCrawler() {
        log.info("[스케줄러] 강의 크롤링 작업을 시작합니다.");
        courseCrawlerService.crawlAndSaveCourses();
        log.info("[스케줄러] 강의 크롤링 작업을 완료했습니다.");
    }
}
