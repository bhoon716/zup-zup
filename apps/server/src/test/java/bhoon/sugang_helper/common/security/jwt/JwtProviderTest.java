package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private JwtProvider jwtProvider;

    private static final String TEST_SECRET_KEY = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 30; // 30 min
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7 days

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtProvider, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        jwtProvider.init();
    }

    @Test
    @DisplayName("Access Token 생성 테스트")
    void createAccessToken() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";

        // when
        String token = jwtProvider.createAccessToken(email, role);

        // then
        assertThat(token).isNotNull();
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    @DisplayName("Refresh Token 생성 테스트")
    void createRefreshToken() {
        // given
        String email = "test@example.com";

        // when
        String token = jwtProvider.createRefreshToken(email);

        // then
        assertThat(token).isNotNull();
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    @DisplayName("토큰 유효성 검사 - 유효한 토큰")
    void validateToken_valid() {
        // given
        String token = jwtProvider.createAccessToken("test@example.com", "ROLE_USER");

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("토큰 유효성 검사 - 블랙리스트 토큰")
    void validateToken_blacklist() {
        // given
        String token = jwtProvider.createAccessToken("test@example.com", "ROLE_USER");
        given(redisService.hasKey("BL:" + token)).willReturn(true);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Authentication 조회 테스트")
    void getAuthentication() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String token = jwtProvider.createAccessToken(email, role);

        // when
        Authentication authentication = jwtProvider.getAuthentication(token);

        // then
        assertThat(authentication.getName()).isEqualTo(email);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo(role);
    }
}
