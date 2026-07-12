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

    private final RedisService redisService;
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;
    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;
    private SecretKey key;
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessTokenExpiration, ACCESS_TOKEN_TYPE);
    }

    public String createRefreshToken(String email) {
        String refreshToken = createToken(email, null, refreshTokenExpiration, REFRESH_TOKEN_TYPE);
        redisService.setValues("RT:" + email, refreshToken, Duration.ofMillis(refreshTokenExpiration));
        return refreshToken;
    }

    private String createToken(String email, String role, long expiration, String tokenType) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(key);

        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
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
            if (redisService.hasKey(SecurityConstant.REDIS_BLACKLIST_PREFIX + token)) {
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

    private String tokenFingerprint(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 4);
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
        return (expiration.getTime() - now);
    }
}
