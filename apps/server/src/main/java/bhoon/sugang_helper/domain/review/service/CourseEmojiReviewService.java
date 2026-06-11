package bhoon.sugang_helper.domain.review.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.review.ReviewScopeKey;
import bhoon.sugang_helper.domain.review.entity.CourseEmojiReview;
import bhoon.sugang_helper.domain.review.repository.CourseEmojiReviewRepository;
import bhoon.sugang_helper.domain.review.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEmojiReviewService {
    private final CourseEmojiReviewRepository emojiReviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * 강의에 이모지 토글 리액션을 남기거나 취소합니다.
     */
    @Transactional
    public void toggleEmoji(String courseKey, String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이모지는 비어 있을 수 없습니다.");
        }

        Course course = getCourse(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course);
        User user = getCurrentUser();

        emojiReviewRepository.findBySubjectCodeAndProfessorAndUserIdAndEmoji(scope.subjectCode(), scope.professor(),
                        user.getId(), emoji)
                .ifPresentOrElse(
                        existing -> {
                            emojiReviewRepository.delete(existing);
                            log.info("[EmojiReview] Removed. courseKey={}, subjectCode={}, professor={}, userId={}, emoji={}",
                                    courseKey, scope.subjectCode(), scope.professor(), user.getId(), emoji);
                        },
                        () -> {
                            emojiReviewRepository.save(
                                    CourseEmojiReview.builder().courseKey(courseKey).userId(user.getId()).emoji(emoji).build()
                            );
                            log.info("[EmojiReview] Added. courseKey={}, subjectCode={}, professor={}, userId={}, emoji={}",
                                    courseKey, scope.subjectCode(), scope.professor(), user.getId(), emoji);
                        }
                );
    }

    /**
     * 강의의 이모지 통계(카운트 + 본인 탭 여부)를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<CourseEmojiReviewResponse> getCourseEmojiStats(String courseKey) {
        Course course = getCourse(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course);
        Long currentUserId = getCurrentUserIdOrNull();
        return emojiReviewRepository.findEmojiStatsBySubjectCodeAndProfessor(scope.subjectCode(), scope.professor()).stream()
                .map(row -> {
                    String emoji = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    boolean isMine = currentUserId != null &&
                            emojiReviewRepository.countBySubjectCodeAndProfessorAndUserIdAndEmoji(
                                    scope.subjectCode(), scope.professor(), currentUserId, emoji) > 0;
                    return CourseEmojiReviewResponse.builder()
                            .emoji(emoji)
                            .count(count)
                            .isMine(isMine)
                            .build();
                })
                .toList();
    }

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private Long getCurrentUserIdOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return null;
        }
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private Course getCourse(String courseKey) {
        return courseRepository.findByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다."));
    }
}
