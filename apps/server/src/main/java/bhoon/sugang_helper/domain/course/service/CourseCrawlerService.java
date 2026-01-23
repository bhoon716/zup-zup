package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
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
        log.info("Starting course crawling...");
        try {
            String xmlResponse = apiClient.fetchCourseDataXml();
            List<Course> courses = courseParser.parseCourses(xmlResponse);

            int savedCount = processCourses(courses);

            log.info("Crawling finished. Processed {} courses.", savedCount);
        } catch (IOException e) {
            log.error("Failed to fetch course data: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR, e.getMessage());
        }
    }

    private int processCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            throw new CustomException(ErrorCode.CRAWLER_NO_DATA, "No course data found.");
        }

        courses.forEach(this::processAndSaveCourse);
        return courses.size();
    }

    private void processAndSaveCourse(Course crawledCourse) {
        courseRepository.findById(crawledCourse.getCourseKey())
                .ifPresentOrElse(
                        existingCourse -> updateExistingCourse(existingCourse, crawledCourse),
                        () -> {
                            courseRepository.save(crawledCourse);
                            saveSeatHistory(crawledCourse);
                        });
    }

    private void updateExistingCourse(Course existingCourse, Course crawledCourse) {
        if (!existingCourse.getCapacity().equals(crawledCourse.getCapacity()) ||
                !existingCourse.getCurrent().equals(crawledCourse.getCurrent())) {

            boolean wasFull = existingCourse.getAvailable() <= 0;
            existingCourse.updateStatus(crawledCourse.getCapacity(), crawledCourse.getCurrent());
            courseRepository.save(existingCourse);
            saveSeatHistory(existingCourse);

            if (wasFull && existingCourse.getAvailable() > 0) {
                publishSeatOpenedEvent(existingCourse);
            }
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
        log.info("Seat Opened! Course: {}, Available: {}", course.getName(), course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(),
                course.getName(),
                0,
                course.getAvailable()));
    }
}
