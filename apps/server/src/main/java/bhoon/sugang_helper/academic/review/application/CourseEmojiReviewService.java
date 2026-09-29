package bhoon.sugang_helper.academic.review.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.academic.course.contract.CourseReference;
import bhoon.sugang_helper.academic.course.contract.CourseReviewCatalog;
import bhoon.sugang_helper.academic.review.domain.CourseEmojiReview;
import bhoon.sugang_helper.academic.review.domain.CourseEmojiReviewRepository;
import bhoon.sugang_helper.academic.review.domain.ReviewScopeKey;
import bhoon.sugang_helper.identity.user.contract.UserIdentity;
import bhoon.sugang_helper.identity.user.contract.UserIdentityQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEmojiReviewService {
    private final CourseEmojiReviewRepository emojiReviewRepository;
    private final CourseReviewCatalog courseReviewCatalog;
    private final UserIdentityQuery userIdentityQuery;

    /**
     * 강의에 이모지 토글 리액션을 남기거나 취소합니다.
     */
    @Transactional
    public void toggleEmoji(String courseKey, String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이모지는 비어 있을 수 없습니다.");
        }

        CourseReference course = getCourse(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course.subjectCode(), course.professor());
        UserIdentity user = getCurrentUser();

        findEmojiReview(course, scope, user.id(), emoji)
                .ifPresentOrElse(
                        existing -> {
                            emojiReviewRepository.delete(existing);
                            log.info(
                                    "[EmojiReview] Removed. courseKey={}, subjectCode={}, professor={}, userId={}, emoji={}",
                                    courseKey, scope.subjectCode(), scope.professor(), user.id(), emoji);
                        },
                        () -> {
                            emojiReviewRepository.save(
                                    CourseEmojiReview.builder().courseKey(courseKey).userId(user.id()).emoji(emoji)
                                            .build()
                            );
                            log.info(
                                    "[EmojiReview] Added. courseKey={}, subjectCode={}, professor={}, userId={}, emoji={}",
                                    courseKey, scope.subjectCode(), scope.professor(), user.id(), emoji);
                        }
                );
    }

    /**
     * 강의의 이모지 통계(카운트 + 본인 탭 여부)를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<CourseEmojiReviewResponse> getCourseEmojiStats(String courseKey) {
        CourseReference course = getCourse(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course.subjectCode(), course.professor());
        Long currentUserId = getCurrentUserIdOrNull();
        List<String> courseKeys = findCourseKeysForScope(course, scope);
        return findEmojiStats(course, scope, courseKeys)
                .stream()
                .map(row -> {
                    String emoji = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    boolean isMine = currentUserId != null
                            && hasEmojiReview(course, scope, courseKeys, currentUserId, emoji);
                    return CourseEmojiReviewResponse.builder()
                            .emoji(emoji)
                            .count(count)
                            .isMine(isMine)
                            .build();
                })
                .toList();
    }

    private Optional<CourseEmojiReview> findEmojiReview(CourseReference course, ReviewScopeKey scope, Long userId,
                                                         String emoji) {
        if (!scope.hasIdentifiableProfessor()) {
            return emojiReviewRepository.findByCourseKeyAndUserIdAndEmoji(course.courseKey(), userId, emoji);
        }

        var courseKeys = findCourseKeysForScope(course, scope);
        return emojiReviewRepository.findByCourseKeysAndUserIdAndEmoji(courseKeys, userId, emoji);
    }

    private List<Object[]> findEmojiStats(CourseReference course, ReviewScopeKey scope, List<String> courseKeys) {
        if (!scope.hasIdentifiableProfessor()) {
            return emojiReviewRepository.findEmojiStatsByCourseKey(course.courseKey());
        }
        return emojiReviewRepository.findEmojiStatsByCourseKeys(courseKeys);
    }

    private boolean hasEmojiReview(CourseReference course, ReviewScopeKey scope, List<String> courseKeys,
                                   Long userId, String emoji) {
        if (!scope.hasIdentifiableProfessor()) {
            return emojiReviewRepository.existsByCourseKeyAndUserIdAndEmoji(course.courseKey(), userId, emoji);
        }

        return emojiReviewRepository.countByCourseKeysAndUserIdAndEmoji(courseKeys, userId, emoji) > 0;
    }

    private List<String> findCourseKeysForScope(CourseReference course, ReviewScopeKey scope) {
        if (!scope.hasIdentifiableProfessor()) {
            return List.of();
        }
        return courseReviewCatalog.findCourseKeysBySubjectCodeAndProfessor(scope.subjectCode(), scope.professor());
    }

    private UserIdentity getCurrentUser() {
        return userIdentityQuery.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private Long getCurrentUserIdOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return null;
        }
        return userIdentityQuery.findByEmail(email).map(UserIdentity::id).orElse(null);
    }

    private CourseReference getCourse(String courseKey) {
        return courseReviewCatalog.findByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다."));
    }
}
