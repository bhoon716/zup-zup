package bhoon.sugang_helper.academic.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.academic.course.dto.result.CourseReference;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.academic.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.academic.review.dto.result.CourseReviewStats;
import bhoon.sugang_helper.academic.review.entity.CourseReview;
import bhoon.sugang_helper.academic.review.repository.CourseReviewRepository;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CourseReviewServiceTest {

    private static final String COURSE_KEY = "C1";
    private static final String SUBJECT_CODE = "CSE101";
    private static final String PROFESSOR = "김교수";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long REVIEW_ID = 100L;

    @InjectMocks
    private CourseReviewService reviewService;

    @Mock
    private CourseReviewRepository reviewRepository;

    @Mock
    private CourseReviewCatalogService courseReviewCatalog;

    @Mock
    private UserIdentityQueryService userIdentityQuery;

    @BeforeEach
    void setUp() {
        lenient().when(courseReviewCatalog.findReviewCourseKeys(any(CourseReference.class)))
                .thenReturn(List.of(COURSE_KEY));
    }

    private User createUser(Long id, Role role) {
        return User.builder()
                .id(id)
                .name("Test User " + id)
                .email("test" + id + "@test.com")
                .role(role)
                .build();
    }

    private CourseReference createCourse() {
        return createCourse(PROFESSOR);
    }

    private CourseReference createCourse(String professor) {
        return new CourseReference(COURSE_KEY, SUBJECT_CODE, professor);
    }

    private CourseReview createReview(String courseKey, Long userId) {
        return CourseReview.builder()
                .courseKey(courseKey)
                .userId(userId)
                .rating(5)
                .build();
    }

    private UserIdentity identity(User user) {
        return new UserIdentity(user.getId(), user.getRole() == Role.ADMIN);
    }

    private void mockCurrentUser(User user) {
        lenient().when(userIdentityQuery.requireCurrentUser()).thenReturn(identity(user));
        lenient().when(userIdentityQuery.findCurrentUser()).thenReturn(Optional.of(identity(user)));
    }

    private void mockCourseStats(double avgRating, long count) {
        lenient().when(reviewRepository.findStatsByCourseKeys(List.of(COURSE_KEY)))
                .thenReturn(new CourseReviewStats(avgRating, count));

    }

    @Nested
    @DisplayName("리뷰 작성")
    class CreateReviewTests {

        @Test
        @DisplayName("리뷰 작성 성공")
        void createReview_Success() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            CourseReference course = createCourse();
            lenient().when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            lenient().when(reviewRepository.countByCourseKeysAndUserId(List.of(COURSE_KEY), user.getId()))
                    .thenReturn(0L);

            CourseReview savedReview = CourseReview.builder()
                    .courseKey(COURSE_KEY)
                    .userId(user.getId())
                    .rating(request.rating())
                    .build();

            when(reviewRepository.saveAndFlush(any(CourseReview.class))).thenReturn(savedReview);
            mockCourseStats(4.1, 11L);

            // when
            ReviewResponse response = reviewService.createReview(COURSE_KEY, request);

            // then
            assertThat(response.rating()).isEqualTo(5);
            verify(courseReviewCatalog).updateReviewStats(List.of(COURSE_KEY), 4.1f, 11);

            verify(reviewRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("리뷰 작성 성공 - 코멘트 없이 별점만 작성")
        void createReview_Success_WithoutContent() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            CourseReference course = createCourse();
            lenient().when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            lenient().when(reviewRepository.countByCourseKeysAndUserId(List.of(COURSE_KEY), user.getId()))
                    .thenReturn(0L);

            CourseReview savedReview = CourseReview.builder()
                    .courseKey(COURSE_KEY)
                    .userId(user.getId())
                    .rating(request.rating())
                    .build();

            when(reviewRepository.saveAndFlush(any(CourseReview.class))).thenReturn(savedReview);
            mockCourseStats(4.1, 11L);

            // when
            ReviewResponse response = reviewService.createReview(COURSE_KEY, request);

            // then
            assertThat(response.rating()).isEqualTo(5);
            verify(reviewRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("교수 미지정 강의는 해당 강의에만 별점 리뷰를 연결한다")
        void createReview_UnassignedProfessor_UsesCourseKeyOnly() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);
            ReviewCreateRequest request = new ReviewCreateRequest(5);
            CourseReference course = createCourse("교수 미지정");
            when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            when(reviewRepository.countByCourseKeysAndUserId(List.of(COURSE_KEY), user.getId())).thenReturn(0L);
            when(reviewRepository.saveAndFlush(any(CourseReview.class))).thenReturn(createReview(COURSE_KEY, user.getId()));
            when(reviewRepository.findStatsByCourseKeys(List.of(COURSE_KEY))).thenReturn(new CourseReviewStats(5.0, 1L));

            // when
            reviewService.createReview(COURSE_KEY, request);

            // then
            verify(reviewRepository).countByCourseKeysAndUserId(List.of(COURSE_KEY), user.getId());
            verify(courseReviewCatalog).updateReviewStats(List.of(COURSE_KEY), 5.0f, 1);
        }

        @Test
        @DisplayName("리뷰 작성 실패 - 없는 강의")
        void createReview_Fail_CourseNotFound() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            String invalidCourseKey = "INVALID";
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            when(courseReviewCatalog.findByCourseKey(invalidCourseKey)).thenReturn(Optional.empty());

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
            ReviewCreateRequest request = new ReviewCreateRequest(5);

            when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(createCourse()));
            when(reviewRepository.countByCourseKeysAndUserId(List.of(COURSE_KEY), user.getId())).thenReturn(1L);

            // when & then
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
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            PageRequest pageRequest = PageRequest.of(0, 10);
            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            Page<CourseReview> page = new PageImpl<>(List.of(review));

            when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(createCourse()));
            when(reviewRepository.findByCourseKeysWithMyReviewFirst(List.of(COURSE_KEY), user.getId(), pageRequest))
                    .thenReturn(page);

            // when
            Page<ReviewResponse> responsePage = reviewService.getReviews(COURSE_KEY, pageRequest);

            // then
            assertThat(responsePage.getContent()).hasSize(1);
            assertThat(responsePage.getContent().get(0).isMine()).isFalse();
        }

        @Test
        @DisplayName("교수 미지정 강의의 별점 리뷰는 해당 강의에서만 조회한다")
        void getReviews_UnassignedProfessor_UsesCourseKeyOnly() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            PageRequest pageRequest = PageRequest.of(0, 10);
            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            Page<CourseReview> page = new PageImpl<>(List.of(review));
            when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(createCourse("교수 미지정")));
            when(reviewRepository.findByCourseKeysWithMyReviewFirst(List.of(COURSE_KEY), user.getId(), pageRequest))
                    .thenReturn(page);

            // when
            Page<ReviewResponse> responsePage = reviewService.getReviews(COURSE_KEY, pageRequest);

            // then
            assertThat(responsePage.getContent()).hasSize(1);
            verify(reviewRepository).findByCourseKeysWithMyReviewFirst(List.of(COURSE_KEY), user.getId(), pageRequest);
        }

        @Test
        @DisplayName("외부교원N 강의의 별점 리뷰는 같은 교수 식별자 범위에서 조회한다")
        void getReviews_ExternalInstructor_UsesSharedProfessorScope() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            PageRequest pageRequest = PageRequest.of(0, 10);
            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            Page<CourseReview> page = new PageImpl<>(List.of(review));
            when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(createCourse("외부교원1")));
            List<String> courseKeys = List.of("LEGACY-C1", COURSE_KEY);
            when(courseReviewCatalog.findReviewCourseKeys(createCourse("외부교원1")))
                    .thenReturn(courseKeys);
            when(reviewRepository.findByCourseKeysWithMyReviewFirst(courseKeys, user.getId(), pageRequest))
                    .thenReturn(page);

            // when
            Page<ReviewResponse> responsePage = reviewService.getReviews(COURSE_KEY, pageRequest);

            // then
            assertThat(responsePage.getContent()).hasSize(1);
            verify(reviewRepository).findByCourseKeysWithMyReviewFirst(courseKeys, user.getId(), pageRequest);
        }
    }

    @Nested
    @DisplayName("리뷰 수정")
    class UpdateReviewTests {

        @Test
        @DisplayName("리뷰 수정 성공")
        void updateReview_Success() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, user.getId());
            ReviewUpdateRequest request = new ReviewUpdateRequest(2);

            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            CourseReference course = createCourse();
            when(courseReviewCatalog.findByCourseKey(COURSE_KEY)).thenReturn(Optional.of(course));
            mockCourseStats(4.2, 10L);

            // when
            ReviewResponse response = reviewService.updateReview(REVIEW_ID, request);

            // then
            assertThat(response.rating()).isEqualTo(2);
            assertThat(review.getRating()).isEqualTo(2);
            verify(courseReviewCatalog).updateReviewStats(List.of(COURSE_KEY), 4.2f, 10);

        }

        @Test
        @DisplayName("리뷰 수정 실패 - 남의 글 수정")
        void updateReview_Fail_Unauthorized() {
            // given
            User user = createUser(USER_ID, Role.USER);
            mockCurrentUser(user);

            CourseReview review = createReview(COURSE_KEY, OTHER_USER_ID);
            ReviewUpdateRequest request = new ReviewUpdateRequest(2);

            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }
}
