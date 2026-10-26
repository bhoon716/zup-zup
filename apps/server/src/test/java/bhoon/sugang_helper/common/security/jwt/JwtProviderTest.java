package bhoon.sugang_helper.common.security.jwt;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.dto.result.JwtAuthenticationResult;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private static final String EMAIL = "test@example.com";
    private static final String ROLE = "ROLE_USER";
    private static final Long USER_ID = 1L;
    private static final String TEST_SECRET_KEY = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 30;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;
    private static final String VALID_HASH = "a".repeat(64);
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String PRESENTED_TOKEN = "presented";
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


        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);


        assertThat(token).isNotNull();
        assertTrue((jwtProvider.getAccessAuthentication(token) != null));
        assertFalse((jwtProvider.getRefreshAuthentication(token) != null));
    }

    @Test
    @DisplayName("Refresh Token은 원문 대신 family와 SHA-256 digest만 Redis에 저장한다")
    void createRefreshToken_storesOnlyOpaqueRecord() {
        String token = jwtProvider.createRefreshToken(USER_ID, EMAIL);

        assertThat(token).isNotNull();
        assertTrue((jwtProvider.getRefreshAuthentication(token) != null));
        assertFalse((jwtProvider.getAccessAuthentication(token) != null));

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
        String first = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        String second = jwtProvider.createRefreshToken(USER_ID, EMAIL);

        Claims firstClaims = parseClaims(first);
        Claims secondClaims = parseClaims(second);

        assertThat(first).isNotEqualTo(second);
        assertThat(firstClaims.getId()).isNotBlank().isNotEqualTo(secondClaims.getId());
        assertThat(firstClaims.get("sid", String.class)).isNotBlank().isNotEqualTo(secondClaims.get("sid", String.class));
        assertThat(firstClaims.get("uid", Long.class)).isEqualTo(USER_ID);
        assertThat(secondClaims.get("uid", Long.class)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("토큰 유효성 검사 - 유효한 토큰")
    void validateToken_valid() {

        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);


        boolean isValid = (jwtProvider.getAccessAuthentication(token) != null);


        assertTrue(isValid);
    }

    @Test
    @DisplayName("만료된 토큰은 WARN으로 fingerprint만 기록한다")
    void validateToken_expiredLogsWarnWithoutToken() {
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .subject(EMAIL)
                .issuedAt(new Date(now.getTime() - 2_000))
                .expiration(new Date(now.getTime() - 1_000))
                .claim(TOKEN_TYPE_CLAIM, "access")
                .claim("uid", USER_ID)
                .claim("role", ROLE)
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
        Logger logger = (Logger) LoggerFactory.getLogger(JwtProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertFalse((jwtProvider.getAccessAuthentication(expiredToken) != null));

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("[JWT] Expired JWT token: tokenFingerprint=" + sha256(expiredToken).substring(0, 8))
                        .doesNotContain(expiredToken, EMAIL);
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("토큰 유효성 검사 - 블랙리스트 토큰")
    void validateToken_blacklist() {
        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willReturn(true);

        boolean isValid = (jwtProvider.getAccessAuthentication(token) != null);

        assertFalse(isValid);
        verify(redisService).hasKey("BL:" + sha256(token));
    }

    @Test
    @DisplayName("블랙리스트 토큰은 로그에 원문이나 이메일을 남기지 않는다")
    void validateToken_blacklistDoesNotLogCredential() {
        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willReturn(true);
        Logger logger = (Logger) LoggerFactory.getLogger(JwtProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertFalse((jwtProvider.getAccessAuthentication(token) != null));

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
        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willReturn(false);
        given(redisService.hasKey("BL:" + token)).willReturn(true);

        assertFalse((jwtProvider.getAccessAuthentication(token) != null));
        verify(redisService).hasKey("BL:" + sha256(token));
        verify(redisService).hasKey("BL:" + token);
    }

    @Test
    @DisplayName("로그아웃 blacklist에는 access token 원문이 아닌 SHA-256 key를 쓴다")
    void blacklistAccessToken_storesOnlyOpaqueKey() {
        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);

        jwtProvider.blacklistAccessToken(token, jwtProvider.getAccessAuthentication(token));

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
        String originalToken = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        ArgumentCaptor<String> initialRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValues(eq("RT:" + EMAIL), initialRecordCaptor.capture(),
                any(Duration.class));
        String initialRecord = initialRecordCaptor.getValue();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(initialRecord);
        given(redisService.compareAndSetValues(eq("RT:" + EMAIL),
                eq(initialRecord), anyString(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);

        String rotatedToken = jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(originalToken), originalToken);

        ArgumentCaptor<String> rotatedRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(eq("RT:" + EMAIL),
                eq(initialRecord), rotatedRecordCaptor.capture(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        assertThat(rotatedToken).isNotNull().isNotEqualTo(originalToken);
        assertThat(rotatedRecordCaptor.getValue()).contains(sha256(rotatedToken)).doesNotContain(rotatedToken);
        assertThat(parseClaims(rotatedToken).get("uid", Long.class)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("같은 family의 이전 Refresh Token 재사용은 현재 family를 폐기한다")
    void rotateRefreshToken_replayRevokesCurrentFamily() {
        String originalToken = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        ArgumentCaptor<String> initialRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValues(eq("RT:" + EMAIL), initialRecordCaptor.capture(),
                any(Duration.class));
        String initialRecord = initialRecordCaptor.getValue();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(initialRecord);
        given(redisService.compareAndSetValues(eq("RT:" + EMAIL),
                eq(initialRecord), anyString(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);
        jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(originalToken), originalToken);
        ArgumentCaptor<String> rotatedRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(eq("RT:" + EMAIL),
                eq(initialRecord), rotatedRecordCaptor.capture(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        String rotatedRecord = rotatedRecordCaptor.getValue();

        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(rotatedRecord);
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, rotatedRecord)).willReturn(true);

        assertThat(jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(originalToken), originalToken)).isNull();
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, rotatedRecord);
    }

    @Test
    @DisplayName("Redis 재시작으로 refresh registry가 사라지면 새 토큰을 발급하지 않고 거절한다")
    void rotateRefreshToken_missingRedisStateRejectsFailClosed() {
        String token = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(null);

        assertThat(jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(token), token)).isNull();
        verify(redisService).getValues("RT:" + EMAIL);
    }

    @Test
    @DisplayName("사용자 ID claim이 없는 기존 refresh token은 재발급하지 않는다")
    void rotateRefreshToken_rejectsLegacyTokenWithoutUserId() {
        String legacyToken = createLegacyRefreshToken();
        clearInvocations(redisService);

        assertFalse((jwtProvider.getRefreshAuthentication(legacyToken) != null));
    }

    @Test
    @DisplayName("사용자 ID claim이 없는 기존 access token은 인증하지 않는다")
    void validateToken_rejectsLegacyAccessTokenWithoutUserId() {
        Date now = new Date();
        String legacyToken = Jwts.builder()
                .subject(EMAIL)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                .claim(TOKEN_TYPE_CLAIM, "access")
                .claim("role", ROLE)
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse((jwtProvider.getAccessAuthentication(legacyToken) != null));
    }

    @Test
    @DisplayName("탈퇴 시 원래 이메일의 모든 refresh registry를 제거한다")
    void revokeAllRefreshTokens_removesRegistry() {
        jwtProvider.revokeAllRefreshTokens(EMAIL);

        verify(redisService).deleteValues("RT:" + EMAIL);
    }

    @Test
    @DisplayName("이전 login family의 로그아웃은 현재 login family를 삭제하지 않는다")
    void revokeRefreshToken_doesNotDeleteDifferentCurrentFamily() {
        String previousToken = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        jwtProvider.createRefreshToken(USER_ID, EMAIL);
        ArgumentCaptor<String> recordsCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService, times(2)).setValues(eq("RT:" + EMAIL),
                recordsCaptor.capture(), any(Duration.class));
        String currentRecord = recordsCaptor.getAllValues().get(1);
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(currentRecord);

        jwtProvider.revokeRefreshToken(jwtProvider.getRefreshAuthentication(previousToken), previousToken);

        verify(redisService, never()).compareAndDeleteValues(anyString(),
                anyString());
    }

    @Test
    @DisplayName("같은 family의 이전 토큰으로 로그아웃하면 회전 상태를 폐기하고 rollback을 차단한다")
    void revokeRefreshToken_sameFamilyPreviousTokenPreventsRollback() {
        String originalToken = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        ArgumentCaptor<String> initialRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValues(eq("RT:" + EMAIL),
                initialRecordCaptor.capture(), any(Duration.class));
        String initialRecord = initialRecordCaptor.getValue();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(initialRecord);
        given(redisService.compareAndSetValues(eq("RT:" + EMAIL),
                eq(initialRecord), anyString(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);

        String replacementToken = jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(originalToken), originalToken);
        ArgumentCaptor<String> replacementRecordCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(eq("RT:" + EMAIL),
                eq(initialRecord), replacementRecordCaptor.capture(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        String replacementRecord = replacementRecordCaptor.getValue();
        String tokenFamily = parseClaims(originalToken).get("sid", String.class);
        String revocationKey = "RTR:" + EMAIL + ":" + tokenFamily;

        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(replacementRecord);
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, replacementRecord)).willReturn(true);
        given(redisService.hasKey(revocationKey)).willReturn(true);

        jwtProvider.revokeRefreshToken(jwtProvider.getRefreshAuthentication(originalToken), originalToken);

        assertThat(jwtProvider.rollbackRefreshToken(jwtProvider.getRefreshAuthentication(originalToken), originalToken, replacementToken)).isFalse();
        verify(redisService).setValues(revocationKey, "logout", Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, replacementRecord);
        verify(redisService, never()).compareAndSetValues(eq("RT:" + EMAIL),
                eq(replacementRecord), anyString(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
    }

    @Test
    @DisplayName("Authentication 조회 테스트")
    void getAuthentication() {

        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);


        Authentication authentication = jwtProvider.getAccessAuthentication(token).authentication();


        assertThat(authentication.getName()).isEqualTo(EMAIL);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo(ROLE);
        assertThat(jwtProvider.getAccessAuthentication(token).userId()).isEqualTo(USER_ID);
    }

    @Test
    void legacyRegistryUpgradeRetainsReplayProtection() {
        Date now = new Date();
        String legacyToken = Jwts.builder().subject(EMAIL).claim("uid", USER_ID).claim(TOKEN_TYPE_CLAIM, "refresh")
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8))).compact();
        given(redisService.getValues("RT:" + EMAIL)).willReturn(legacyToken);
        given(redisService.compareAndSetValues(eq("RT:" + EMAIL),
                eq(legacyToken), anyString(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)))).willReturn(true);

        String replacement = jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(legacyToken), legacyToken);

        ArgumentCaptor<String> record = ArgumentCaptor.forClass(String.class);
        verify(redisService).compareAndSetValues(eq("RT:" + EMAIL),
                eq(legacyToken), record.capture(),
                eq(Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)));
        assertThat(record.getValue()).contains(sha256(replacement), sha256(legacyToken)).doesNotContain(legacyToken);
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(record.getValue());
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, record.getValue())).willReturn(true);
        assertThat(jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(legacyToken), legacyToken)).isNull();
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, record.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "v1:family:hash", "v2:family:invalid-hash", "v2::", "v2: :"})
    void malformedOrBlankFamilyRegistryNeverRotates(String storedPrefix) {
        String token = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        String storedValue = storedPrefix.endsWith(":") ? storedPrefix + sha256(token) : storedPrefix;
        given(redisService.getValues("RT:" + EMAIL)).willReturn(storedValue);
        clearInvocations(redisService);

        assertThat(jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(token), token)).isNull();
        verify(redisService, never()).compareAndSetValues(anyString(),
                anyString(), anyString(),
                any(Duration.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void revocationAndRollbackRetainTheirLegacyBlankFamilyParsing(String family) {
        String record = RefreshTokenRecord.create(family, VALID_HASH, null).serializedValue();
        String key = "RT:" + EMAIL;
        String revocationKey = "RTR:" + EMAIL + ":" + family;
        Duration duration = Duration.ofMillis(REFRESH_TOKEN_EXPIRATION);
        given(redisService.getValues(key)).willReturn(record);
        given(redisService.compareAndDeleteValues(key, record)).willReturn(true);

        RefreshTokenRevocation.revoke(redisService, key, PRESENTED_TOKEN, family, VALID_HASH, revocationKey, duration);
        verify(redisService).compareAndDeleteValues(key, record);
        String restoredRecord = RefreshTokenRecord.create(family, sha256(PRESENTED_TOKEN), null).serializedValue();
        given(redisService.compareAndSetValues(key, record, restoredRecord, duration)).willReturn(true);

        assertThat(RefreshTokenRollback.rollback(redisService, key, PRESENTED_TOKEN, family,
                sha256(PRESENTED_TOKEN), VALID_HASH, revocationKey, duration)).isTrue();
        verify(redisService).compareAndSetValues(key, record, restoredRecord, duration);
    }

    @Test
    void lostRotationCasDeletesTheCompetingValueOnlyWithinTheSameFamily() {
        String token = jwtProvider.createRefreshToken(USER_ID, EMAIL);
        ArgumentCaptor<String> initialRecord = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValues(eq("RT:" + EMAIL), initialRecord.capture(),
                any(Duration.class));
        String family = parseClaims(token).get("sid", String.class);
        String winningRecord = RefreshTokenRecord.create(family, VALID_HASH, null).serializedValue();
        clearInvocations(redisService);
        given(redisService.getValues("RT:" + EMAIL)).willReturn(initialRecord.getValue(), winningRecord);
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, initialRecord.getValue())).willReturn(false);
        given(redisService.compareAndDeleteValues("RT:" + EMAIL, winningRecord)).willReturn(true);

        assertThat(jwtProvider.rotateRefreshToken(jwtProvider.getRefreshAuthentication(token), token)).isNull();
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, initialRecord.getValue());
        verify(redisService).compareAndDeleteValues("RT:" + EMAIL, winningRecord);
    }

    @Test
    void logoutDuringRollbackPreventsTheRestoredRecordFromSurviving() {
        String family = "test-family";
        String key = "RT:" + EMAIL;
        String revocationKey = "RTR:" + EMAIL + ":" + family;
        String replacement = RefreshTokenRecord.create(family, VALID_HASH, null).serializedValue();
        String previous = RefreshTokenRecord.create(family, sha256(PRESENTED_TOKEN), null).serializedValue();
        Duration duration = Duration.ofMillis(REFRESH_TOKEN_EXPIRATION);
        given(redisService.getValues(key)).willReturn(replacement);
        given(redisService.hasKey(revocationKey)).willReturn(false, true);
        given(redisService.compareAndSetValues(key, replacement, previous, duration)).willReturn(true);

        assertThat(RefreshTokenRollback.rollback(redisService, key, PRESENTED_TOKEN, family,
                sha256(PRESENTED_TOKEN), VALID_HASH, revocationKey, duration)).isFalse();
        verify(redisService).compareAndDeleteValues(key, previous);
    }

    @Test
    void expiredParsedRefreshCannotRotateRevokeOrRollback() {
        JwtAuthenticationResult expired = new JwtAuthenticationResult(USER_ID, EMAIL, null, 0L, "family");

        assertThatThrownBy(() -> jwtProvider.rotateRefreshToken(expired, PRESENTED_TOKEN))
                .isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> jwtProvider.revokeRefreshToken(expired, PRESENTED_TOKEN))
                .isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> jwtProvider.rollbackRefreshToken(expired, PRESENTED_TOKEN, "replacement"))
                .isInstanceOf(ExpiredJwtException.class);
        verifyNoInteractions(redisService);
    }

    @Test
    void accessAuthenticationFailsClosedWhenBlacklistStorageIsUnavailable() {
        String token = jwtProvider.createAccessToken(USER_ID, EMAIL, ROLE);
        given(redisService.hasKey("BL:" + sha256(token))).willThrow(new IllegalStateException("Redis unavailable"));

        assertThat(jwtProvider.getAccessAuthentication(token)).isNull();
    }

    @Test
    void refreshWithoutExpirationKeepsTheExistingAcceptanceContract() {
        String family = "test-family";
        String token = Jwts.builder().subject(EMAIL).claim("uid", USER_ID).claim(TOKEN_TYPE_CLAIM, "refresh")
                .claim("sid", family).signWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
        String record = RefreshTokenRecord.create(family, sha256(token), null).serializedValue();
        given(redisService.getValues("RT:" + EMAIL)).willReturn(record);
        given(redisService.compareAndSetValues(eq("RT:" + EMAIL),
                eq(record), anyString(),
                any(Duration.class))).willReturn(true);

        JwtAuthenticationResult authentication = jwtProvider.getRefreshAuthentication(token);
        assertThat(authentication.expiresAtMillis()).isNull();
        assertThat(jwtProvider.rotateRefreshToken(authentication, token)).isNotBlank();
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
                .claim(TOKEN_TYPE_CLAIM, "refresh")
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
