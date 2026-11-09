package bhoon.sugang_helper.identity.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

@ExtendWith(MockitoExtension.class)
class SessionInvalidatorTest {

    private static final String EMAIL = "same@example.com";

    @Mock
    private FindByIndexNameSessionRepository<MapSession> sessions;

    @Test
    @DisplayName("탈퇴 시 해당 사용자 식별자의 모든 기기 세션만 삭제한다")
    void withdrawalDeletesAllDevicesOfOnlyTheWithdrawnUserId() {
        // given
        MapSession first = session(1L);
        MapSession second = session(1L);
        MapSession rejoined = session(2L);
        MapSession legacy = new MapSession();
        given(sessions.findByPrincipalName(EMAIL)).willReturn(Map.of(
                first.getId(), first, second.getId(), second, rejoined.getId(), rejoined, legacy.getId(), legacy));

        new SessionInvalidator(sessions).invalidateAll(1L, EMAIL);

        // when & then
        verify(sessions).deleteById(first.getId());
        verify(sessions).deleteById(second.getId());
        verify(sessions, never()).deleteById(rejoined.getId());
        verify(sessions, never()).deleteById(legacy.getId());
    }

    @Test
    @DisplayName("세션 조회 실패를 탈퇴 성공으로 간주하지 않는다")
    void failedSessionLookupIsNotSilentlyTreatedAsSuccessfulWithdrawal() {
        // given
        given(sessions.findByPrincipalName(EMAIL)).willThrow(new RedisConnectionFailureException("unavailable"));

        // when & then
        assertThatThrownBy(() -> new SessionInvalidator(sessions).invalidateAll(1L, EMAIL))
                .isInstanceOf(RedisConnectionFailureException.class);
    }

    private MapSession session(Long userId) {
        MapSession session = new MapSession();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionPrincipal(userId, EMAIL).authentication("ROLE_USER"));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
