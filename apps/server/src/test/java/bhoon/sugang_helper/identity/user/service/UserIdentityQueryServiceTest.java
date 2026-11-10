package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.UserAccountStatus;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserIdentityQueryServiceTest {

    private static final String EMAIL = "test1@test.com";

    @InjectMocks
    private UserIdentityQueryService userIdentityQueryService;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("비로그인 사용자의 필수 조회는 A001이고 선택 조회는 DB에 접근하지 않는다")
    void currentUserWithoutAuthentication() {
        // when & then
        assertThatThrownBy(userIdentityQueryService::requireCurrentUser)
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED));
        assertThat(userIdentityQueryService.findCurrentUser()).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("로그인한 활성 계정을 필수 조회와 선택 조회에서 반환한다")
    void currentUserWithActiveAccount() {
        // given
        authenticate();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L)));

        // when & then
        assertThat(userIdentityQueryService.requireCurrentUser()).isEqualTo(new UserIdentity(1L, false));
        assertThat(userIdentityQueryService.findCurrentUser()).contains(new UserIdentity(1L, false));
    }

    @Test
    @DisplayName("로그인 계정을 찾지 못하면 필수 조회는 U002이고 선택 조회는 비어 있다")
    void currentUserWithMissingAccount() {
        // given
        authenticate();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // when & then
        assertCurrentAccountUnavailable();
    }

    @Test
    @DisplayName("탈퇴한 계정은 필수 조회와 선택 조회에서 제외한다")
    void currentUserWithWithdrawnAccount() {
        // given
        authenticate();
        User withdrawnUser = user(1L);
        withdrawnUser.withdraw();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(withdrawnUser));

        // when & then
        assertCurrentAccountUnavailable();
    }

    @Test
    @DisplayName("계정 상태 조회는 요청한 사용자의 상태만 반환한다")
    void findAccountStatuses_returnsOnlyAccountStateForRequestedUsers() {
        // given
        User activeUser = user(1L);
        User withdrawnUser = user(2L);
        withdrawnUser.withdraw();
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(activeUser, withdrawnUser));

        // when
        List<UserAccountStatus> result = userIdentityQueryService.findAccountStatuses(List.of(1L, 2L));

        // then
        assertThat(result).containsExactly(
                new UserAccountStatus(1L, false),
                new UserAccountStatus(2L, true));
        verify(userRepository).findAllById(List.of(1L, 2L));
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of()));
    }

    private void assertCurrentAccountUnavailable() {
        assertThatThrownBy(userIdentityQueryService::requireCurrentUser)
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_UNAUTHORIZED));
        assertThat(userIdentityQueryService.findCurrentUser()).isEmpty();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("Test User " + id)
                .email("test" + id + "@test.com")
                .role(Role.USER)
                .build();
    }
}
