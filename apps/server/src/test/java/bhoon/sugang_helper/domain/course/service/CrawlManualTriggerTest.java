package bhoon.sugang_helper.domain.course.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("prod")
@Tag("manual")
public class CrawlManualTriggerTest {

    @Autowired
    private CourseCrawlerService courseCrawlerService;

    /**
     * 강의 크롤링 프로세스를 수동으로 즉시 실행하여 작동 여부를 테스트합니다.
     */
    @Test
    void triggerCrawl() {
        System.out.println("Starting manual crawl trigger...");
        courseCrawlerService.crawlAndSaveCourses();
        System.out.println("Manual crawl trigger completed.");
    }
}
