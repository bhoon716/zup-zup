package bhoon.sugang_helper.academic.course.application;

import bhoon.sugang_helper.academic.course.contract.CourseReference;
import bhoon.sugang_helper.academic.course.contract.CourseReviewCatalog;
import bhoon.sugang_helper.academic.course.domain.Course;
import bhoon.sugang_helper.academic.course.domain.CourseRepository;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewCatalogService implements CourseReviewCatalog {

    private final CourseRepository courseRepository;

    @Override
    public Optional<CourseReference> findByCourseKey(String courseKey) {
        return courseRepository.findByCourseKey(courseKey).map(CourseReviewCatalogService::toReference);
    }

    @Override
    @Transactional
    public Optional<CourseReference> findByCourseKeyForUpdate(String courseKey) {
        return courseRepository.findByCourseKeyForUpdate(courseKey).map(CourseReviewCatalogService::toReference);
    }

    @Override
    public List<String> findCourseKeysBySubjectCodeAndProfessor(String subjectCode, String professor) {
        return courseRepository.findCourseKeysBySubjectCodeAndProfessor(subjectCode, professor);
    }

    @Override
    @Transactional
    public void updateReviewStats(CourseReference reference, float averageRating, int reviewCount) {
        String professor = normalizeProfessor(reference.professor());
        if (professor.isEmpty()) {
            courseRepository.findByCourseKey(reference.courseKey())
                    .ifPresent(course -> course.updateReviewStats(averageRating, reviewCount));
            return;
        }

        courseRepository.findBySubjectCodeAndProfessor(reference.subjectCode(), professor)
                .forEach(course -> course.updateReviewStats(averageRating, reviewCount));
    }

    private static CourseReference toReference(Course course) {
        return new CourseReference(course.getCourseKey(), course.getSubjectCode(), course.getProfessor());
    }

    private String normalizeProfessor(String professor) {
        String normalized = professor == null ? "" : professor.trim();
        return "교수 미지정".equals(normalized) ? "" : normalized;
    }
}
