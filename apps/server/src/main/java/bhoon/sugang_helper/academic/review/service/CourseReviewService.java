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
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.api.users.Users;
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

    private static final String EVENT_KEY = "event";
    private static final String REVIEW_ID_KEY = "reviewId";
    private static final String USER_ID_KEY = "userId";

    private final CourseReviewRepository reviewRepository;
    private final CourseReviewReactionRepository reactionRepository;
    private final CourseReviewCatalogService courseReviewCatalog;
    private final Users userIdentityQuery;

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
        logCourseReviewCreated(saved.getId(), courseKey, scope.subjectCode(), user.id());

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
        logCourseReviewUpdated(reviewId, user.id());

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
        logCourseReviewDeleted(reviewId, user.id());

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

        logCourseReviewReactionAdded(review.getId(), userId, type);
    }

    private void handleExistingReaction(CourseReview review, CourseReviewReaction existing, ReactionType targetType,
                                        Long userId) {
        if (existing.getReactionType() == targetType) {
            reactionRepository.delete(existing);
            decrementReactionCount(review, targetType);
            logCourseReviewReactionRemoved(review.getId(), userId, targetType);
        } else {
            ReactionType oldType = existing.getReactionType();
            existing.updateReactionType(targetType);

            decrementReactionCount(review, oldType);
            incrementReactionCount(review, targetType);
            logCourseReviewReactionChanged(review.getId(), userId, oldType, targetType);
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

    private void logCourseReviewCreated(Long reviewId, String courseKey, String subjectCode, Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "course_review_created")
                .addKeyValue(REVIEW_ID_KEY, reviewId)
                .addKeyValue("courseKey", courseKey)
                .addKeyValue("subjectCode", subjectCode)
                .addKeyValue(USER_ID_KEY, userId)
                .log("[Review] 리뷰 등록.");
    }

    private void logCourseReviewUpdated(Long reviewId, Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "course_review_updated")
                .addKeyValue(REVIEW_ID_KEY, reviewId)
                .addKeyValue(USER_ID_KEY, userId)
                .log("[Review] 리뷰 수정.");
    }

    private void logCourseReviewDeleted(Long reviewId, Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "course_review_deleted")
                .addKeyValue(REVIEW_ID_KEY, reviewId)
                .addKeyValue(USER_ID_KEY, userId)
                .log("[Review] 리뷰 삭제.");
    }

    private void logCourseReviewReactionAdded(Long reviewId, Long userId, ReactionType type) {
        log.atDebug().addKeyValue(EVENT_KEY, "course_review_reaction_added")
                .addKeyValue(REVIEW_ID_KEY, reviewId)
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("type", type)
                .log("[Review Reaction] 리뷰 반응 등록.");
    }

    private void logCourseReviewReactionRemoved(Long reviewId, Long userId, ReactionType type) {
        log.atDebug().addKeyValue(EVENT_KEY, "course_review_reaction_removed")
                .addKeyValue(REVIEW_ID_KEY, reviewId)
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("type", type)
                .log("[Review Reaction] 리뷰 반응 취소.");
    }

    private void logCourseReviewReactionChanged(Long reviewId,
                                                Long userId,
                                                ReactionType oldType,
                                                ReactionType newType) {
        log.atDebug().addKeyValue(EVENT_KEY, "course_review_reaction_changed")
                .addKeyValue(REVIEW_ID_KEY, reviewId)
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("oldType", oldType)
                .addKeyValue("newType", newType)
                .log("[Review Reaction] 리뷰 반응 변경.");
    }

}
