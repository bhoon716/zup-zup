package bhoon.sugang_helper.academic.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.course.service.CourseReviewCatalogService;
import bhoon.sugang_helper.academic.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.academic.review.dto.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.academic.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.academic.review.entity.CourseEmojiReview;
import bhoon.sugang_helper.academic.review.entity.CourseReview;
import bhoon.sugang_helper.academic.review.repository.CourseEmojiReviewRepository;
import bhoon.sugang_helper.academic.review.repository.CourseReviewRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({CourseReviewCatalogService.class, CourseReviewService.class,
        CourseEmojiReviewService.class, CourseReviewQueryService.class})
class ReviewScopeContractTest {

    private static final String SUBJECT = "CSE101";
    private static final String CURRENT = "2026:U211600020:CSE101:02";
    private static final String PREVIOUS = "2025:U211600010:CSE101:01";
    private static final String EMAIL = "reviewer@example.com";

    @Autowired private CourseRepository courses;
    @Autowired private CourseReviewRepository reviews;
    @Autowired private CourseEmojiReviewRepository emojis;
    @Autowired private CourseReviewService reviewService;
    @Autowired private CourseEmojiReviewService emojiService;
    @Autowired private CourseReviewQueryService reviewQuery;
    @MockitoBean private UserIdentityQuery users;
    private MockedStatic<SecurityUtil> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtil.class);
        security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);
        security.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(EMAIL);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(new UserIdentity(1L, false)));
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", " 교수 미지정 "})
    @DisplayName("교수 미지정 변형은 다른 학기·분반의 별점과 이모지를 연결하지 않는다")
    void unassignedProfessorKeepsEachOfferingSeparate(String professor) {
        // given
        Course current = saveCourse(CURRENT, SUBJECT, professor);
        Course previous = saveCourse(PREVIOUS, SUBJECT, professor);
        saveReview(PREVIOUS, 1L, 1);
        CourseReview otherReview = saveReview(CURRENT, 2L, 5);
        saveEmoji(PREVIOUS, 1L, "👍");
        saveEmoji(CURRENT, 2L, "🔥");

        // when & then
        assertThat(reviewQuery.hasReviewByUser(CURRENT, SUBJECT, professor, 1L)).isFalse();
        assertThat(reviewService.getReviews(CURRENT, PageRequest.of(0, 10)))
                .extracting(ReviewResponse::id).containsExactly(otherReview.getId());
        assertThat(emojiService.getCourseEmojiStats(CURRENT))
                .containsExactly(new CourseEmojiReviewResponse("🔥", 1L, false));

        // when
        reviewService.createReview(CURRENT, new ReviewCreateRequest(3));
        emojiService.toggleEmoji(CURRENT, "👍");

        // then
        assertThat(reviewQuery.hasReviewByUser(CURRENT, SUBJECT, professor, 1L)).isTrue();
        assertThat(current.getReviewCount()).isEqualTo(2);
        assertThat(current.getAverageRating()).isEqualTo(4.0f);
        assertThat(previous.getReviewCount()).isZero();
        assertThat(emojis.count()).isEqualTo(3);
        assertThat(emojiService.getCourseEmojiStats(CURRENT)).containsExactlyInAnyOrder(
                new CourseEmojiReviewResponse("👍", 1L, true),
                new CourseEmojiReviewResponse("🔥", 1L, false));
        assertDuplicateReviewRejected();
    }

    @ParameterizedTest
    @ValueSource(strings = {" 김교수 ", " 외부교원1 "})
    @DisplayName("같은 과목·교수는 학기·분반을 공유하고 다른 교수·과목은 제외한다")
    void identifiableProfessorSharesOnlyItsReviewScope(String professor) {
        // given
        Course current = saveCourse(CURRENT, SUBJECT, professor);
        Course previous = saveCourse(PREVIOUS, SUBJECT, professor.trim());
        Course otherProfessor = saveCourse("OTHER-PROFESSOR", SUBJECT, "외부교원2");
        Course otherSubject = saveCourse("OTHER-SUBJECT", "CSE102", professor);
        CourseReview myReview = saveReview(PREVIOUS, 1L, 5);
        CourseReview otherReview = saveReview(CURRENT, 2L, 3);
        saveReview(otherProfessor.getCourseKey(), 1L, 1);
        saveReview(otherSubject.getCourseKey(), 1L, 1);
        saveEmoji(PREVIOUS, 1L, "👍");
        saveEmoji(CURRENT, 2L, "👍");
        saveEmoji(otherProfessor.getCourseKey(), 1L, "🔥");
        saveEmoji(otherSubject.getCourseKey(), 1L, "🔥");

        // when & then
        assertThat(reviewQuery.hasReviewByUser(CURRENT, SUBJECT, professor, 1L)).isTrue();
        assertThat(reviewService.getReviews(CURRENT, PageRequest.of(0, 10)))
                .extracting(ReviewResponse::id).containsExactly(myReview.getId(), otherReview.getId());
        assertThat(emojiService.getCourseEmojiStats(CURRENT))
                .containsExactly(new CourseEmojiReviewResponse("👍", 2L, true));

        // when: toggle through another offering cancels the original shared reaction.
        emojiService.toggleEmoji(CURRENT, "👍");
        reviewService.updateReview(myReview.getId(), new ReviewUpdateRequest(1));

        // then
        assertThat(emojiService.getCourseEmojiStats(CURRENT))
                .containsExactly(new CourseEmojiReviewResponse("👍", 1L, false));
        assertThat(List.of(current, previous)).allSatisfy(course -> {
            assertThat(course.getReviewCount()).isEqualTo(2);
            assertThat(course.getAverageRating()).isEqualTo(2.0f);
        });

        // when
        reviewService.deleteReview(myReview.getId());
        emojiService.toggleEmoji(CURRENT, "👍");

        // then
        assertThat(List.of(current, previous)).allSatisfy(course -> {
            assertThat(course.getReviewCount()).isEqualTo(1);
            assertThat(course.getAverageRating()).isEqualTo(3.0f);
        });
        assertThat(emojiService.getCourseEmojiStats(CURRENT))
                .containsExactly(new CourseEmojiReviewResponse("👍", 2L, true));
        assertThat(List.of(otherProfessor, otherSubject)).allSatisfy(course -> {
            assertThat(course.getReviewCount()).isZero();
            assertThat(course.getAverageRating()).isZero();
        });
        reviewService.createReview(CURRENT, new ReviewCreateRequest(5));
        assertDuplicateReviewRejected();
    }

    private void assertDuplicateReviewRejected() {
        assertThatThrownBy(() -> reviewService.createReview(CURRENT, new ReviewCreateRequest(4)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("식별 가능한 교수의 대상 조회가 비어 있으면 입력 강의 키로 대체하지 않는다")
    void missingProfessorScopeDoesNotFallBackToCurrentKey() {
        // given
        saveCourse(CURRENT, SUBJECT, "김교수");
        saveReview(CURRENT, 1L, 5);

        // when & then
        assertThat(reviewQuery.hasReviewByUser(CURRENT, SUBJECT, "외부교원1", 1L)).isFalse();
    }

    private Course saveCourse(String key, String subject, String professor) {
        return courses.saveAndFlush(Course.builder()
                .courseKey(key).subjectCode(subject).name("자료구조").classNumber("1")
                .professor(professor).capacity(50).current(10).academicYear("2026")
                .semester("U211600020").build());
    }

    private CourseReview saveReview(String key, Long userId, int rating) {
        return reviews.saveAndFlush(CourseReview.builder().courseKey(key).userId(userId).rating(rating).build());
    }

    private void saveEmoji(String key, Long userId, String emoji) {
        emojis.saveAndFlush(CourseEmojiReview.builder().courseKey(key).userId(userId).emoji(emoji).build());
    }
}
