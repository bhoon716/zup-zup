package bhoon.sugang_helper.common.security.jwt;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.CLAIM_ROLE;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
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

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    private final RedisService redisService;
    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessTokenExpiration);
    }

    public String createRefreshToken(String email) {
        String refreshToken = createToken(email, null, refreshTokenExpiration);
        redisService.setValues("RT:" + email, refreshToken, Duration.ofMillis(refreshTokenExpiration));
        return refreshToken;
    }

    private String createToken(String email, String role, long expiration) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
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
        try {
            parseClaims(token);
            if (redisService.hasKey(SecurityConstant.REDIS_BLACKLIST_PREFIX + token)) {
                log.warn("[JWT] Blacklisted token usage attempted: {}", token);
                return false;
            }
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException e) {
            log.error("[JWT] Invalid signature or malformed token: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("[JWT] Expired token: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("[JWT] Unsupported token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[JWT] Token claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[JWT] Token validation failed: {}", e.getMessage());
        }
        return false;
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
