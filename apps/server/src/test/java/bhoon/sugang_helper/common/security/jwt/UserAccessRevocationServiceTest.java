package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.redis.RedisService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccessRevocationServiceTest {

    private static final long ACCESS_TOKEN_EXPIRATION = 7_200_000L;

    @Mock
    private RedisService redisService;

    private UserAccessRevocationService userAccessRevocationService;

    @BeforeEach
    void setUp() {
        userAccessRevocationService = new UserAccessRevocationService(redisService, ACCESS_TOKEN_EXPIRATION);
    }

    @Test
    void revokeStoresMarkerForMaximumAccessLifetime() {
        userAccessRevocationService.revoke(1L);

        verify(redisService).setValues("UA:1", "logout", Duration.ofMillis(ACCESS_TOKEN_EXPIRATION));
    }

    @Test
    void isRevokedReadsUserMarkerWithoutDatabaseAccess() {
        given(redisService.hasKey("UA:1")).willReturn(true);

        assertThat(userAccessRevocationService.isRevoked(1L)).isTrue();
    }
}
