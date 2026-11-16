package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.academic.api.catalog.CourseName;
import bhoon.sugang_helper.academic.api.catalog.CourseScheduleSummary;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCatalogQueryService implements CourseCatalog {

    private final CourseRepository courseRepository;

    public Optional<CourseSummary> findWithoutSchedulesByCourseKey(String courseKey) {
        return courseRepository.findByCourseKey(courseKey).map(course -> toSummary(course, List.of()));
    }

    public List<CourseSummary> findByCourseKeyIn(List<String> courseKeys) {
        return courseRepository.findWithSchedulesByCourseKeyIn(courseKeys).stream()
                .map(CourseCatalogQueryService::toSummary)
                .toList();
    }

    public List<CourseSummary> findWithoutSchedulesByCourseKeyIn(List<String> courseKeys) {
        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .map(course -> toSummary(course, List.of()))
                .toList();
    }

    public List<CourseName> findNamesByCourseKeyIn(List<String> courseKeys) {
        return courseRepository.findNamesByCourseKeyIn(courseKeys);
    }

    public boolean existsByCourseKey(String courseKey) {
        return courseRepository.existsByCourseKey(courseKey);
    }

    private static CourseSummary toSummary(Course course) {
        List<CourseScheduleSummary> schedules = course.getSchedules().stream()
                .map(schedule -> CourseScheduleSummary.of(
                        schedule.getDayOfWeek().name(),
                        schedule.getStartTime(),
                        schedule.getEndTime()))
                .toList();
        return toSummary(course, schedules);
    }

    private static CourseSummary toSummary(Course course, List<CourseScheduleSummary> schedules) {
        return new CourseSummary(
                course.getCourseKey(),
                course.getSubjectCode(),
                course.getClassNumber(),
                course.getName(),
                course.getProfessor(),
                course.getCapacity(),
                course.getCurrent(),
                course.getAcademicYear(),
                course.getSemester(),
                course.getClassification() == null ? null : course.getClassification().getDescription(),
                course.getCredits(),
                course.getClassTime(),
                course.getClassroom(),
                schedules);
    }
}
