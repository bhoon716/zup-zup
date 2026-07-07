package bhoon.sugang_helper.review.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;

import java.util.List;
import java.util.Optional;

public interface CourseEmojiReviewRepository extends RepositoryContract<CourseEmojiReview, Long> {
    Optional<CourseEmojiReview> findByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);
    List<CourseEmojiReview> findByCourseKey(String courseKey);
    long countByCourseKeyAndEmoji(String courseKey, String emoji);
    boolean existsByCourseKeyAndUserIdAndEmoji(String courseKey, Long userId, String emoji);

    void deleteAllByUserId(Long userId);

    Optional<CourseEmojiReview> findBySubjectCodeAndProfessorAndUserIdAndEmoji(String subjectCode,
                                                                                String professor,
                                                                                Long userId,
                                                                                String emoji);

    long countBySubjectCodeAndProfessorAndEmoji(String subjectCode,
                                                String professor,
                                                String emoji);

    long countBySubjectCodeAndProfessorAndUserIdAndEmoji(String subjectCode,
                                                          String professor,
                                                          Long userId,
                                                          String emoji);

    List<Object[]> findEmojiStatsByCourseKey(String courseKey);

    List<Object[]> findEmojiStatsBySubjectCodeAndProfessor(String subjectCode,
                                                           String professor);
}
