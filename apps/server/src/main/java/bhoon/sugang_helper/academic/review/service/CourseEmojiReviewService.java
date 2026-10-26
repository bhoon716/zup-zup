package bhoon.sugang_helper.academic.review.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.criteria.ReviewScopeKey;
import bhoon.sugang_helper.academic.review.dto.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.academic.review.entity.CourseEmojiReview;
import bhoon.sugang_helper.academic.review.repository.CourseEmojiReviewRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.result.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEmojiReviewService {
    private final CourseEmojiReviewRepository emojiReviewRepository;
    private final CourseReviewCatalogService courseReviewCatalog;
    private final UserIdentityQueryService userIdentityQuery;




    @Transactional
    public void toggleEmoji(String courseKey, String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이모지는 비어 있을 수 없습니다.");
        }

        CourseReference course = getCourse(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course.subjectCode(), course.professor());
        UserIdentity user = getCurrentUser();

        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);
        emojiReviewRepository.findByCourseKeysAndUserIdAndEmoji(courseKeys, user.id(), emoji)
                .ifPresentOrElse(
                        existing -> {
                            emojiReviewRepository.delete(existing);
                            log.debug(
                                    "[EmojiReview] 이모지 반응 취소. courseKey={}, subjectCode={}, userId={}",
                                    courseKey, scope.subjectCode(), user.id());
                        },
                        () -> {
                            emojiReviewRepository.save(
                                    CourseEmojiReview.builder().courseKey(courseKey).userId(user.id()).emoji(emoji)
                                            .build()
                            );
                            log.debug(
                                    "[EmojiReview] 이모지 반응 등록. courseKey={}, subjectCode={}, userId={}",
                                    courseKey, scope.subjectCode(), user.id());
                        }
                );
    }




    @Transactional(readOnly = true)
    public List<CourseEmojiReviewResponse> getCourseEmojiStats(String courseKey) {
        CourseReference course = getCourse(courseKey);
        Long currentUserId = getCurrentUserIdOrNull();
        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);
        return emojiReviewRepository.findEmojiStatsByCourseKeys(courseKeys)
                .stream()
                .map(row -> {
                    String emoji = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    boolean isMine = currentUserId != null
                            && emojiReviewRepository.countByCourseKeysAndUserIdAndEmoji(
                                    courseKeys, currentUserId, emoji) > 0;
                    return CourseEmojiReviewResponse.builder()
                            .emoji(emoji)
                            .count(count)
                            .isMine(isMine)
                            .build();
                })
                .toList();
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
