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
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰이 없습니다.");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtProvider.getAuthentication(refreshToken).getName();
        String savedToken = redisService.getValues(REDIS_REFRESH_TOKEN_PREFIX + email);

        if (!refreshToken.equals(savedToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));

        String newAccessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRoleKey());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail());

        // 세션이 없으면 새로 생성해 최신 토큰을 동기화한다.
        HttpSession session = request.getSession(true);
        session.setAttribute("ACCESS_TOKEN", newAccessToken);
        session.setAttribute("REFRESH_TOKEN", newRefreshToken);
        log.info("[Auth] Refreshed session token. email={}", email);

        addRefreshTokenCookie(response, newRefreshToken);

        return newAccessToken;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        String accessToken = jwtProvider.resolveToken(request);

        HttpSession session = request.getSession(false);
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
        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // 운영 환경에서는 반드시 활성화해야 한다.
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
