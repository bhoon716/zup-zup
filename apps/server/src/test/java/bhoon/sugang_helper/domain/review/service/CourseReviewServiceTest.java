package bhoon.sugang_helper.domain.review.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseReviewServiceTest {

    private static final String COURSE_KEY = "C1";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long REVIEW_ID = 100L;
    private static final String REVIEW_CONTENT = "Good Course";

    @InjectMocks
    private CourseReviewService reviewService;

    @Mock
    private CourseReviewRepository reviewRepository;

    @Mock
    private CourseReviewReactionRepository reactionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private MockedStatic<SecurityUtil> securityUtilMockedStatic;

    @BeforeEach
    void setUp() {
        securityUtilMockedStatic = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilMockedStatic != null) {
            securityUtilMockedStatic.close();
        }
    }

    private User createUser(Long id, Role role) {
        return User.builder()
                .id(id)
                .name("Test User " + id)
                .email("test" + id + "@test.com")
                .role(role)
                .build();
    }

    private CourseReview createReview(String courseKey, Long userId) {
        return CourseReview.builder()
                .courseKey(courseKey)
                .userId(userId)
                .rating(5)
                .content("Good")
                .build();
    }

    private void mockCurrentUser(User user) {
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(user.getEmail());
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private void mockCourseStats(String courseKey, double avgRating, long count) {
        lenient().when(reviewRepository.getAverageRatingByCourseKey(courseKey)).thenReturn(avgRating);
        lenient().when(reviewRepository.countByCourseKey(courseKey)).thenReturn(count);
    }

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_Success() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);
        ReviewCreateRequest request = new ReviewCreateRequest(5, REVIEW_CONTENT);

        Course course = Course.builder().courseKey(COURSE_KEY).build();
        lenient().when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(true);
        lenient().when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
        lenient().when(reviewRepository.existsByCourseKeyAndUserId(COURSE_KEY, user.getId())).thenReturn(false);

        CourseReview savedReview = CourseReview.builder()
                .courseKey(COURSE_KEY).userId(user.getId()).rating(request.rating()).content(request.content()).build();

        when(reviewRepository.saveAndFlush(any(CourseReview.class))).thenReturn(savedReview);
        mockCourseStats(COURSE_KEY, 5.0, 1L);

        // when
        ReviewResponse response = reviewService.createReview(COURSE_KEY, request);

        // then
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo(REVIEW_CONTENT);
        verify(reviewRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 없는 강의")
    void createReview_Fail_CourseNotFound() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        String invalidCourseKey = "INVALID";
        ReviewCreateRequest request = new ReviewCreateRequest(5, "Good");

        when(courseRepository.existsByCourseKey(invalidCourseKey)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(invalidCourseKey, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("detail", "유효하지 않은 강의입니다.");
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 이미 작성함")
    void createReview_Fail_AlreadyReviewed() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);
        ReviewCreateRequest request = new ReviewCreateRequest(5, "Good");

        lenient().when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(true);
        lenient().when(reviewRepository.existsByCourseKeyAndUserId(COURSE_KEY, user.getId())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(COURSE_KEY, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
                });
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 (페이징)")
    void getReviews_Success() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        PageRequest pageRequest = PageRequest.of(0, 10);
        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        Page<CourseReview> page = new PageImpl<>(List.of(review));

        when(reviewRepository.findByCourseKeyWithMyReviewFirst(COURSE_KEY, user.getId(), pageRequest)).thenReturn(page);

        // when
        Page<ReviewResponse> responsePage = reviewService.getReviews(COURSE_KEY, pageRequest);

        // then
        assertThat(responsePage.getContent()).hasSize(1);
        assertThat(responsePage.getContent().get(0).isMine()).isFalse(); // userId=OTHER, currentUserId=USER
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_Success() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, user.getId());
        ReviewUpdateRequest request = new ReviewUpdateRequest(2, "Bad Course");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        Course course = Course.builder().courseKey(COURSE_KEY).build();
        when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
        mockCourseStats(COURSE_KEY, 2.0, 1L);

        // when
        ReviewResponse response = reviewService.updateReview(REVIEW_ID, request);

        // then
        assertThat(response.rating()).isEqualTo(2);
        assertThat(response.content()).isEqualTo("Bad Course");
        assertThat(review.getRating()).isEqualTo(2);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 남의 글 수정")
    void updateReview_Fail_Unauthorized() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        ReviewUpdateRequest request = new ReviewUpdateRequest(2, "Bad");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("공감/비공감 토글 - 새로운 '공감' 추가")
    void toggleReaction_AddNewReaction() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        ReviewReactionRequest request = new ReviewReactionRequest(ReactionType.LIKE);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reactionRepository.findByReviewAndUserId(review, user.getId())).thenReturn(Optional.empty());

        // when
        reviewService.toggleReaction(REVIEW_ID, request);

        // then
        assertThat(review.getLikeCount()).isEqualTo(1);
        verify(reactionRepository).save(any(CourseReviewReaction.class));
    }

    @Test
    @DisplayName("공감/비공감 토글 - '공감' 취소")
    void toggleReaction_CancelExistingReaction() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        review.increaseLikeCount();

        CourseReviewReaction existingReaction = CourseReviewReaction.builder()
                .review(review).userId(user.getId()).reactionType(ReactionType.LIKE).build();
        ReviewReactionRequest request = new ReviewReactionRequest(ReactionType.LIKE);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reactionRepository.findByReviewAndUserId(review, user.getId()))
                .thenReturn(Optional.of(existingReaction));

        // when
        reviewService.toggleReaction(REVIEW_ID, request);

        // then
        assertThat(review.getLikeCount()).isEqualTo(0);
        verify(reactionRepository).delete(existingReaction);
    }

    @Test
    @DisplayName("공감/비공감 토글 - '공감'을 '비공감'으로 변경")
    void toggleReaction_SwitchReaction() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        review.increaseLikeCount();

        CourseReviewReaction existingReaction = CourseReviewReaction.builder()
                .review(review).userId(user.getId()).reactionType(ReactionType.LIKE).build();
        ReviewReactionRequest request = new ReviewReactionRequest(ReactionType.DISLIKE);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reactionRepository.findByReviewAndUserId(review, user.getId()))
                .thenReturn(Optional.of(existingReaction));

        // when
        reviewService.toggleReaction(REVIEW_ID, request);

        // then
        assertThat(review.getLikeCount()).isEqualTo(0);
        assertThat(review.getDislikeCount()).isEqualTo(1);
        assertThat(existingReaction.getReactionType()).isEqualTo(ReactionType.DISLIKE);
        verify(reactionRepository, never()).delete(any());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_Success() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, user.getId());
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        Course course = Course.builder().courseKey(COURSE_KEY).build();
        when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
        mockCourseStats(COURSE_KEY, 0.0, 0L);

        // when
        reviewService.deleteReview(REVIEW_ID);

        // then
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 권한 없음")
    void deleteReview_Fail_Unauthorized() {
        // given
        User user = createUser(USER_ID, Role.USER);
        mockCurrentUser(user);

        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 관리자 권한")
    void deleteReview_Admin_Success() {
        // given
        User admin = createUser(USER_ID, Role.ADMIN);
        mockCurrentUser(admin);

        CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        Course course = Course.builder().courseKey(COURSE_KEY).build();
        when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
        mockCourseStats(COURSE_KEY, 0.0, 0L);

        // when
        reviewService.deleteReview(REVIEW_ID);

        // then
        verify(reviewRepository).delete(review);
    }
}
