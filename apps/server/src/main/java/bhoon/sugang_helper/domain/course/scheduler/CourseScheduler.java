package bhoon.sugang_helper.domain.course.scheduler;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseScheduler {

    private final CourseCrawlerService courseCrawlerService;

    @Scheduled(cron = "0 0/5 * * * ?") // Every 5 minutes
    public void runCrawler() {
        log.info("Scheduled crawler task started.");
        try {
            courseCrawlerService.crawlAndSaveCourses();
            log.info("Scheduled crawler task completed.");
        } catch (Exception e) {
            log.error("Scheduled crawler task failed.", e);
        }
    }
}
