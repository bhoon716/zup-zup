package bhoon.sugang_helper.domain.review.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.domain.review.dto.request.ReviewReactionRequest;
import bhoon.sugang_helper.domain.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.domain.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.domain.review.entity.CourseReview;
import bhoon.sugang_helper.domain.review.entity.CourseReviewReaction;
import bhoon.sugang_helper.domain.review.enums.ReactionType;
import bhoon.sugang_helper.domain.review.repository.CourseReviewReactionRepository;
import bhoon.sugang_helper.domain.review.repository.CourseReviewRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseReviewReactionRepository reactionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * 현재 로그인한 사용자를 조회합니다.
     */
    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    /**
     * 현재 로그인한 사용자의 ID를 조회합니다. 로그인하지 않은 경우 null을 반환합니다.
     */
    private Long getCurrentUserIdOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return null;
        }
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

    /**
     * 강의 리뷰를 작성합니다.
     */
    @Transactional
    public ReviewResponse createReview(String courseKey, ReviewCreateRequest request) {
        User user = getCurrentUser();

        if (!courseRepository.existsByCourseKey(courseKey)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다.");
        }

        if (reviewRepository.existsByCourseKeyAndUserId(courseKey, user.getId())) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        CourseReview review = CourseReview.builder()
                .courseKey(courseKey)
                .userId(user.getId())
                .rating(request.rating())
                .content(request.content())
                .build();

        CourseReview saved = reviewRepository.saveAndFlush(review);
        log.info("[Review] Created. reviewId={}, courseKey={}, userId={}", saved.getId(), courseKey,
                user.getId());

        updateCourseReviewStats(courseKey);

        return ReviewResponse.of(saved, user.getId());
    }

    /**
     * 특정 강의의 리뷰 목록을 조회합니다. 현재 로그인한 사용자의 리뷰가 있다면 가장 상단에 위치합니다.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(String courseKey, Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        return reviewRepository.findByCourseKeyWithMyReviewFirst(courseKey, currentUserId, pageable)
                .map(review -> ReviewResponse.of(review, currentUserId));
    }

    /**
     * 자신이 작성한 리뷰를 수정합니다.
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        User user = getCurrentUser();

        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        validateReviewOwner(review, user);

        review.update(request.rating(), request.content());
        reviewRepository.saveAndFlush(review);
        log.info("[Review] Updated. reviewId={}, userId={}", reviewId, user.getId());

        updateCourseReviewStats(review.getCourseKey());

        return ReviewResponse.of(review, user.getId());
    }

    /**
     * 자신이 작성한 리뷰를 삭제합니다.
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = getCurrentUser();

        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        validateReviewOwner(review, user);

        String courseKey = review.getCourseKey();
        reviewRepository.delete(review);
        reviewRepository.flush();
        log.info("[Review] Deleted. reviewId={}, userId={}", reviewId, user.getId());

        updateCourseReviewStats(courseKey);
    }

    /**
     * 리뷰 작성자 본인인지 확인합니다. (ADMIN 계정은 통과)
     */
    private void validateReviewOwner(CourseReview review, User user) {
        if (!review.getUserId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.REVIEW_UNAUTHORIZED);
        }
    }

    /**
     * 리뷰에 공감/비공감 반응을 남기거나 취소/변경합니다.
     */
    @Transactional
    public void toggleReaction(Long reviewId, ReviewReactionRequest request) {
        User user = getCurrentUser();
        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        ReactionType targetReaction = request.reactionType();
        reactionRepository.findByReviewAndUserId(review, user.getId())
                .ifPresentOrElse(
                        existing -> handleExistingReaction(review, existing, targetReaction, user.getId()),
                        () -> handleNewReaction(review, targetReaction, user.getId()));
    }

    /**
     * 새로운 반응을 추가합니다.
     */
    private void handleNewReaction(CourseReview review, ReactionType type, Long userId) {
        CourseReviewReaction newReaction = CourseReviewReaction.builder()
                .review(review)
                .userId(userId)
                .reactionType(type)
                .build();
        reactionRepository.save(newReaction);
        incrementReactionCount(review, type);

        log.info("[Review Reaction] Added. reviewId={}, userId={}, type={}", review.getId(), userId, type);
    }

    /**
     * 기존 반응을 취소하거나 변경합니다.
     */
    private void handleExistingReaction(CourseReview review, CourseReviewReaction existing, ReactionType targetType,
            Long userId) {
        if (existing.getReactionType() == targetType) {
            reactionRepository.delete(existing);
            decrementReactionCount(review, targetType);
            log.info("[Review Reaction] Removed. reviewId={}, userId={}, type={}", review.getId(), userId, targetType);
        } else {
            ReactionType oldType = existing.getReactionType();
            existing.updateReactionType(targetType);

            decrementReactionCount(review, oldType);
            incrementReactionCount(review, targetType);
            log.info("[Review Reaction] Switched. reviewId={}, userId={}, oldType={}, newType={}",
                    review.getId(), userId, oldType, targetType);
        }
    }

    /**
     * 반응 타입에 따라 리뷰의 공감/비공감 카운트를 증가시킵니다.
     */
    private void incrementReactionCount(CourseReview review, ReactionType type) {
        if (type == ReactionType.LIKE)
            review.increaseLikeCount();
        else
            review.increaseDislikeCount();
    }

    /**
     * 반응 타입에 따라 리뷰의 공감/비공감 카운트를 감소시킵니다.
     */
    private void decrementReactionCount(CourseReview review, ReactionType type) {
        if (type == ReactionType.LIKE)
            review.decreaseLikeCount();
        else
            review.decreaseDislikeCount();
    }

    /**
     * 특정 강의의 전체 리뷰 통계(평균 별점, 리뷰 수)를 업데이트합니다.
     */
    private void updateCourseReviewStats(String courseKey) {
        courseRepository.findByCourseKey(courseKey).ifPresent(course -> {
            long count = reviewRepository.countByCourseKey(courseKey);
            Double avg = reviewRepository.getAverageRatingByCourseKey(courseKey);
            course.updateReviewStats(avg != null ? avg.floatValue() : 0.0f, (int) count);
        });
    }
}
