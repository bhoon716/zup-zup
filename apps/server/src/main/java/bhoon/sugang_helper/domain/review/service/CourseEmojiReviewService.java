package bhoon.sugang_helper.domain.review.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
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

    /** 시스템 고정 6종 이모지 (커스텀 불가) */
    static final List<String> SUPPORTED_EMOJIS =
            List.of("👍", "🔥", "🎓", "📝", "😴", "🚨");

    private final CourseEmojiReviewRepository emojiReviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * 강의에 이모지 토글 리액션을 남기거나 취소합니다.
     */
    @Transactional
    public void toggleEmoji(String courseKey, String emoji) {
        if (!SUPPORTED_EMOJIS.contains(emoji)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "지원하지 않는 이모지입니다.");
        }
        if (!courseRepository.existsByCourseKey(courseKey)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다.");
        }

        User user = getCurrentUser();

        emojiReviewRepository.findByCourseKeyAndUserIdAndEmoji(courseKey, user.getId(), emoji)
                .ifPresentOrElse(
                        existing -> {
                            emojiReviewRepository.delete(existing);
                            log.info("[EmojiReview] Removed. courseKey={}, userId={}, emoji={}", courseKey, user.getId(), emoji);
                        },
                        () -> {
                            emojiReviewRepository.save(
                                    CourseEmojiReview.builder().courseKey(courseKey).userId(user.getId()).emoji(emoji).build()
                            );
                            log.info("[EmojiReview] Added. courseKey={}, userId={}, emoji={}", courseKey, user.getId(), emoji);
                        }
                );
    }

    /**
     * 강의의 6종 이모지 통계(카운트 + 본인 탭 여부)를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<CourseEmojiReviewResponse> getCourseEmojiStats(String courseKey) {
        Long currentUserId = getCurrentUserIdOrNull();
        return SUPPORTED_EMOJIS.stream()
                .map(emoji -> CourseEmojiReviewResponse.builder()
                        .emoji(emoji)
                        .count(emojiReviewRepository.countByCourseKeyAndEmoji(courseKey, emoji))
                        .isMine(currentUserId != null &&
                                emojiReviewRepository.existsByCourseKeyAndUserIdAndEmoji(courseKey, currentUserId, emoji))
                        .build())
                .toList();
    }

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private Long getCurrentUserIdOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
