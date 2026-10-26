package bhoon.sugang_helper.planning.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.course.dto.result.CourseSummary;
import bhoon.sugang_helper.academic.course.service.CourseCatalogQueryService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.result.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.planning.wishlist.dto.response.WishlistResponse;
import bhoon.sugang_helper.planning.wishlist.entity.Wishlist;
import bhoon.sugang_helper.planning.wishlist.repository.WishlistRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String COURSE_KEY = "2024:1:12345:01";
    private static final String COURSE_KEY_1 = "CK1";
    private static final String COURSE_KEY_2 = "CK2";
    private static MockedStatic<SecurityUtil> securityUtil;
    @InjectMocks
    private WishlistService wishlistService;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private CourseCatalogQueryService courseCatalogQuery;
    @Mock
    private UserIdentityQueryService userIdentityQuery;

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @DisplayName("찜 토글 시 - 없는 강좌면 예외 발생")
    @Test
    void toggleWishlist_CourseNotFound_ThrowsException() {
        // given
        String email = EMAIL;
        String courseKey = COURSE_KEY;

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userIdentityQuery.findByEmail(email)).willReturn(Optional.of(new UserIdentity(1L, false)));
        given(courseCatalogQuery.findByCourseKey(courseKey)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.toggleWishlist(courseKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @DisplayName("찜 토글 시 - 이미 찜한 상태면 삭제(Un-wish) 후 false 반환")
    @Test
    void toggleWishlist_AlreadyExists_Delete() {
        // given
        String email = EMAIL;
        String courseKey = COURSE_KEY;
        UserIdentity user = new UserIdentity(1L, false);
        Wishlist wishlist = Wishlist.builder().userId(1L).courseKey(courseKey).build();

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userIdentityQuery.findByEmail(email)).willReturn(Optional.of(user));
        given(courseCatalogQuery.findByCourseKey(courseKey)).willReturn(Optional.of(summary(courseKey, "Course", 20, 3)));
        given(wishlistRepository.findByUserIdAndCourseKey(1L, courseKey)).willReturn(Optional.of(wishlist));

        // when
        var response = wishlistService.toggleWishlist(courseKey);

        // then
        verify(wishlistRepository, times(1)).delete(wishlist);
        verify(wishlistRepository, times(0)).save(any(Wishlist.class));
        assertThat(response.isWished()).isFalse();
    }

    @DisplayName("찜 토글 시 - 찜하지 않은 상태면 추가(Wish) 후 true 반환")
    @Test
    void toggleWishlist_NotExists_Save() {
        // given
        String email = EMAIL;
        String courseKey = COURSE_KEY;
        UserIdentity user = new UserIdentity(1L, false);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userIdentityQuery.findByEmail(email)).willReturn(Optional.of(user));
        given(courseCatalogQuery.findByCourseKey(courseKey)).willReturn(Optional.of(summary(courseKey, "Course", 20, 3)));
        given(wishlistRepository.findByUserIdAndCourseKey(1L, courseKey)).willReturn(Optional.empty());

        // when
        var response = wishlistService.toggleWishlist(courseKey);

        // then
        verify(wishlistRepository, times(0)).delete(any(Wishlist.class));
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
        assertThat(response.isWished()).isTrue();
    }

    @DisplayName("내 찜 목록 조회 - 빈 목록 반환")
    @Test
    void getMyWishlist_EmptyList_ReturnsEmpty() {
        // given
        String email = EMAIL;
        UserIdentity user = new UserIdentity(1L, false);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userIdentityQuery.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByUserId(1L)).willReturn(Collections.emptyList());

        // when
        List<WishlistResponse> result = wishlistService.getMyWishlist();

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("내 찜 목록 조회 - Course 정보 매핑 후 반환")
    @Test
    void getMyWishlist_Exists_ReturnsMappedResponse() {
        // given
        String email = EMAIL;
        UserIdentity user = new UserIdentity(1L, false);
        Wishlist w1 = Wishlist.builder().userId(1L).courseKey(COURSE_KEY_1).build();
        Wishlist w2 = Wishlist.builder().userId(1L).courseKey(COURSE_KEY_2).build();

        CourseSummary c1 = summary(COURSE_KEY_1, "Course1", 50, 10);
        CourseSummary c2 = summary(COURSE_KEY_2, "Course2", 30, 30);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userIdentityQuery.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByUserId(1L)).willReturn(Arrays.asList(w1, w2));
        given(courseCatalogQuery.findByCourseKeyIn(anyList())).willReturn(Arrays.asList(c1, c2));

        // when
        List<WishlistResponse> result = wishlistService.getMyWishlist();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).courseName()).isEqualTo("Course1");
        assertThat(result.get(1).courseName()).isEqualTo("Course2");
    }

    @Test
    @DisplayName("내 찜 목록 조회 - 중복 courseKey를 기존 강의로 병합한다")
    void getMyWishlist_DuplicateCourseKey_KeepsExistingCourse() {
        UserIdentity user = new UserIdentity(1L, false);
        Wishlist wishlist = Wishlist.builder().userId(1L).courseKey(COURSE_KEY_1).build();
        CourseSummary existingCourse = summary(COURSE_KEY_1, "Existing Course", 30, 10);
        CourseSummary replacementCourse = summary(COURSE_KEY_1, "Replacement Course", 30, 20);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(EMAIL);
        given(userIdentityQuery.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(wishlistRepository.findByUserId(1L)).willReturn(List.of(wishlist));
        given(courseCatalogQuery.findByCourseKeyIn(anyList())).willReturn(List.of(existingCourse, replacementCourse));

        List<WishlistResponse> result = wishlistService.getMyWishlist();

        assertThat(result).singleElement().satisfies(response ->
                assertThat(response.courseName()).isEqualTo("Existing Course"));
    }

    private CourseSummary summary(String key, String name, int capacity, int current) {
        return new CourseSummary(key, "SUBJECT", "01", name, "Professor", capacity, current,
                "2026", "U211600010", "전공필수", "3", "월 1-A", "Room", List.of());
    }
}
