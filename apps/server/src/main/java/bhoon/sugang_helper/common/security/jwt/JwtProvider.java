package bhoon.sugang_helper.common.security.jwt;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.CLAIM_ROLE;
import static bhoon.sugang_helper.common.security.jwt.RefreshTokenRecord.constantTimeEquals;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import bhoon.sugang_helper.common.security.dto.result.JwtAuthenticationResult;
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
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public String createAccessToken(Long userId, String email, String role) {
        return createToken(userId, email, role, accessTokenExpiration, ACCESS_TOKEN_TYPE, null);
    }




    public String createRefreshToken(Long userId, String email) {
        String tokenFamily = UUID.randomUUID().toString();
        String refreshToken = createRefreshToken(userId, email, tokenFamily);
        redisService.setValues(refreshTokenKey(email), refreshTokenRecord(tokenFamily, refreshToken, null),
                refreshTokenDuration());
        return refreshToken;
    }

    private String createRefreshToken(Long userId, String email, String tokenFamily) {
        return createToken(userId, email, null, refreshTokenExpiration, REFRESH_TOKEN_TYPE, tokenFamily);
    }

    private String createToken(Long userId, String email, String role, long expiration, String tokenType,
                               String tokenFamily) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(SecurityConstant.CLAIM_USER_ID, userId)
                .signWith(key);

        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }
        if (tokenFamily != null) {
            builder.claim(CLAIM_TOKEN_FAMILY, tokenFamily);
        }

        return builder.compact();
    }

    public JwtAuthenticationResult parseIssuedAccessToken(String token) {
        return authenticationResult(parseClaims(token));
    }

    private JwtAuthenticationResult authenticationResult(Claims claims) {
        String email = claims.getSubject();
        String role = claims.get(CLAIM_ROLE, String.class);

        List<SimpleGrantedAuthority> authorities = Optional.ofNullable(role)
                .filter(StringUtils::hasText)
                .<List<SimpleGrantedAuthority>>map(value -> Collections.singletonList(new SimpleGrantedAuthority(value)))
                .orElseGet(Collections::emptyList);

        var authentication = new UsernamePasswordAuthenticationToken(email, "", authorities);
        Date expiration = claims.getExpiration();
        return new JwtAuthenticationResult(getUserId(claims), authentication.getName(), authentication,
                expiration == null ? null : expiration.getTime(), claims.get(CLAIM_TOKEN_FAMILY, String.class));
    }

    public JwtAuthenticationResult getAccessAuthentication(String token) {
        return validateToken(token, ACCESS_TOKEN_TYPE);
    }

    public JwtAuthenticationResult getRefreshAuthentication(String token) {
        return validateToken(token, REFRESH_TOKEN_TYPE);
    }

    private JwtAuthenticationResult validateToken(String token, String expectedTokenType) {
        Claims claims;
        try {
            claims = parseClaims(token);
            if (!expectedTokenType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class)) || getUserId(claims) == null) {
                return null;
            }
            if (ACCESS_TOKEN_TYPE.equals(expectedTokenType) && isBlacklisted(token)) {
                log.warn("[JWT] Blacklisted token usage detected: tokenFingerprint={}", tokenFingerprint(token));
                return null;
            }
            return authenticationResult(claims);
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Expired JWT token: tokenFingerprint={}", tokenFingerprint(token));
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("[JWT] Empty token claims. exceptionType={}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("[JWT] Token validation failed. exceptionType={}", e.getClass().getSimpleName());
        }
        return null;
    }




    public String rotateRefreshToken(JwtAuthenticationResult authentication, String presentedToken) {
        requireNotExpired(authentication);
        String email = authentication.email();
        Long userId = authentication.userId();
        String tokenFamily = authentication.tokenFamily();
        if (tokenFamily != null && isRefreshFamilyRevoked(email, tokenFamily)) {
            return null;
        }

        String key = refreshTokenKey(email);
        String storedValue = redisService.getValues(key);
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }

        RefreshTokenRecord storedRecord = parseRefreshTokenRecord(storedValue);
        if (storedRecord == null) {
            return rotateLegacyRefreshToken(key, storedValue, userId, email, presentedToken);
        }

        if (tokenFamily == null && storedRecord.matchesLegacyToken(tokenHash(presentedToken))) {
            RefreshTokenRevocation.revokeFamily(redisService, key, storedRecord);
            return null;
        }
        if (!storedRecord.tokenFamily().equals(tokenFamily)) {
            return null;
        }
        if (!constantTimeEquals(storedRecord.tokenHash(), tokenHash(presentedToken))) {
            RefreshTokenRevocation.revokeFamily(redisService, key, storedRecord);
            return null;
        }

        String newRefreshToken = createRefreshToken(userId, email, tokenFamily);
        String newRecord = refreshTokenRecord(tokenFamily, newRefreshToken, storedRecord.legacyTokenHash());
        if (redisService.compareAndSetValues(key, storedRecord.serializedValue(), newRecord, refreshTokenDuration())) {
            if (isRefreshFamilyRevoked(email, tokenFamily)) {
                redisService.compareAndDeleteValues(key, newRecord);
                return null;
            }
            return newRefreshToken;
        }

        RefreshTokenRevocation.revokeFamily(redisService, key, storedRecord);
        return null;
    }





    public boolean rollbackRefreshToken(JwtAuthenticationResult authentication, String presentedToken,
                                        String replacementToken) {
        requireNotExpired(authentication);
        String email = authentication.email();
        String tokenFamily = authentication.tokenFamily();
        return RefreshTokenRollback.rollback(redisService, refreshTokenKey(email), presentedToken,
                tokenFamily, tokenHash(presentedToken), tokenHash(replacementToken),
                refreshFamilyRevocationKey(email, tokenFamily), refreshTokenDuration());
    }




    public void revokeRefreshToken(JwtAuthenticationResult authentication, String presentedToken) {
        requireNotExpired(authentication);
        String email = authentication.email();
        String tokenFamily = authentication.tokenFamily();
        RefreshTokenRevocation.revoke(redisService, refreshTokenKey(email), presentedToken, tokenFamily,
                tokenHash(presentedToken), refreshFamilyRevocationKey(email, tokenFamily), refreshTokenDuration());
    }




    public void revokeAllRefreshTokens(String email) {
        redisService.deleteValues(refreshTokenKey(email));
    }




    public void blacklistAccessToken(String token, JwtAuthenticationResult authentication) {
        long expiration = getExpiration(authentication);
        if (expiration > 0) {
            redisService.setValues(blacklistKey(token), SecurityConstant.LOGOUT_VALUE, Duration.ofMillis(expiration));
        }
    }

    private String rotateLegacyRefreshToken(String key, String storedToken, Long userId, String email,
                                            String presentedToken) {
        if (!constantTimeEquals(storedToken, presentedToken)) {
            return null;
        }

        String tokenFamily = UUID.randomUUID().toString();
        String newRefreshToken = createRefreshToken(userId, email, tokenFamily);
        String newRecord = refreshTokenRecord(tokenFamily, newRefreshToken, tokenHash(presentedToken));
        if (redisService.compareAndSetValues(key, storedToken, newRecord, refreshTokenDuration())) {
            return newRefreshToken;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = parseRefreshTokenRecord(currentValue);
        if (currentRecord != null && currentRecord.matchesLegacyToken(tokenHash(presentedToken))) {
            RefreshTokenRevocation.revokeFamily(redisService, key, currentRecord);
        }
        return null;
    }

    private boolean isRefreshFamilyRevoked(String email, String tokenFamily) {
        return redisService.hasKey(refreshFamilyRevocationKey(email, tokenFamily));
    }

    private boolean isBlacklisted(String token) {

        return redisService.hasKey(blacklistKey(token))
                || redisService.hasKey(SecurityConstant.REDIS_BLACKLIST_PREFIX + token);
    }

    private String refreshTokenKey(String email) {
        return SecurityConstant.REDIS_REFRESH_TOKEN_PREFIX + email;
    }

    private String refreshFamilyRevocationKey(String email, String tokenFamily) {
        return SecurityConstant.REDIS_REFRESH_TOKEN_REVOCATION_PREFIX + email + ":" + tokenFamily;
    }

    private String blacklistKey(String token) {
        return SecurityConstant.REDIS_BLACKLIST_PREFIX + tokenHash(token);
    }

    private Duration refreshTokenDuration() {
        return Duration.ofMillis(refreshTokenExpiration);
    }

    private String refreshTokenRecord(String tokenFamily, String token, String legacyTokenHash) {
        return RefreshTokenRecord.create(tokenFamily, tokenHash(token), legacyTokenHash).serializedValue();
    }

    private RefreshTokenRecord parseRefreshTokenRecord(String value) {
        RefreshTokenRecord record = RefreshTokenRecord.parse(value);
        return record != null && StringUtils.hasText(record.tokenFamily()) ? record : null;
    }

    private Long getUserId(Claims claims) {
        return Optional.ofNullable(claims.get(SecurityConstant.CLAIM_USER_ID, Number.class))
                .map(Number::longValue)
                .orElse(null);
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

    public void requireNotExpired(JwtAuthenticationResult authentication) {
        if (authentication.expiresAtMillis() != null && System.currentTimeMillis() > authentication.expiresAtMillis()) {
            throw new ExpiredJwtException(null, null, "JWT token has expired");
        }
    }

    public long getExpiration(JwtAuthenticationResult authentication) {
        requireNotExpired(authentication);
        return authentication.expiresAtMillis() - System.currentTimeMillis();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstant.ACCESS_TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstant.TOKEN_PREFIX.length());
        }
        return null;
    }

}
