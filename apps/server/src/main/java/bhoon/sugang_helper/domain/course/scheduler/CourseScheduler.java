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

    @Scheduled(cron = "0 0/5 * * * ?") // 5분
    public void runCrawler() {
        log.info("[스케줄러] 강의 크롤링 작업을 시작합니다.");
        try {
            courseCrawlerService.crawlAndSaveCourses();
            log.info("[스케줄러] 강의 크롤링 작업을 완료했습니다.");
        } catch (Exception e) {
            log.error("[스케줄러] 강의 크롤링 작업에 실패했습니다.", e);
        }
    }
}
