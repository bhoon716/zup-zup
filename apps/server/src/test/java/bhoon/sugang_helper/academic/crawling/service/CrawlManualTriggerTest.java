package bhoon.sugang_helper.academic.crawling.service;

import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("수동 강의 수집을 실행한다")
    void triggerCrawl() {
        // given
        System.out.println("Starting manual crawl trigger...");
        courseCrawlerService.crawlAndSaveCourses();

        // when
        System.out.println("Manual crawl trigger completed.");
    }
}
