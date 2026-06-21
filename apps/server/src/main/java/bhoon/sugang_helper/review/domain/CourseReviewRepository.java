package bhoon.sugang_helper.review.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseReviewRepository extends RepositoryContract<CourseReview, Long> {
    Optional<CourseReview> findByCourseKeyAndUserId(String courseKey, Long userId);

    Optional<CourseReview> findBySubjectCodeAndProfessorAndUserId(String subjectCode,
                                                                  String professor,
                                                                  Long userId);

    Page<CourseReview> findByCourseKey(String courseKey, Pageable pageable);

    Page<CourseReview> findBySubjectCodeAndProfessorWithMyReviewFirst(String subjectCode,
                                                                      String professor,
                                                                      Long userId,
                                                                      Pageable pageable);

    Page<CourseReview> findByCourseKeyWithMyReviewFirst(String courseKey,
                                                        Long userId,
                                                        Pageable pageable);

    boolean existsByCourseKeyAndUserId(String courseKey, Long userId);

    long countBySubjectCodeAndProfessorAndUserId(String subjectCode,
                                                 String professor,
                                                 Long userId);

    long countByCourseKey(String courseKey);

    long countBySubjectCodeAndProfessor(String subjectCode,
                                        String professor);

    Double getAverageRatingByCourseKey(String courseKey);

    Double getAverageRatingBySubjectCodeAndProfessor(String subjectCode,
                                                     String professor);
}
