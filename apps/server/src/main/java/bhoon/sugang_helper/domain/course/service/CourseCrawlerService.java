package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.io.IOException;
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
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean isCrawling = new AtomicBoolean(false);

    /**
     * 의존성 주입을 위한 생성자
     */
    public CourseCrawlerService(CourseRepository courseRepository, JbnuCourseApiClient apiClient,
            JbnuCourseParser courseParser, ApplicationEventPublisher eventPublisher,
            CourseSeatHistoryRepository courseSeatHistoryRepository,
            PlatformTransactionManager transactionManager) {
        this.courseRepository = courseRepository;
        this.apiClient = apiClient;
        this.courseParser = courseParser;
        this.eventPublisher = eventPublisher;
        this.courseSeatHistoryRepository = courseSeatHistoryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 강의 크롤링 및 저장 수행 (중복 실행 방지 적용)
     */
    public void crawlAndSaveCourses() {
        if (!isCrawling.compareAndSet(false, true)) {
            log.warn("[크롤러] 이미 크롤링 작업이 진행 중입니다. 작업을 스킵합니다.");
            return;
        }

        log.info("[크롤러] 강의 크롤링을 시작합니다.");
        try {
            // API 호출은 트랜잭션 외부에서 수행
            String xmlResponse = apiClient.fetchCourseDataXml();
            List<Course> courses = courseParser.parseCourses(xmlResponse);

            int savedCount = processCourses(courses);

            log.info("[크롤러] 강의 크롤링을 완료했습니다. 처리 건수={}", savedCount);
        } catch (IOException e) {
            log.error("[크롤러] 강의 데이터를 가져오지 못했습니다. reason={}", e.getMessage(), e);
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR, e.getMessage());
        } finally {
            isCrawling.set(false);
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
                log.error("[크롤러] 강의 처리 중 오류 발생: courseKey={}, reason={}", course.getCourseKey(), e.getMessage());
                // 개별 강의 실패는 로그만 남기고 계속 진행
            }
        }
        return courses.size();
    }

    /**
     * 개별 강의별로 트랜잭션을 분리하여 락 경합 및 대량 데이터 처리 안정성 확보
     */
    public void processCourse(Course crawledCourse) {
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
        existingCourse.updateMetadata(crawledCourse);
        courseRepository.save(existingCourse);
        saveSeatHistory(existingCourse);

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
        log.info("[크롤러] 빈자리 발생을 감지했습니다. courseName={}, available={}", course.getName(), course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(),
                course.getName(),
                0,
                course.getAvailable()));
    }
}
