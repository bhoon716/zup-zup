package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private UserService userService;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @Test
    @DisplayName("사용자 프로필 정보를 정상적으로 수정한다")
    void updateProfile() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Old Name")
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when
        UserResponse result = userService.updateProfile("New Name");

        // then
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(user.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("회원 탈퇴 시 구독 정보를 포함하여 삭제한다")
    void withdraw() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Name")
                .role(Role.USER)
                .build();

        securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when
        userService.withdraw();

        // then
        verify(subscriptionRepository, times(1)).deleteAllByUserId(1L);
        verify(userRepository, times(1)).delete(user);
    }
}
