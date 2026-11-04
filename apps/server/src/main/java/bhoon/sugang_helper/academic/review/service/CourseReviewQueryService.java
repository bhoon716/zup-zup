package bhoon.sugang_helper.academic.review.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.repository.CourseReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewQueryService {

    private final CourseReviewRepository reviewRepository;
    private final CourseReviewCatalogService courseReviewCatalog;

    public boolean hasReviewByUser(String courseKey, String subjectCode, String professor, Long userId) {
        var courseKeys = courseReviewCatalog.findReviewCourseKeys(new CourseReference(courseKey, subjectCode, professor));
        return reviewRepository.countByCourseKeysAndUserId(courseKeys, userId) > 0;
    }
}
