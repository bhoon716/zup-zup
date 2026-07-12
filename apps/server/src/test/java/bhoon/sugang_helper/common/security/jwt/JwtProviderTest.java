package bhoon.sugang_helper.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.redis.RedisService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private static final String EMAIL = "test@example.com";
    private static final String ROLE = "ROLE_USER";
    private static final String TEST_SECRET_KEY = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 30; // 30 min
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7 days
    @Mock
    private RedisService redisService;
    @InjectMocks
    private JwtProvider jwtProvider;

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
        // when
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);

        // then
        assertThat(token).isNotNull();
        assertTrue(jwtProvider.validateToken(token));
        assertFalse(jwtProvider.validateRefreshToken(token));
    }

    @Test
    @DisplayName("Refresh Token은 원문 대신 family와 SHA-256 digest만 Redis에 저장한다")
    void createRefreshToken_storesOnlyOpaqueRecord() {
        String token = jwtProvider.createRefreshToken(EMAIL);

        assertThat(token).isNotNull();
        assertTrue(jwtProvider.validateRefreshToken(token));
        assertFalse(jwtProvider.validateToken(token));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisService).setValues(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("RT:" + EMAIL);
        assertThat(valueCaptor.getValue())
                .startsWith("v2:")
                .contains(sha256(token))
                .doesNotContain(token);
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));
    }

    @Test
    @DisplayName("같은 시각에 만든 Refresh Token도 고유한 jti와 family를 가진다")
    void createRefreshToken_isUniqueWithinSameClockTick() {
        String first = jwtProvider.createRefreshToken(EMAIL);
        String second = jwtProvider.createRefreshToken(EMAIL);

        Claims firstClaims = parseClaims(first);
        Claims secondClaims = parseClaims(second);

        assertThat(first).isNotEqualTo(second);
        assertThat(firstClaims.getId()).isNotBlank().isNotEqualTo(secondClaims.getId());
        assertThat(firstClaims.get("sid", String.class)).isNotBlank().isNotEqualTo(secondClaims.get("sid", String.class));
    }

    @Test
    @DisplayName("토큰 유효성 검사 - 유효한 토큰")
    void validateToken_valid() {
        // given
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("토큰 유효성 검사 - 블랙리스트 토큰")
    void validateToken_blacklist() {
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willReturn(true);

        boolean isValid = jwtProvider.validateToken(token);

        assertFalse(isValid);
        verify(redisService).hasKey("BL:" + sha256(token));
    }

    @Test
    @DisplayName("블랙리스트 토큰은 로그에 원문이나 이메일을 남기지 않는다")
    void validateToken_blacklistDoesNotLogCredential() {
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willReturn(true);
        Logger logger = (Logger) LoggerFactory.getLogger(JwtProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertFalse(jwtProvider.validateToken(token));

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", String::concat);
            assertThat(messages).doesNotContain(token).doesNotContain(EMAIL);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("기존 raw blacklist key는 TTL 동안 읽기 전용으로 호환한다")
    void validateToken_readsLegacyRawBlacklistKeyWithoutWritingIt() {
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willReturn(false);
        given(redisService.hasKey("BL:" + token)).willReturn(true);

        assertFalse(jwtProvider.validateToken(token));
        verify(redisService).hasKey("BL:" + sha256(token));
        verify(redisService).hasKey("BL:" + token);
    }

    @Test
    @DisplayName("로그아웃 blacklist에는 access token 원문이 아닌 SHA-256 key를 쓴다")
    void blacklistAccessToken_storesOnlyOpaqueKey() {
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);

        jwtProvider.blacklistAccessToken(token);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisService).setValues(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("BL:" + sha256(token)).doesNotContain(token);
        assertThat(valueCaptor.getValue()).isEqualTo("logout");
        assertThat(durationCaptor.getValue()).isPositive();
    }

    @Test
    @DisplayName("Refresh Token rotation은 digest를 CAS로 교체하고 원문을 Redis에 쓰지 않는다")
    void rotateRefreshToken_rotatesOpaqueRecordAtomically() {
        String originalToken = jwtProvider.createRefreshToken(EMAIL);
        ArgumentCaptor<String> initialRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL), initialRecordCaptor.capture(),
                org.mockito.ArgumentMatchers.any(Duration.class));
        String initialRecord = initialRecordCaptor.getValue();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(initialRecord);
        given(redisService.compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(initialRecord), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);

        String rotatedToken = jwtProvider.rotateRefreshToken(EMAIL, originalToken);

        ArgumentCaptor<String> rotatedRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(initialRecord), rotatedRecordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        assertThat(rotatedToken).isNotNull().isNotEqualTo(originalToken);
        assertThat(rotatedRecordCaptor.getValue()).contains(sha256(rotatedToken)).doesNotContain(rotatedToken);
    }

    @Test
    @DisplayName("같은 family의 이전 Refresh Token 재사용은 현재 family를 폐기한다")
    void rotateRefreshToken_replayRevokesCurrentFamily() {
        String originalToken = jwtProvider.createRefreshToken(EMAIL);
        ArgumentCaptor<String> initialRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL), initialRecordCaptor.capture(),
                org.mockito.ArgumentMatchers.any(Duration.class));
        String initialRecord = initialRecordCaptor.getValue();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(initialRecord);
        given(redisService.compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(initialRecord), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);
        jwtProvider.rotateRefreshToken(EMAIL, originalToken);
        ArgumentCaptor<String> rotatedRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(initialRecord), rotatedRecordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        String rotatedRecord = rotatedRecordCaptor.getValue();

        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(rotatedRecord);
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, rotatedRecord)).willReturn(true);

        assertThat(jwtProvider.rotateRefreshToken(EMAIL, originalToken)).isNull();
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, rotatedRecord);
    }

    @Test
    @DisplayName("Redis 재시작으로 refresh registry가 사라지면 새 토큰을 발급하지 않고 거절한다")
    void rotateRefreshToken_missingRedisStateRejectsFailClosed() {
        String token = jwtProvider.createRefreshToken(EMAIL);
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(null);

        assertThat(jwtProvider.rotateRefreshToken(EMAIL, token)).isNull();
        verify(redisService).getValues("RT:" + EMAIL);
    }

    @Test
    @DisplayName("기존 raw refresh record는 한 번만 읽어 CAS로 opaque record로 전환한다")
    void rotateRefreshToken_migratesLegacyRawRecordWithoutDualWriting() {
        String legacyToken = createLegacyRefreshToken();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(legacyToken);
        given(redisService.compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(legacyToken), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);

        String rotatedToken = jwtProvider.rotateRefreshToken(EMAIL, legacyToken);

        ArgumentCaptor<String> opaqueRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(legacyToken), opaqueRecordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        assertThat(opaqueRecordCaptor.getValue())
                .startsWith("v2:")
                .contains(sha256(legacyToken))
                .doesNotContain(rotatedToken);
    }

    @Test
    @DisplayName("legacy refresh 재사용도 전환된 v2 family를 폐기한다")
    void rotateRefreshToken_legacyReplayRevokesMigratedFamily() {
        String legacyToken = createLegacyRefreshToken();
        given(redisService.getValues("RT:" + EMAIL)).willReturn(legacyToken);
        given(redisService.compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(legacyToken), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);

        jwtProvider.rotateRefreshToken(EMAIL, legacyToken);
        ArgumentCaptor<String> migratedRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                org.mockito.ArgumentMatchers.eq(legacyToken), migratedRecordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        String migratedRecord = migratedRecordCaptor.getValue();

        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(migratedRecord);
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, migratedRecord)).willReturn(true);

        assertThat(jwtProvider.rotateRefreshToken(EMAIL, legacyToken)).isNull();
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, migratedRecord);
    }

    @Test
    @DisplayName("이전 login family의 로그아웃은 현재 login family를 삭제하지 않는다")
    void revokeRefreshToken_doesNotDeleteDifferentCurrentFamily() {
        String previousToken = jwtProvider.createRefreshToken(EMAIL);
        jwtProvider.createRefreshToken(EMAIL);
        ArgumentCaptor<String> recordsCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService, times(2)).setValues(org.mockito.ArgumentMatchers.eq("RT:" + EMAIL),
                recordsCaptor.capture(), org.mockito.ArgumentMatchers.any(Duration.class));
        String currentRecord = recordsCaptor.getAllValues().get(1);
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(currentRecord);

        jwtProvider.revokeRefreshToken(EMAIL, previousToken);

        verify(redisService, never()).compareAndDeleteValues(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Authentication 조회 테스트")
    void getAuthentication() {
        // given
        String token = jwtProvider.createAccessToken(EMAIL, ROLE);

        // when
        Authentication authentication = jwtProvider.getAuthentication(token);

        // then
        assertThat(authentication.getName()).isEqualTo(EMAIL);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo(ROLE);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String createLegacyRefreshToken() {
        Date now = new Date();
        return Jwts.builder()
                .subject(EMAIL)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION))
                .claim("token_type", "refresh")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
