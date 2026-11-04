package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.review.dto.criteria.ReviewScopeKey;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewCatalogService {

    private final CourseRepository courseRepository;

    public Optional<CourseReference> findByCourseKey(String courseKey) {
        return courseRepository.findByCourseKey(courseKey).map(CourseReviewCatalogService::toReference);
    }

    @Transactional
    public Optional<CourseReference> findByCourseKeyForUpdate(String courseKey) {
        return courseRepository.findByCourseKeyForUpdate(courseKey).map(CourseReviewCatalogService::toReference);
    }

    public List<String> findReviewCourseKeys(CourseReference reference) {
        ReviewScopeKey scope = ReviewScopeKey.from(reference.subjectCode(), reference.professor());
        if (!scope.hasIdentifiableProfessor()) {
            return List.of(reference.courseKey());
        }
        return courseRepository.findCourseKeysBySubjectCodeAndProfessor(scope.subjectCode(), scope.professor());
    }

    @Transactional
    public void updateReviewStats(List<String> courseKeys, float averageRating, int reviewCount) {
        courseRepository.findByCourseKeyIn(courseKeys)
                .forEach(course -> course.updateReviewStats(averageRating, reviewCount));
    }

    private static CourseReference toReference(Course course) {
        return new CourseReference(course.getCourseKey(), course.getSubjectCode(), course.getProfessor());
    }
}
