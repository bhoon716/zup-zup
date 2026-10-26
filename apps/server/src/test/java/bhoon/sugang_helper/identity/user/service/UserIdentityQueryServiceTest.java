package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.identity.user.dto.result.UserAccountStatus;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserIdentityQueryServiceTest {

    @InjectMocks
    private UserIdentityQueryService userIdentityQueryService;

    @Mock
    private UserRepository userRepository;

    @Test
    void findAccountStatuses_returnsOnlyAccountStateForRequestedUsers() {
        User activeUser = user(1L);
        User withdrawnUser = user(2L);
        withdrawnUser.withdraw();
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(activeUser, withdrawnUser));

        List<UserAccountStatus> result = userIdentityQueryService.findAccountStatuses(List.of(1L, 2L));

        assertThat(result).containsExactly(
                new UserAccountStatus(1L, false),
                new UserAccountStatus(2L, true));
        verify(userRepository).findAllById(List.of(1L, 2L));
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
