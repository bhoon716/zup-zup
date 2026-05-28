package bhoon.sugang_helper.domain.review.service;

import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.review.entity.CourseEmojiReview;
import bhoon.sugang_helper.domain.review.repository.CourseEmojiReviewRepository;
import bhoon.sugang_helper.domain.review.response.CourseEmojiReviewResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseEmojiReviewServiceTest {

    private static final String COURSE_KEY = "C1";
    private static final Long USER_ID = 1L;
    private static final String EMOJI_THUMB = "👍";
    private static final String EMOJI_FIRE = "🔥";
    private static final String EMOJI_LAUGH = "😂";

    @InjectMocks
    private CourseEmojiReviewService emojiReviewService;

    @Mock
    private CourseEmojiReviewRepository emojiReviewRepository;

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

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .name("Test User " + id)
                .email("test" + id + "@test.com")
                .role(Role.USER)
                .build();
    }

    private void mockCurrentUser(User user) {
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmail).thenReturn(user.getEmail());
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(user.getEmail());
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("이모지 토글 - 처음 탭하면 신규 리액션 생성")
    void toggleEmoji_AddNew() {
        // given
        User user = createUser(USER_ID);
        mockCurrentUser(user);
        when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(true);
        when(emojiReviewRepository.findByCourseKeyAndUserIdAndEmoji(COURSE_KEY, USER_ID, EMOJI_THUMB))
                .thenReturn(Optional.empty());

        // when
        emojiReviewService.toggleEmoji(COURSE_KEY, EMOJI_THUMB);

        // then
        verify(emojiReviewRepository).save(any(CourseEmojiReview.class));
    }

    @Test
    @DisplayName("이모지 토글 - 커스텀 시스템 이모지도 추가 가능")
    void toggleEmoji_AddCustomEmoji() {
        // given
        User user = createUser(USER_ID);
        mockCurrentUser(user);
        when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(true);
        when(emojiReviewRepository.findByCourseKeyAndUserIdAndEmoji(COURSE_KEY, USER_ID, EMOJI_LAUGH))
                .thenReturn(Optional.empty());

        // when
        emojiReviewService.toggleEmoji(COURSE_KEY, EMOJI_LAUGH);

        // then
        verify(emojiReviewRepository).save(any(CourseEmojiReview.class));
    }

    @Test
    @DisplayName("이모지 토글 - 동일 이모지 재탭 시 기존 리액션 삭제(취소)")
    void toggleEmoji_Cancel() {
        // given
        User user = createUser(USER_ID);
        mockCurrentUser(user);
        when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(true);

        CourseEmojiReview existing = CourseEmojiReview.builder()
                .courseKey(COURSE_KEY).userId(USER_ID).emoji(EMOJI_THUMB).build();
        when(emojiReviewRepository.findByCourseKeyAndUserIdAndEmoji(COURSE_KEY, USER_ID, EMOJI_THUMB))
                .thenReturn(Optional.of(existing));

        // when
        emojiReviewService.toggleEmoji(COURSE_KEY, EMOJI_THUMB);

        // then
        verify(emojiReviewRepository).delete(existing);
        verify(emojiReviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("이모지 토글 - 빈 이모지는 거부한다")
    void toggleEmoji_BlankEmoji_throwsException() {
        // given
        User user = createUser(USER_ID);
        mockCurrentUser(user);

        // when & then
        assertThatThrownBy(() -> emojiReviewService.toggleEmoji(COURSE_KEY, " "))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verifyNoInteractions(courseRepository, emojiReviewRepository);
    }

    @Test
    @DisplayName("이모지 토글 - 존재하지 않는 강의는 거부한다")
    void toggleEmoji_InvalidCourse_throwsException() {
        // given
        User user = createUser(USER_ID);
        mockCurrentUser(user);
        when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> emojiReviewService.toggleEmoji(COURSE_KEY, EMOJI_THUMB))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        verify(courseRepository).existsByCourseKey(COURSE_KEY);
        verifyNoInteractions(emojiReviewRepository);
    }

    @Test
    @DisplayName("이모지 토글 - 비로그인 상태면 인증 오류를 반환한다")
    void toggleEmoji_Unauthorized_throwsException() {
        // given
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmail).thenReturn("anonymous@test.com");
        when(userRepository.findByEmail("anonymous@test.com")).thenReturn(Optional.empty());
        when(courseRepository.existsByCourseKey(COURSE_KEY)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> emojiReviewService.toggleEmoji(COURSE_KEY, EMOJI_THUMB))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_UNAUTHORIZED);
        verify(courseRepository).existsByCourseKey(COURSE_KEY);
        verify(userRepository).findByEmail("anonymous@test.com");
        verifyNoInteractions(emojiReviewRepository);
    }

    @Test
    @DisplayName("이모지 통계 조회 - 모든 등록 이모지의 카운트 및 본인 탭 여부 반환")
    void getCourseEmojiStats_ReturnsMergedStats() {
        // given
        User user = createUser(USER_ID);
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        when(emojiReviewRepository.findEmojiStatsByCourseKey(COURSE_KEY)).thenReturn(List.<Object[]>of(
                new Object[]{EMOJI_THUMB, 3L},
                new Object[]{EMOJI_FIRE, 1L},
                new Object[]{EMOJI_LAUGH, 1L}
        ));
        when(emojiReviewRepository.existsByCourseKeyAndUserIdAndEmoji(COURSE_KEY, USER_ID, EMOJI_THUMB)).thenReturn(true);
        when(emojiReviewRepository.existsByCourseKeyAndUserIdAndEmoji(COURSE_KEY, USER_ID, EMOJI_FIRE)).thenReturn(false);
        when(emojiReviewRepository.existsByCourseKeyAndUserIdAndEmoji(COURSE_KEY, USER_ID, EMOJI_LAUGH)).thenReturn(false);

        // when
        List<CourseEmojiReviewResponse> result = emojiReviewService.getCourseEmojiStats(COURSE_KEY);

        // then
        assertThat(result).hasSize(3);
        CourseEmojiReviewResponse thumbResponse = result.stream()
                .filter(r -> r.getEmoji().equals(EMOJI_THUMB))
                .findFirst().orElseThrow();
        assertThat(thumbResponse.getCount()).isEqualTo(3L);
        assertThat(thumbResponse.isMine()).isTrue();

        CourseEmojiReviewResponse fireResponse = result.stream()
                .filter(r -> r.getEmoji().equals(EMOJI_FIRE))
                .findFirst().orElseThrow();
        assertThat(fireResponse.getCount()).isEqualTo(1L);
        assertThat(fireResponse.isMine()).isFalse();

        CourseEmojiReviewResponse laughResponse = result.stream()
                .filter(r -> r.getEmoji().equals(EMOJI_LAUGH))
                .findFirst().orElseThrow();
        assertThat(laughResponse.getCount()).isEqualTo(1L);
        assertThat(laughResponse.isMine()).isFalse();
    }

    @Test
    @DisplayName("이모지 통계 조회 - 비로그인 상태에서도 카운트는 조회된다")
    void getCourseEmojiStats_AnonymousUser_DoesNotSetMine() {
        // given
        securityUtilMockedStatic.when(SecurityUtil::getCurrentUserEmailOrNull).thenReturn(null);
        when(emojiReviewRepository.findEmojiStatsByCourseKey(COURSE_KEY)).thenReturn(List.<Object[]>of(
                new Object[]{EMOJI_THUMB, 2L}
        ));

        // when
        List<CourseEmojiReviewResponse> result = emojiReviewService.getCourseEmojiStats(COURSE_KEY);

        // then
        assertThat(result).hasSize(1);
        CourseEmojiReviewResponse thumbResponse = result.stream()
                .filter(r -> r.getEmoji().equals(EMOJI_THUMB))
                .findFirst().orElseThrow();
        assertThat(thumbResponse.getCount()).isEqualTo(2L);
        assertThat(thumbResponse.isMine()).isFalse();
        verify(userRepository, never()).findByEmail(any());
    }
}
