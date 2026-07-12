package bhoon.sugang_helper.auth.application;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.IS_LOGGED_IN_COOKIE_NAME;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_MAX_AGE;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.SESSION_AUTHENTICATION_EXPIRES_AT;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.SESSION_AUTHENTICATION_USER_ID;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.common.security.util.CookieUtil;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Transactional
    public String reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);

        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰이 없습니다.");
        }

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtProvider.getAuthentication(refreshToken).getName();
        Long userId = jwtProvider.getUserId(refreshToken);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "사용자 식별자가 없는 리프레시 토큰입니다.");
        }
        User user = userRepository.findByIdAndEmailAndDeletedAtIsNull(userId, email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));

        String newRefreshToken = jwtProvider.rotateRefreshToken(email, refreshToken);
        if (!StringUtils.hasText(newRefreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleKey());
        saveSessionAuthentication(request, response, newAccessToken);
        log.info("[Auth] Refreshed authenticated session. emailMasked={}", SensitiveDataRedactor.maskEmail(email));

        addRefreshTokenCookie(response, newRefreshToken);

        return newAccessToken;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        String accessToken = jwtProvider.resolveToken(request);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (StringUtils.hasText(refreshToken) && jwtProvider.validateRefreshToken(refreshToken)) {
            String email = jwtProvider.getAuthentication(refreshToken).getName();
            Long userId = jwtProvider.getUserId(refreshToken);
            if (userId != null) {
                jwtProvider.revokeRefreshToken(email, refreshToken);
            }
        }

        if (StringUtils.hasText(accessToken) && jwtProvider.validateToken(accessToken)) {
            jwtProvider.blacklistAccessToken(accessToken);
        }

        deleteRefreshTokenCookie(response);
    }

    /**
     * Redis-backed HTTP session에는 JWT 원문 대신 인증 주체와 권한만 저장합니다.
     */
    public void saveSessionAuthentication(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        Long userId = jwtProvider.getUserId(accessToken);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "사용자 식별자가 없는 액세스 토큰입니다.");
        }
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.removeAttribute("ACCESS_TOKEN");
            existingSession.removeAttribute("REFRESH_TOKEN");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(jwtProvider.getAuthentication(accessToken));
        securityContextRepository.saveContext(context, request, response);

        HttpSession authenticatedSession = request.getSession(false);
        if (authenticatedSession != null) {
            long expiresAt = System.currentTimeMillis() + jwtProvider.getExpiration(accessToken);
            authenticatedSession.setAttribute(SESSION_AUTHENTICATION_EXPIRES_AT, expiresAt);
            authenticatedSession.setAttribute(SESSION_AUTHENTICATION_USER_ID, userId);
        }
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
        ResponseCookie cookie = CookieUtil.createCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken,
                REFRESH_TOKEN_COOKIE_MAX_AGE, refreshCookieSecure);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ResponseCookie isLoggedInCookie = CookieUtil.createCookie(IS_LOGGED_IN_COOKIE_NAME, "true",
                REFRESH_TOKEN_COOKIE_MAX_AGE, refreshCookieSecure);
        response.addHeader(HttpHeaders.SET_COOKIE, isLoggedInCookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtil.deleteCookie(REFRESH_TOKEN_COOKIE_NAME, refreshCookieSecure);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ResponseCookie isLoggedInCookie = CookieUtil.deleteCookie(IS_LOGGED_IN_COOKIE_NAME, refreshCookieSecure);
        response.addHeader(HttpHeaders.SET_COOKIE, isLoggedInCookie.toString());
    }
}
