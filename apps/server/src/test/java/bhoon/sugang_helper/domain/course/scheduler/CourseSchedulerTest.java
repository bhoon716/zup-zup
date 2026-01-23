package bhoon.sugang_helper.domain.course.scheduler;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseSchedulerTest {

    @InjectMocks
    private CourseScheduler courseScheduler;

    @Mock
    private CourseCrawlerService courseCrawlerService;

    @Test
    @DisplayName("스케줄러가 크롤러 호출하는지 확인")
    void runCrawler_calls_service() {
        // when
        courseScheduler.runCrawler();

        // then
        verify(courseCrawlerService, times(1)).crawlAndSaveCourses();
    }
}
