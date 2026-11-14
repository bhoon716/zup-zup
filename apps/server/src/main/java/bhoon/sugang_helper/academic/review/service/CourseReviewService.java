package bhoon.sugang_helper.academic.review.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.criteria.ReviewScopeKey;
import bhoon.sugang_helper.academic.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.academic.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.academic.review.dto.result.CourseReviewStats;
import bhoon.sugang_helper.academic.review.entity.CourseReview;
import bhoon.sugang_helper.academic.review.repository.CourseReviewRepository;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
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

    private static final String INVALID_COURSE_MESSAGE = "유효하지 않은 강의입니다.";

    private static final String EVENT_KEY = "event";
    private static final String REVIEW_ID_KEY = "reviewId";
    private static final String USER_ID_KEY = "userId";

    private final CourseReviewRepository reviewRepository;
    private final CourseReviewCatalogService courseReviewCatalog;
    private final CurrentUserQuery currentUserQuery;

    @Transactional
    public ReviewResponse createReview(String courseKey, ReviewCreateRequest request) {
        UserIdentity user = currentUserQuery.requireCurrentUser();

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

        return ReviewResponse.from(saved, user.id());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(String courseKey, Pageable pageable) {
        Long currentUserId = currentUserQuery.findCurrentUser().map(UserIdentity::id).orElse(null);
        CourseReference course = getCourse(courseKey);
        Pageable boundedPageable = PageableGuard.requireBounded(pageable);
        List<String> courseKeys = courseReviewCatalog.findReviewCourseKeys(course);
        return reviewRepository.findByCourseKeysWithMyReviewFirst(courseKeys, currentUserId, boundedPageable)
                .map(review -> ReviewResponse.from(review, currentUserId));
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        UserIdentity user = currentUserQuery.requireCurrentUser();

        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        validateReviewOwner(review, user);

        CourseReference course = getCourseForUpdate(review.getCourseKey());
        review.update(request.rating());
        reviewRepository.saveAndFlush(review);
        logCourseReviewUpdated(reviewId, user.id());

        updateCourseReviewStats(courseReviewCatalog.findReviewCourseKeys(course));

        return ReviewResponse.from(review, user.id());
    }

    private void validateReviewOwner(CourseReview review, UserIdentity user) {
        if (!review.getUserId().equals(user.id()) && !user.administrator()) {
            throw new CustomException(ErrorCode.REVIEW_UNAUTHORIZED);
        }
    }

    private void updateCourseReviewStats(List<String> courseKeys) {
        CourseReviewStats stats = reviewRepository.findStatsByCourseKeys(courseKeys);
        courseReviewCatalog.updateReviewStats(courseKeys, (float) stats.average(), (int) stats.count());
    }

    private CourseReference getCourse(String courseKey) {
        return courseReviewCatalog.findByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, INVALID_COURSE_MESSAGE));
    }

    private CourseReference getCourseForUpdate(String courseKey) {
        return courseReviewCatalog.findByCourseKeyForUpdate(courseKey)
                .or(() -> courseReviewCatalog.findByCourseKey(courseKey))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, INVALID_COURSE_MESSAGE));
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
}
