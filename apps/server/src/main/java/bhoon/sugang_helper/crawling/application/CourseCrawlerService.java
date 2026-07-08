package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.course.domain.SemesterType;
import java.time.Year;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

/**
 * 전북대학교 오아시스 시스템으로부터 강의 정보를 크롤링하고 데이터베이스에 동기화하는 서비스입니다. Spring Batch의 JobLauncher를 통해 배치 작업을 수행합니다.
 */
@Service
@Slf4j
public class CourseCrawlerService {

    private final JobLauncher jobLauncher;
    private final Job crawlJob;
    private final CourseCrawlerTargetService crawlerTargetService;
    private final AtomicBoolean isCrawling = new AtomicBoolean(false);

    public CourseCrawlerService(JobLauncher jobLauncher, Job crawlJob,
                                CourseCrawlerTargetService crawlerTargetService) {
        this.jobLauncher = jobLauncher;
        this.crawlJob = crawlJob;
        this.crawlerTargetService = crawlerTargetService;
    }

    /**
     * 강의 크롤링 및 저장 수행 (중복 실행 방지 적용)
     */
    public void crawlAndSaveCourses() {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        crawlAndSaveCourses(target.year(), target.semester().getCode());
    }

    /**
     * 특정 년도와 학기를 지정하여 강의 크롤링 및 저장을 수행합니다.
     */
    public void crawlAndSaveCourses(String year, String semester) {
        if (!isCrawling.compareAndSet(false, true)) {
            log.warn("[Crawler] Target crawl is already in progress. Skipping.");
            return;
        }

        try {
            executeCrawl(year, semester);
        } finally {
            isCrawling.set(false);
        }
    }

    /**
     * 최근 3개년의 모든 학기에 대해 강의 데이터를 크롤링합니다.
     */
    public void crawlRecentYears() {
        if (!isCrawling.compareAndSet(false, true)) {
            log.warn("[Crawler] Historical crawl is already in progress. Skipping.");
            return;
        }

        try {
            int currentYear = Year.now().getValue();
            for (int y = currentYear; y > currentYear - 3; y--) {
                String year = String.valueOf(y);
                for (SemesterType semester : SemesterType.values()) {
                    try {
                        log.info("[Crawler] Automatic crawling: year={}, semester={}", year, semester.getDescription());
                        executeCrawl(year, semester.getCode());
                    } catch (Exception e) {
                        log.warn("[Crawler] Failed to crawl year={}, semester={} : {}", year, semester.getDescription(),
                                e.getMessage());
                    }
                }
            }
        } finally {
            isCrawling.set(false);
        }
    }

    /**
     * Spring Batch Job을 실행하여 실제 크롤링 로직을 수행합니다.
     */
    private void executeCrawl(String year, String semester) {
        log.info("[Crawler] Starting Spring Batch course crawl. year={}, semester={}", year, semester);
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("year", year)
                    .addString("semester", semester)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(crawlJob, jobParameters);

            if (execution.getStatus().isUnsuccessful()) {
                log.error("[Crawler] Batch crawl job failed with status: {}, exitDescription: {}",
                        execution.getStatus(), execution.getExitStatus().getExitDescription());
                throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR, "크롤링 배치 작업 실패");
            }

            log.info("[Crawler] Completed Spring Batch course crawl. year={}, semester={}", year, semester);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Crawler] Unknown error during batch crawling: {}", e.getMessage());
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }
}
