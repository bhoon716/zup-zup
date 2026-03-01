package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.enums.SemesterType;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class CourseCrawlerService {

    private final CourseRepository courseRepository;
    private final JbnuCourseApiClient apiClient;
    private final JbnuCourseParser courseParser;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseSeatHistoryRepository courseSeatHistoryRepository;
    private final CourseCrawlerTargetService crawlerTargetService;
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean isCrawling = new AtomicBoolean(false);

    /**
     * 의존성 주입을 위한 생성자
     */
    public CourseCrawlerService(CourseRepository courseRepository, JbnuCourseApiClient apiClient,
            JbnuCourseParser courseParser, ApplicationEventPublisher eventPublisher,
            CourseSeatHistoryRepository courseSeatHistoryRepository,
            CourseCrawlerTargetService crawlerTargetService,
            PlatformTransactionManager transactionManager) {
        this.courseRepository = courseRepository;
        this.apiClient = apiClient;
        this.courseParser = courseParser;
        this.eventPublisher = eventPublisher;
        this.courseSeatHistoryRepository = courseSeatHistoryRepository;
        this.crawlerTargetService = crawlerTargetService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 강의 크롤링 및 저장 수행 (중복 실행 방지 적용)
     */
    public void crawlAndSaveCourses() {
        CourseCrawlerTargetService.CrawlTarget target = crawlerTargetService.getCurrentTargetValue();
        crawlAndSaveCourses(target.year(), target.semester());
    }

    /**
     * 특정 년도와 학기를 지정하여 강의 크롤링 및 저장을 수행합니다.
     */
    public void crawlAndSaveCourses(String year, String semester) {
        if (!isCrawling.compareAndSet(false, true)) {
            log.warn("[Crawler] Crawling already in progress. Skipping.");
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
            log.warn("[Crawler] Crawling already in progress. Skipping.");
            return;
        }

        try {
            int currentYear = java.time.Year.now().getValue();
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
     * 실제 크롤링 로직을 실행합니다. (중복 체크 및 Lock 관리는 호출부에서 담당)
     */
    private void executeCrawl(String year, String semester) {
        log.info("[Crawler] Starting course crawl. year={}, semester={}", year, semester);
        try {
            // API 호출은 트랜잭션 외부에서 수행
            String xmlResponse = apiClient.fetchCourseDataXml(year, semester);
            List<Course> courses = courseParser.parseCourses(xmlResponse);

            int savedCount = processCourses(courses);

            log.info("[Crawler] Completed course crawl. year={}, semester={}, count={}",
                    year, semester, savedCount);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Crawler] Unknown error during crawling: {}", e.getMessage());
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }

    /**
     * 파싱된 강의 목록을 순회하며 개별 강의 처리 프로세스 실행
     */
    private int processCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            throw new CustomException(ErrorCode.CRAWLER_NO_DATA, "강의 데이터가 비어 있습니다.");
        }

        for (Course course : courses) {
            try {
                // 개별 강의별로 트랜잭션을 분리하여 안정성 확보
                transactionTemplate.executeWithoutResult(status -> processCourse(course));
            } catch (Exception e) {
                log.error("[Crawler] Error processing course: courseKey={}, reason={}", course.getCourseKey(),
                        e.getMessage());
                // 개별 강의 실패는 로그만 남기고 계속 진행
            }
        }
        return courses.size();
    }

    /**
     * 개별 강의별로 트랜잭션을 분리하여 락 경합 및 대량 데이터 처리 안정성 확보
     */
    private void processCourse(Course crawledCourse) {
        courseRepository.findByCourseKey(crawledCourse.getCourseKey())
                .ifPresentOrElse(
                        existingCourse -> updateExistingCourse(existingCourse, crawledCourse),
                        () -> createNewCourse(crawledCourse));
    }

    /**
     * 신규 강의 정보를 데이터베이스에 저장하고 이력 기록
     */
    private void createNewCourse(Course course) {
        courseRepository.save(course);
        saveSeatHistory(course);
    }

    /**
     * 기존 강의 정보를 업데이트하고, 빈자리 발생 시 알림 이벤트 발행
     */
    private void updateExistingCourse(Course existingCourse, Course crawledCourse) {
        boolean wasFull = existingCourse.getAvailable() <= 0;
        // 정원 또는 현재 인원에 변동이 있는지 확인
        boolean seatsChanged = !existingCourse.getCapacity().equals(crawledCourse.getCapacity()) ||
                !existingCourse.getCurrent().equals(crawledCourse.getCurrent());

        existingCourse.updateMetadata(crawledCourse);
        courseRepository.save(existingCourse);

        // 변경 사항이 있을 때만 이력 저장 (저장 공간 최적화)
        if (seatsChanged) {
            saveSeatHistory(existingCourse);
        }

        if (wasFull && existingCourse.getAvailable() > 0) {
            publishSeatOpenedEvent(existingCourse);
        }
    }

    /**
     * 강의의 현재 수강 인원 상태를 이력 테이블에 저장
     */
    private void saveSeatHistory(Course course) {
        courseSeatHistoryRepository.save(CourseSeatHistory.builder()
                .courseKey(course.getCourseKey())
                .capacity(course.getCapacity())
                .current(course.getCurrent())
                .build());
    }

    /**
     * 빈자리 발생 알림 이벤트를 시스템에 발행
     */
    private void publishSeatOpenedEvent(Course course) {
        log.info("[Crawler] Seat opening detected. courseName={}, available={}", course.getName(),
                course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(),
                course.getName(),
                0,
                course.getAvailable()));
    }
}
