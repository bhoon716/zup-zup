package bhoon.sugang_helper.domain.wishlist.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.wishlist.response.WishlistResponse;
import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import bhoon.sugang_helper.domain.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @InjectMocks
    private WishlistService wishlistService;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private static MockedStatic<SecurityUtil> securityUtil;

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
        String email = "test@example.com";
        String courseKey = "2024:1:12345:01";

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(mock(User.class)));
        given(courseRepository.findByCourseKey(courseKey)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.toggleWishlist(courseKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @DisplayName("찜 토글 시 - 이미 찜한 상태면 삭제(Un-wish)")
    @Test
    void toggleWishlist_AlreadyExists_Delete() {
        // given
        String email = "test@example.com";
        String courseKey = "2024:1:12345:01";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        Wishlist wishlist = Wishlist.builder().userId(1L).courseKey(courseKey).build();

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(courseRepository.findByCourseKey(courseKey)).willReturn(Optional.of(mock(Course.class)));
        given(wishlistRepository.findByUserIdAndCourseKey(1L, courseKey)).willReturn(Optional.of(wishlist));

        // when
        wishlistService.toggleWishlist(courseKey);

        // then
        verify(wishlistRepository, times(1)).delete(wishlist);
        verify(wishlistRepository, times(0)).save(any(Wishlist.class));
    }

    @DisplayName("찜 토글 시 - 찜하지 않은 상태면 추가(Wish)")
    @Test
    void toggleWishlist_NotExists_Save() {
        // given
        String email = "test@example.com";
        String courseKey = "2024:1:12345:01";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(courseRepository.findByCourseKey(courseKey)).willReturn(Optional.of(mock(Course.class)));
        given(wishlistRepository.findByUserIdAndCourseKey(1L, courseKey)).willReturn(Optional.empty());

        // when
        wishlistService.toggleWishlist(courseKey);

        // then
        verify(wishlistRepository, times(0)).delete(any(Wishlist.class));
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
    }

    @DisplayName("내 찜 목록 조회 - 빈 목록 반환")
    @Test
    void getMyWishlist_EmptyList_ReturnsEmpty() {
        // given
        String email = "test@example.com";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
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
        String email = "test@example.com";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        String courseKey1 = "CK1";
        String courseKey2 = "CK2";

        Wishlist w1 = Wishlist.builder().userId(1L).courseKey(courseKey1).build();
        Wishlist w2 = Wishlist.builder().userId(1L).courseKey(courseKey2).build();

        Course c1 = mock(Course.class);
        given(c1.getCourseKey()).willReturn(courseKey1);
        given(c1.getName()).willReturn("Course1");
        given(c1.getCapacity()).willReturn(50); // for available calculation
        given(c1.getCurrent()).willReturn(10);

        Course c2 = mock(Course.class);
        given(c2.getCourseKey()).willReturn(courseKey2);
        given(c2.getName()).willReturn("Course2");
        given(c2.getCapacity()).willReturn(30);
        given(c2.getCurrent()).willReturn(30);

        given(SecurityUtil.getCurrentUserEmail()).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByUserId(1L)).willReturn(Arrays.asList(w1, w2));
        given(courseRepository.findByCourseKeyIn(anyList())).willReturn(Arrays.asList(c1, c2));

        // when
        List<WishlistResponse> result = wishlistService.getMyWishlist();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCourseName()).isEqualTo("Course1");
        assertThat(result.get(1).getCourseName()).isEqualTo("Course2");
    }
}
