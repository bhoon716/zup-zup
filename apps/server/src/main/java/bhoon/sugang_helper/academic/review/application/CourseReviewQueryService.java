package bhoon.sugang_helper.academic.review.application;

import bhoon.sugang_helper.academic.course.contract.CourseReviewQuery;
import bhoon.sugang_helper.academic.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.academic.review.domain.ReviewScopeKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewQueryService implements CourseReviewQuery {

    private final CourseReviewRepository reviewRepository;

    @Override
    public boolean hasReviewByUser(String courseKey, String subjectCode, String professor, Long userId) {
        ReviewScopeKey scope = ReviewScopeKey.from(subjectCode, professor);
        if (!scope.hasIdentifiableProfessor()) {
            return reviewRepository.existsByCourseKeyAndUserId(courseKey, userId);
        }
        return reviewRepository.countBySubjectCodeAndProfessorAndUserId(
                scope.subjectCode(), scope.professor(), userId) > 0;
    }
}
