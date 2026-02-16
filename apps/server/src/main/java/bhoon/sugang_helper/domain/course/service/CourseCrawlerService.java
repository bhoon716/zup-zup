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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseCrawlerService {

    private final CourseRepository courseRepository;
    private final JbnuCourseApiClient apiClient;
    private final JbnuCourseParser courseParser;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseSeatHistoryRepository courseSeatHistoryRepository;

    @Transactional
    public void crawlAndSaveCourses() {
        log.info("[크롤러] 강의 크롤링을 시작합니다.");
        try {
            String xmlResponse = apiClient.fetchCourseDataXml();
            List<Course> courses = courseParser.parseCourses(xmlResponse);

            int savedCount = processCourses(courses);

            log.info("[크롤러] 강의 크롤링을 완료했습니다. 처리 건수={}", savedCount);
        } catch (IOException e) {
            log.error("[크롤러] 강의 데이터를 가져오지 못했습니다. reason={}", e.getMessage(), e);
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR, e.getMessage());
        }
    }

    private int processCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            throw new CustomException(ErrorCode.CRAWLER_NO_DATA, "강의 데이터가 비어 있습니다.");
        }

        courses.forEach(this::processCourse);
        return courses.size();
    }

    private void processCourse(Course crawledCourse) {
        courseRepository.findByCourseKey(crawledCourse.getCourseKey())
                .ifPresentOrElse(
                        existingCourse -> updateExistingCourse(existingCourse, crawledCourse),
                        () -> createNewCourse(crawledCourse));
    }

    private void createNewCourse(Course course) {
        courseRepository.save(course);
        saveSeatHistory(course);
    }

    private void updateExistingCourse(Course existingCourse, Course crawledCourse) {
        boolean wasFull = existingCourse.getAvailable() <= 0;
        existingCourse.updateMetadata(crawledCourse);
        courseRepository.save(existingCourse);
        saveSeatHistory(existingCourse);

        if (wasFull && existingCourse.getAvailable() > 0) {
            publishSeatOpenedEvent(existingCourse);
        }
    }

    private void saveSeatHistory(Course course) {
        courseSeatHistoryRepository.save(CourseSeatHistory.builder()
                .courseKey(course.getCourseKey())
                .capacity(course.getCapacity())
                .current(course.getCurrent())
                .build());
    }

    private void publishSeatOpenedEvent(Course course) {
        log.info("[크롤러] 빈자리 발생을 감지했습니다. courseName={}, available={}", course.getName(), course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(),
                course.getName(),
                0,
                course.getAvailable()));
    }
}
