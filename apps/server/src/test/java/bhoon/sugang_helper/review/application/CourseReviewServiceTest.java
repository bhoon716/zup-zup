package bhoon.sugang_helper.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.review.domain.CourseReview;
import bhoon.sugang_helper.review.domain.CourseReviewReaction;
import bhoon.sugang_helper.review.domain.CourseReviewReactionRepository;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.review.domain.ReactionType;
import bhoon.sugang_helper.review.domain.ReviewScopeKey;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CourseReviewServiceTest {

    private static final String COURSE_KEY = "C1";
    private static final String SUBJECT_CODE = "CSE101";
    private static final String PROFESSOR = "김교수";
    private static final String NORMALIZED_PROFESSOR = ReviewScopeKey.normalizeProfessor(PROFESSOR);
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long REVIEW_ID = 100L;

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

    private Course createCourse() {
        return Course.builder()
                .courseKey(COURSE_KEY)
                .subjectCode(SUBJECT_CODE)
                .professor(PROFESSOR)
                .name("자료구조")
                .averageRating(4.0f)
                .reviewCount(10)
                .build();
    }

    private CourseReview createReview(String courseKey, Long userId) {
        return CourseReview.builder()
                .courseKey(courseKey)
                .userId(userId)
                .rating(5)
                .build();
    }

    private void mockCurrentUser(User user) {
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(user.getEmail());
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private void mockCourseStats(Course course, double avgRating, long count) {
        lenient().when(reviewRepository.getAverageRatingBySubjectCodeAndProfessor(SUBJECT_CODE, NORMALIZED_PROFESSOR))
                .thenReturn(avgRating);
        lenient().when(reviewRepository.countBySubjectCodeAndProfessor(SUBJECT_CODE, NORMALIZED_PROFESSOR))
                .thenReturn(count);
        lenient().when(courseRepository.findBySubjectCodeAndProfessor(SUBJECT_CODE, NORMALIZED_PROFESSOR))
                .thenReturn(List.of(course));
    }

    @Nested
    @DisplayName("리뷰 작성")
    class CreateReviewTests {

        @Test
        @DisplayName("리뷰 작성 성공")
        void createReview_Success() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            Course course = createCourse();
            lenient().when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            lenient().when(reviewRepository.countBySubjectCodeAndProfessorAndUserId(SUBJECT_CODE, NORMALIZED_PROFESSOR,
                    user.getId())).thenReturn(0L);

            CourseReview savedReview = CourseReview.builder()
                    .courseKey(COURSE_KEY)
                    .userId(user.getId())
                    .rating(request.rating())
                    .build();

            when(reviewRepository.saveAndFlush(any(CourseReview.class))).thenReturn(savedReview);
            mockCourseStats(course, 4.1, 11L);

            ReviewResponse response = reviewService.createReview(COURSE_KEY, request);

            assertThat(response.rating()).isEqualTo(5);
            assertThat(course.getAverageRating()).isEqualTo(4.1f);
            assertThat(course.getReviewCount()).isEqualTo(11);
            verify(reviewRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("리뷰 작성 성공 - 코멘트 없이 별점만 작성")
        void createReview_Success_WithoutContent() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            Course course = createCourse();
            lenient().when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            lenient().when(reviewRepository.countBySubjectCodeAndProfessorAndUserId(SUBJECT_CODE, NORMALIZED_PROFESSOR,
                    user.getId())).thenReturn(0L);

            CourseReview savedReview = CourseReview.builder()
                    .courseKey(COURSE_KEY)
                    .userId(user.getId())
                    .rating(request.rating())
                    .build();

            when(reviewRepository.saveAndFlush(any(CourseReview.class))).thenReturn(savedReview);
            mockCourseStats(course, 4.1, 11L);

            ReviewResponse response = reviewService.createReview(COURSE_KEY, request);

            assertThat(response.rating()).isEqualTo(5);
            verify(reviewRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("리뷰 작성 실패 - 없는 강의")
        void createReview_Fail_CourseNotFound() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            String invalidCourseKey = "INVALID";
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            when(courseRepository.findByCourseKey(invalidCourseKey)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(invalidCourseKey, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("detail", "유효하지 않은 강의입니다.");
        }

        @Test
        @DisplayName("리뷰 작성 실패 - 이미 작성함")
        void createReview_Fail_AlreadyReviewed() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(createCourse()));
            when(reviewRepository.countBySubjectCodeAndProfessorAndUserId(SUBJECT_CODE, NORMALIZED_PROFESSOR,
                    user.getId())).thenReturn(1L);

            assertThatThrownBy(() -> reviewService.createReview(COURSE_KEY, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("리뷰 조회")
    class GetReviewsTests {

        @Test
        @DisplayName("리뷰 목록 조회 성공 (페이징)")
        void getReviews_Success() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            PageRequest pageRequest = PageRequest.of(0, 10);
            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            Page<CourseReview> page = new PageImpl<>(List.of(review));

            when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(createCourse()));
            when(reviewRepository.findBySubjectCodeAndProfessorWithMyReviewFirst(SUBJECT_CODE, NORMALIZED_PROFESSOR,
                    user.getId(), pageRequest)).thenReturn(page);

            Page<ReviewResponse> responsePage = reviewService.getReviews(COURSE_KEY, pageRequest);

            assertThat(responsePage.getContent()).hasSize(1);
            assertThat(responsePage.getContent().get(0).isMine()).isFalse();
        }
    }

    @Nested
    @DisplayName("리뷰 수정")
    class UpdateReviewTests {

        @Test
        @DisplayName("리뷰 수정 성공")
        void updateReview_Success() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, user.getId());
            ReviewUpdateRequest request = new ReviewUpdateRequest(2);

            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            Course course = createCourse();
            when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            mockCourseStats(course, 4.2, 10L);

            ReviewResponse response = reviewService.updateReview(REVIEW_ID, request);

            assertThat(response.rating()).isEqualTo(2);
            assertThat(review.getRating()).isEqualTo(2);
            assertThat(course.getAverageRating()).isEqualTo(4.2f);
            assertThat(course.getReviewCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("리뷰 수정 실패 - 남의 글 수정")
        void updateReview_Fail_Unauthorized() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            ReviewUpdateRequest request = new ReviewUpdateRequest(2);

            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("공감")
    class ReactionTests {

        @Test
        @DisplayName("공감/비공감 토글 - 새로운 '공감' 추가")
        void toggleReaction_AddNewReaction() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            ReviewReactionRequest request = new ReviewReactionRequest(ReactionType.LIKE);

            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            when(reactionRepository.findByReviewAndUserId(review, user.getId())).thenReturn(Optional.empty());

            reviewService.toggleReaction(REVIEW_ID, request);

            verify(reviewRepository).incrementLikeCount(any());
            verify(reactionRepository).save(any(CourseReviewReaction.class));
        }

        @Test
        @DisplayName("공감/비공감 토글 - '공감' 취소")
        void toggleReaction_CancelExistingReaction() {
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

            reviewService.toggleReaction(REVIEW_ID, request);

            verify(reviewRepository).decrementLikeCount(any());
            verify(reactionRepository).delete(existingReaction);
        }

        @Test
        @DisplayName("공감/비공감 토글 - '공감'을 '비공감'으로 변경")
        void toggleReaction_SwitchReaction() {
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

            reviewService.toggleReaction(REVIEW_ID, request);

            verify(reviewRepository).decrementLikeCount(any());
            verify(reviewRepository).incrementDislikeCount(any());
            assertThat(existingReaction.getReactionType()).isEqualTo(ReactionType.DISLIKE);
            verify(reactionRepository, never()).delete(any());
            verify(reactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("리뷰 삭제")
    class DeleteReviewTests {

        @Test
        @DisplayName("리뷰 삭제 성공")
        void deleteReview_Success() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, user.getId());
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            Course course = createCourse();
            when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            mockCourseStats(course, 4.0, 9L);

            reviewService.deleteReview(REVIEW_ID);

            verify(reviewRepository).delete(review);
            assertThat(course.getAverageRating()).isEqualTo(4.0f);
            assertThat(course.getReviewCount()).isEqualTo(9);
        }

        @Test
        @DisplayName("리뷰 삭제 실패 - 권한 없음")
        void deleteReview_Fail_Unauthorized() {
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("리뷰 삭제 성공 - 관리자 권한")
        void deleteReview_Admin_Success() {
            User admin = createUser(USER_ID, Role.ADMIN);
            mockCurrentUser(admin);

            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            Course course = createCourse();
            when(courseRepository.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            mockCourseStats(course, 4.0, 9L);

            reviewService.deleteReview(REVIEW_ID);

            verify(reviewRepository).delete(review);
            assertThat(course.getAverageRating()).isEqualTo(4.0f);
            assertThat(course.getReviewCount()).isEqualTo(9);
        }
    }
}
