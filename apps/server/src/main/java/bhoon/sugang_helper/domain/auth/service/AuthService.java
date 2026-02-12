package bhoon.sugang_helper.domain.auth.service;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.LOGOUT_VALUE;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REDIS_BLACKLIST_PREFIX;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REDIS_REFRESH_TOKEN_PREFIX;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_MAX_AGE;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final UserRepository userRepository;

    @Transactional
    public String reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);

        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Refresh Token not found");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Invalid Refresh Token");
        }

        String email = jwtProvider.getAuthentication(refreshToken).getName();
        String savedToken = redisService.getValues(REDIS_REFRESH_TOKEN_PREFIX + email);

        if (!refreshToken.equals(savedToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Token Unmatched");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));

        String newAccessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRoleKey());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail());

        // Update Session (BFF 패턴: 서버 세션에 최신 토큰 동기화)
        // getSession(true)를 사용하여 세션이 만료되었더라도 새 세션을 생성하도록 보장합니다.
        jakarta.servlet.http.HttpSession session = request.getSession(true);
        session.setAttribute("ACCESS_TOKEN", newAccessToken);
        session.setAttribute("REFRESH_TOKEN", newRefreshToken);
        log.info("[Auth] Session tokens updated for email: {}", email);

        // Update Cookie
        addRefreshTokenCookie(response, newRefreshToken);

        return newAccessToken;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        String accessToken = jwtProvider.resolveToken(request);

        // 세션 무효화 (BFF 세션 제거)
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            if (accessToken == null) {
                accessToken = (String) session.getAttribute("ACCESS_TOKEN");
            }
            if (refreshToken == null) {
                refreshToken = (String) session.getAttribute("REFRESH_TOKEN");
            }
            session.invalidate();
        }

        if (StringUtils.hasText(refreshToken) && jwtProvider.validateToken(refreshToken)) {
            String email = jwtProvider.getAuthentication(refreshToken).getName();
            redisService.deleteValues(REDIS_REFRESH_TOKEN_PREFIX + email);
        }

        if (StringUtils.hasText(accessToken) && jwtProvider.validateToken(accessToken)) {
            long expiration = jwtProvider.getExpiration(accessToken);
            redisService.setValues(REDIS_BLACKLIST_PREFIX + accessToken, LOGOUT_VALUE, Duration.ofMillis(expiration));
        }

        deleteRefreshTokenCookie(response);
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // 로컬 테스트를 위해 false, 운영 환경에서는 true 권장
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("Lax") // CSRF 방어
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
