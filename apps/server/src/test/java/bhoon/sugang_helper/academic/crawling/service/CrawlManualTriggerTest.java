package bhoon.sugang_helper.academic.crawling.service;

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
    void triggerCrawl() {
        System.out.println("Starting manual crawl trigger...");
        courseCrawlerService.crawlAndSaveCourses();
        System.out.println("Manual crawl trigger completed.");
    }
}
