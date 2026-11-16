package bhoon.sugang_helper.academic.review.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.criteria.ReviewScopeKey;
import bhoon.sugang_helper.academic.review.dto.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.academic.review.entity.CourseEmojiReview;
import bhoon.sugang_helper.academic.review.repository.CourseEmojiReviewRepository;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEmojiReviewService {

    private static final String COURSE_KEY = "courseKey";
    private static final String EVENT_KEY = "event";
    private static final String SUBJECT_CODE_KEY = "subjectCode";
    private static final String USER_ID_KEY = "userId";

    private final CourseEmojiReviewRepository emojiReviewRepository;
    private final CourseReviewCatalogService courseReviewCatalog;
    private final CurrentUserQuery currentUserQuery;

    @Transactional
    public void toggleEmoji(String courseKey, String emoji) {
        if (!StringUtils.hasText(emoji)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이모지는 비어 있을 수 없습니다.");
        }

        CourseReference course = getCourse(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course.subjectCode(), course.professor());
        UserIdentity user = currentUserQuery.requireCurrentUser();

        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);
        emojiReviewRepository.findByCourseKeysAndUserIdAndEmoji(courseKeys, user.id(), emoji)
                .ifPresentOrElse(
                        existing -> removeReaction(existing, courseKey, scope.subjectCode(), user.id()),
                        () -> addReaction(courseKey, scope.subjectCode(), user.id(), emoji)
                );
    }

    @Transactional(readOnly = true)
    public List<CourseEmojiReviewResponse> getCourseEmojiStats(String courseKey) {
        CourseReference course = getCourse(courseKey);
        Long currentUserId = currentUserQuery.findCurrentUser().map(UserIdentity::id).orElse(null);
        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);
        return emojiReviewRepository.findEmojiStatsByCourseKeys(courseKeys, currentUserId)
                .stream()
                .map(CourseEmojiReviewResponse::from)
                .toList();
    }

    private void removeReaction(CourseEmojiReview existing, String courseKey, String subjectCode, Long userId) {
        emojiReviewRepository.delete(existing);
        logCourseEmojiReactionRemoved(courseKey, subjectCode, userId);
    }

    private void addReaction(String courseKey, String subjectCode, Long userId, String emoji) {
        emojiReviewRepository.save(CourseEmojiReview.builder().courseKey(courseKey).userId(userId).emoji(emoji).build());
        logCourseEmojiReactionAdded(courseKey, subjectCode, userId);
    }

    private CourseReference getCourse(String courseKey) {
        return courseReviewCatalog.findByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다."));
    }

    private void logCourseEmojiReactionRemoved(String courseKey, String subjectCode, Long userId) {
        log.atDebug().addKeyValue(EVENT_KEY, "course_emoji_reaction_removed")
                .addKeyValue(COURSE_KEY, courseKey)
                .addKeyValue(SUBJECT_CODE_KEY, subjectCode)
                .addKeyValue(USER_ID_KEY, userId)
                .log("[EmojiReview] 이모지 반응 취소.");
    }

    private void logCourseEmojiReactionAdded(String courseKey, String subjectCode, Long userId) {
        log.atDebug().addKeyValue(EVENT_KEY, "course_emoji_reaction_added")
                .addKeyValue(COURSE_KEY, courseKey)
                .addKeyValue(SUBJECT_CODE_KEY, subjectCode)
                .addKeyValue(USER_ID_KEY, userId)
                .log("[EmojiReview] 이모지 반응 등록.");
    }
}
