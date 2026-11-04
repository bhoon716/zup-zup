package bhoon.sugang_helper.academic.review.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.criteria.ReviewScopeKey;
import bhoon.sugang_helper.academic.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewReactionRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.academic.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.academic.review.entity.CourseReview;
import bhoon.sugang_helper.academic.review.entity.CourseReviewReaction;
import bhoon.sugang_helper.academic.review.enums.ReactionType;
import bhoon.sugang_helper.academic.review.repository.CourseReviewReactionRepository;
import bhoon.sugang_helper.academic.review.repository.CourseReviewRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.identity.user.dto.result.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import java.util.List;
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
    private final CourseReviewCatalogService courseReviewCatalog;
    private final UserIdentityQueryService userIdentityQuery;




    private UserIdentity getCurrentUser() {
        return userIdentityQuery.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }




    private Long getCurrentUserIdOrNull() {
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email == null) {
            return null;
        }
        return userIdentityQuery.findByEmail(email)
                .map(UserIdentity::id)
                .orElse(null);
    }




    @Transactional
    public ReviewResponse createReview(String courseKey, ReviewCreateRequest request) {
        UserIdentity user = getCurrentUser();

        CourseReference course = getCourseForUpdate(courseKey);
        ReviewScopeKey scope = ReviewScopeKey.from(course.subjectCode(), course.professor());
        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);

        if (reviewRepository.countByCourseKeysAndUserId(courseKeys, user.id()) > 0) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        CourseReview review = CourseReview.builder()
                .courseKey(courseKey)
                .userId(user.id())
                .rating(request.rating())
                .build();

        CourseReview saved = reviewRepository.saveAndFlush(review);
        log.info("[Review] 리뷰 등록. reviewId={}, courseKey={}, subjectCode={}, userId={}",
                saved.getId(), courseKey, scope.subjectCode(), user.id());

        updateCourseReviewStats(courseKeys);

        return ReviewResponse.of(saved, user.id());
    }




    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(String courseKey, Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        CourseReference course = getCourse(courseKey);
        Pageable boundedPageable = PageableGuard.requireBounded(pageable);
        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);
        return reviewRepository.findByCourseKeysWithMyReviewFirst(courseKeys, currentUserId, boundedPageable)
                .map(review -> ReviewResponse.of(review, currentUserId));
    }




    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        UserIdentity user = getCurrentUser();

        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        validateReviewOwner(review, user);

        CourseReference course = getCourseForUpdate(review.getCourseKey());
        review.update(request.rating());
        reviewRepository.saveAndFlush(review);
        log.info("[Review] 리뷰 수정. reviewId={}, userId={}", reviewId, user.id());

        updateCourseReviewStats(courseReviewCatalog.findReviewCourseKeys(course));

        return ReviewResponse.of(review, user.id());
    }




    @Transactional
    public void deleteReview(Long reviewId) {
        UserIdentity user = getCurrentUser();

        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        validateReviewOwner(review, user);

        CourseReference course = getCourseForUpdate(review.getCourseKey());
        reviewRepository.delete(review);
        reviewRepository.flush();
        log.info("[Review] 리뷰 삭제. reviewId={}, userId={}", reviewId, user.id());

        updateCourseReviewStats(courseReviewCatalog.findReviewCourseKeys(course));
    }




    private void validateReviewOwner(CourseReview review, UserIdentity user) {
        if (!review.getUserId().equals(user.id()) && !user.administrator()) {
            throw new CustomException(ErrorCode.REVIEW_UNAUTHORIZED);
        }
    }




    @Transactional
    public void toggleReaction(Long reviewId, ReviewReactionRequest request) {
        UserIdentity user = getCurrentUser();
        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        ReactionType targetReaction = request.reactionType();
        reactionRepository.findByReviewAndUserId(review, user.id())
                .ifPresentOrElse(
                        existing -> handleExistingReaction(review, existing, targetReaction, user.id()),
                        () -> handleNewReaction(review, targetReaction, user.id()));
    }




    private void handleNewReaction(CourseReview review, ReactionType type, Long userId) {
        CourseReviewReaction newReaction = CourseReviewReaction.builder()
                .review(review)
                .userId(userId)
                .reactionType(type)
                .build();
        reactionRepository.save(newReaction);
        incrementReactionCount(review, type);

        log.debug("[Review Reaction] 리뷰 반응 등록. reviewId={}, userId={}, type={}", review.getId(), userId, type);
    }




    private void handleExistingReaction(CourseReview review, CourseReviewReaction existing, ReactionType targetType,
                                        Long userId) {
        if (existing.getReactionType() == targetType) {
            reactionRepository.delete(existing);
            decrementReactionCount(review, targetType);
            log.debug("[Review Reaction] 리뷰 반응 취소. reviewId={}, userId={}, type={}", review.getId(), userId, targetType);
        } else {
            ReactionType oldType = existing.getReactionType();
            existing.updateReactionType(targetType);

            decrementReactionCount(review, oldType);
            incrementReactionCount(review, targetType);
            log.debug("[Review Reaction] 리뷰 반응 변경. reviewId={}, userId={}, oldType={}, newType={}",
                    review.getId(), userId, oldType, targetType);
        }
    }




    private void incrementReactionCount(CourseReview review, ReactionType type) {
        if (type == ReactionType.LIKE) {
            reviewRepository.incrementLikeCount(review.getId());
        } else {
            reviewRepository.incrementDislikeCount(review.getId());
        }
    }




    private void decrementReactionCount(CourseReview review, ReactionType type) {
        if (type == ReactionType.LIKE) {
            reviewRepository.decrementLikeCount(review.getId());
        } else {
            reviewRepository.decrementDislikeCount(review.getId());
        }
    }




    private void updateCourseReviewStats(List<String> courseKeys) {
        long count = reviewRepository.countByCourseKeys(courseKeys);
        Double avg = reviewRepository.getAverageRatingByCourseKeys(courseKeys);
        courseReviewCatalog.updateReviewStats(courseKeys, avg != null ? avg.floatValue() : 0.0f, (int) count);
    }

    private CourseReference getCourse(String courseKey) {
        return courseReviewCatalog.findByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다."));
    }

    private CourseReference getCourseForUpdate(String courseKey) {
        return courseReviewCatalog.findByCourseKeyForUpdate(courseKey)
                .or(() -> courseReviewCatalog.findByCourseKey(courseKey))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 강의입니다."));
    }
}
