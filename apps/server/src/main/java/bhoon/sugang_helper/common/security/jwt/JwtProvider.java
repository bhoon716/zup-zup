package bhoon.sugang_helper.common.security.jwt;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.CLAIM_ROLE;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_TOKEN_FAMILY = "sid";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String REFRESH_RECORD_VERSION = "v2";

    private final RedisService redisService;
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;
    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;
    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessTokenExpiration, ACCESS_TOKEN_TYPE, null);
    }

    /**
     * 새 로그인용 refresh token을 만들고 원문이 아닌 family와 digest만 Redis에 저장합니다.
     */
    public String createRefreshToken(String email) {
        String tokenFamily = UUID.randomUUID().toString();
        String refreshToken = createRefreshToken(email, tokenFamily);
        redisService.setValues(refreshTokenKey(email), refreshTokenRecord(tokenFamily, refreshToken), refreshTokenDuration());
        return refreshToken;
    }

    private String createRefreshToken(String email, String tokenFamily) {
        return createToken(email, null, refreshTokenExpiration, REFRESH_TOKEN_TYPE, tokenFamily);
    }

    private String createToken(String email, String role, long expiration, String tokenType, String tokenFamily) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(key);

        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }
        if (tokenFamily != null) {
            builder.claim(CLAIM_TOKEN_FAMILY, tokenFamily);
        }

        return builder.compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        String role = claims.get(CLAIM_ROLE, String.class);

        List<SimpleGrantedAuthority> authorities = StringUtils.hasText(role)
                ? Collections.singletonList(new SimpleGrantedAuthority(role))
                : Collections.emptyList();

        return new UsernamePasswordAuthenticationToken(email, "", authorities);
    }

    public boolean validateToken(String token) {
        return validateToken(token, ACCESS_TOKEN_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN_TYPE);
    }

    private boolean validateToken(String token, String expectedTokenType) {
        try {
            Claims claims = parseClaims(token);
            if (!expectedTokenType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                return false;
            }
            if (ACCESS_TOKEN_TYPE.equals(expectedTokenType) && isBlacklisted(token)) {
                log.warn("[JWT] Blacklisted token usage detected: tokenFingerprint={}", tokenFingerprint(token));
                return false;
            }
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("[JWT] Empty token claims. exceptionType={}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("[JWT] Token validation failed. exceptionType={}", e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Refresh token을 단일 사용으로 회전합니다. 기존 raw registry는 한 번만 읽어 opaque record로 전환합니다.
     */
    public String rotateRefreshToken(String email, String presentedToken) {
        String key = refreshTokenKey(email);
        String storedValue = redisService.getValues(key);
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }

        RefreshTokenRecord storedRecord = parseRefreshTokenRecord(storedValue);
        if (storedRecord == null) {
            return rotateLegacyRefreshToken(key, storedValue, email, presentedToken);
        }

        String tokenFamily = getTokenFamily(presentedToken);
        if (tokenFamily == null && matchesLegacyToken(storedRecord, presentedToken)) {
            revokeRefreshFamily(key, storedRecord);
            return null;
        }
        if (!storedRecord.tokenFamily().equals(tokenFamily)) {
            return null;
        }
        if (!constantTimeEquals(storedRecord.tokenHash(), tokenHash(presentedToken))) {
            revokeRefreshFamily(key, storedRecord);
            return null;
        }

        String newRefreshToken = createRefreshToken(email, tokenFamily);
        String newRecord = refreshTokenRecord(tokenFamily, newRefreshToken, storedRecord.legacyTokenHash());
        if (redisService.compareAndSetValues(key, storedRecord.serializedValue(), newRecord, refreshTokenDuration())) {
            return newRefreshToken;
        }

        revokeRefreshFamily(key, storedRecord);
        return null;
    }

    /**
     * 현재 registry와 일치하는 refresh token만 로그아웃 시 삭제합니다.
     */
    public void revokeRefreshToken(String email, String presentedToken) {
        String key = refreshTokenKey(email);
        String storedValue = redisService.getValues(key);
        if (!StringUtils.hasText(storedValue)) {
            return;
        }

        RefreshTokenRecord storedRecord = parseRefreshTokenRecord(storedValue);
        if (storedRecord == null) {
            if (constantTimeEquals(storedValue, presentedToken)) {
                redisService.compareAndDeleteValues(key, storedValue);
            }
            return;
        }

        String tokenFamily = getTokenFamily(presentedToken);
        if (tokenFamily == null && matchesLegacyToken(storedRecord, presentedToken)) {
            revokeRefreshFamily(key, storedRecord);
            return;
        }
        if (storedRecord.tokenFamily().equals(tokenFamily)
                && constantTimeEquals(storedRecord.tokenHash(), tokenHash(presentedToken))) {
            redisService.compareAndDeleteValues(key, storedRecord.serializedValue());
        }
    }

    /**
     * Access token 원문 대신 SHA-256 식별자로 blacklist를 저장합니다.
     */
    public void blacklistAccessToken(String token) {
        long expiration = getExpiration(token);
        if (expiration > 0) {
            redisService.setValues(blacklistKey(token), SecurityConstant.LOGOUT_VALUE, Duration.ofMillis(expiration));
        }
    }

    private String rotateLegacyRefreshToken(String key, String storedToken, String email, String presentedToken) {
        if (!constantTimeEquals(storedToken, presentedToken)) {
            return null;
        }

        String tokenFamily = UUID.randomUUID().toString();
        String newRefreshToken = createRefreshToken(email, tokenFamily);
        String newRecord = refreshTokenRecord(tokenFamily, newRefreshToken, tokenHash(presentedToken));
        if (redisService.compareAndSetValues(key, storedToken, newRecord, refreshTokenDuration())) {
            return newRefreshToken;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = parseRefreshTokenRecord(currentValue);
        if (currentRecord != null && matchesLegacyToken(currentRecord, presentedToken)) {
            revokeRefreshFamily(key, currentRecord);
        }
        return null;
    }

    private void revokeRefreshFamily(String key, RefreshTokenRecord expectedRecord) {
        if (redisService.compareAndDeleteValues(key, expectedRecord.serializedValue())) {
            return;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = parseRefreshTokenRecord(currentValue);
        if (currentRecord != null && currentRecord.tokenFamily().equals(expectedRecord.tokenFamily())) {
            redisService.compareAndDeleteValues(key, currentRecord.serializedValue());
        }
    }

    private boolean isBlacklisted(String token) {
        // 배포 전 raw key는 access token TTL(최대 2시간) 동안만 남는다. 새 raw key는 절대 쓰지 않는다.
        return redisService.hasKey(blacklistKey(token))
                || redisService.hasKey(SecurityConstant.REDIS_BLACKLIST_PREFIX + token);
    }

    private String refreshTokenKey(String email) {
        return SecurityConstant.REDIS_REFRESH_TOKEN_PREFIX + email;
    }

    private String blacklistKey(String token) {
        return SecurityConstant.REDIS_BLACKLIST_PREFIX + tokenHash(token);
    }

    private Duration refreshTokenDuration() {
        return Duration.ofMillis(refreshTokenExpiration);
    }

    private String refreshTokenRecord(String tokenFamily, String token) {
        return refreshTokenRecord(tokenFamily, token, null);
    }

    private String refreshTokenRecord(String tokenFamily, String token, String legacyTokenHash) {
        String record = REFRESH_RECORD_VERSION + ":" + tokenFamily + ":" + tokenHash(token);
        return legacyTokenHash == null ? record : record + ":" + legacyTokenHash;
    }

    private RefreshTokenRecord parseRefreshTokenRecord(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String[] values = value.split(":", 4);
        if ((values.length != 3 && values.length != 4) || !REFRESH_RECORD_VERSION.equals(values[0])
                || !StringUtils.hasText(values[1]) || !values[2].matches("[0-9a-f]{64}")
                || (values.length == 4 && !values[3].matches("[0-9a-f]{64}"))) {
            return null;
        }
        String legacyTokenHash = values.length == 4 ? values[3] : null;
        return new RefreshTokenRecord(values[1], values[2], legacyTokenHash, value);
    }

    private String getTokenFamily(String token) {
        return parseClaims(token).get(CLAIM_TOKEN_FAMILY, String.class);
    }

    private boolean constantTimeEquals(String first, String second) {
        return first != null && second != null && MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }

    private boolean matchesLegacyToken(RefreshTokenRecord record, String token) {
        return record.legacyTokenHash() != null && constantTimeEquals(record.legacyTokenHash(), tokenHash(token));
    }

    private String tokenFingerprint(String token) {
        return tokenHash(token).substring(0, 8);
    }

    private String tokenHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstant.ACCESS_TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstant.TOKEN_PREFIX.length());
        }
        return null;
    }

    public long getExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long now = new Date().getTime();
        return expiration.getTime() - now;
    }

    private record RefreshTokenRecord(String tokenFamily, String tokenHash, String legacyTokenHash,
                                      String serializedValue) {
    }
}
