package bhoon.sugang_helper.crawling.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.course.domain.SemesterType;
import bhoon.sugang_helper.crawling.application.CrawlTargetInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceTest {

    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job crawlJob;
    @Mock
    private CourseCrawlerTargetService crawlerTargetService;
    @Mock
    private JobExecution jobExecution;

    @InjectMocks
    private CourseCrawlerService courseCrawlerService;

    @Test
    @DisplayName("crawlAndSaveCourses 실행 시 JobLauncher를 통해 배치 Job이 성공적으로 실행된다")
    void crawlAndSaveCourses_LaunchesJobSuccessfully() throws Exception {
        // given
        CrawlTargetInfo target = new CrawlTargetInfo("2026", SemesterType.FIRST_SEMESTER);
        given(crawlerTargetService.getCurrentTargetValue()).willReturn(target);
        given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(jobExecution);
        given(jobExecution.getStatus()).willReturn(org.springframework.batch.core.BatchStatus.COMPLETED);

        // when
        courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
    }
}
