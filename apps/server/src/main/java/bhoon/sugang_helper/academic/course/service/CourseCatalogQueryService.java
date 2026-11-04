package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseName;
import bhoon.sugang_helper.academic.course.dto.result.CourseScheduleSummary;
import bhoon.sugang_helper.academic.course.dto.result.CourseSummary;
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
public class CourseCatalogQueryService {

    private final CourseRepository courseRepository;

    public Optional<CourseSummary> findByCourseKey(String courseKey) {
        return courseRepository.findByCourseKey(courseKey).map(CourseCatalogQueryService::toSummary);
    }

    public List<CourseSummary> findByCourseKeyIn(List<String> courseKeys) {
        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .map(CourseCatalogQueryService::toSummary)
                .toList();
    }

    public List<CourseName> findNamesByCourseKeyIn(List<String> courseKeys) {
        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .map(course -> new CourseName(course.getCourseKey(), course.getName(), course.getProfessor()))
                .toList();
    }

    public boolean existsByCourseKey(String courseKey) {
        return courseRepository.existsByCourseKey(courseKey);
    }

    private static CourseSummary toSummary(Course course) {
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
                course.getSchedules().stream()
                        .map(schedule -> new CourseScheduleSummary(
                                schedule.getDayOfWeek().name(),
                                schedule.getStartTime().toString(),
                                schedule.getEndTime().toString()))
                        .toList());
    }
}
