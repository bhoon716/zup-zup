package bhoon.sugang_helper.identity.auth.service;

import static bhoon.sugang_helper.common.security.constant.SecurityConstant.IS_LOGGED_IN_COOKIE_NAME;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_MAX_AGE;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.SESSION_AUTHENTICATION_EXPIRES_AT;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.SESSION_AUTHENTICATION_USER_ID;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.dto.result.JwtAuthenticationResult;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;
import bhoon.sugang_helper.common.security.util.CookieUtil;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final UserIdentityQueryService userIdentityQuery;
    private final SecurityContextRepository securityContextRepository;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Transactional
    public String reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        String email = null;
        String newRefreshToken = null;
        JwtAuthenticationResult refreshAuthentication = null;
        try {
            refreshToken = resolveRefreshToken(request);

            if (!StringUtils.hasText(refreshToken)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰이 없습니다.");
            }

            refreshAuthentication = jwtProvider.getRefreshAuthentication(refreshToken);
            if (refreshAuthentication == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
            }

            email = refreshAuthentication.email();
            Long userId = refreshAuthentication.userId();
            if (userId == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN, "사용자 식별자가 없는 리프레시 토큰입니다.");
            }
            AuthenticatedUser user = userIdentityQuery.findAuthenticatedUser(userId, email)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));

            newRefreshToken = jwtProvider.rotateRefreshToken(refreshAuthentication, refreshToken);
            if (!StringUtils.hasText(newRefreshToken)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN, "저장된 리프레시 토큰과 일치하지 않습니다.");
            }

            String newAccessToken = jwtProvider.createAccessToken(user.id(), user.email(), user.roleKey());
            saveSessionAuthentication(request, response, newAccessToken);
            log.info("[Auth] Refreshed authenticated session. emailMasked={}", SensitiveDataRedactor.maskEmail(email));

            addRefreshTokenCookie(response, newRefreshToken);

            return newAccessToken;
        } catch (Exception e) {
            if (StringUtils.hasText(refreshToken) && StringUtils.hasText(email)
                    && StringUtils.hasText(newRefreshToken)) {
                compensateRefreshRotation(response, refreshAuthentication, refreshToken, newRefreshToken);
            }
            if (isDefinitiveAuthFailure(e)) {
                deleteRefreshTokenCookie(response);
            }
            throw e;
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        String accessToken = jwtProvider.resolveToken(request);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (StringUtils.hasText(refreshToken)) {
            JwtAuthenticationResult refreshAuthentication = jwtProvider.getRefreshAuthentication(refreshToken);
            if (refreshAuthentication != null && refreshAuthentication.userId() != null) {
                jwtProvider.revokeRefreshToken(refreshAuthentication, refreshToken);
            }
        }

        if (StringUtils.hasText(accessToken)) {
            JwtAuthenticationResult accessAuthentication = jwtProvider.getAccessAuthentication(accessToken);
            if (accessAuthentication != null) {
                jwtProvider.blacklistAccessToken(accessToken, accessAuthentication);
            }
        }

        deleteRefreshTokenCookie(response);
    }




    public void saveSessionAuthentication(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        JwtAuthenticationResult authentication = jwtProvider.parseIssuedAccessToken(accessToken);
        Long userId = authentication.userId();
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "사용자 식별자가 없는 액세스 토큰입니다.");
        }
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.removeAttribute("ACCESS_TOKEN");
            existingSession.removeAttribute("REFRESH_TOKEN");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        jwtProvider.requireNotExpired(authentication);
        context.setAuthentication(authentication.authentication());
        securityContextRepository.saveContext(context, request, response);

        HttpSession authenticatedSession = request.getSession(false);
        if (authenticatedSession != null) {
            long expiresAt = System.currentTimeMillis() + jwtProvider.getExpiration(authentication);
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

    private void compensateRefreshRotation(HttpServletResponse response, JwtAuthenticationResult authentication,
                                           String refreshToken,
                                           String newRefreshToken) {
        String email = authentication.email();
        boolean restored = false;
        try {
            restored = jwtProvider.rollbackRefreshToken(authentication, refreshToken, newRefreshToken);
        } catch (RuntimeException rollbackException) {
            log.error("[Auth] Refresh token rollback failed. emailMasked={}",
                    SensitiveDataRedactor.maskEmail(email), rollbackException);
        }
        if (restored) {
            return;
        }

        log.error("[Auth] Refresh token rollback was not confirmed. Clearing stale auth cookies. emailMasked={}",
                SensitiveDataRedactor.maskEmail(email));
        try {
            deleteRefreshTokenCookie(response);
        } catch (RuntimeException cookieException) {
            log.error("[Auth] Failed to clear stale auth cookies after refresh rollback failure. emailMasked={}",
                    SensitiveDataRedactor.maskEmail(email), cookieException);
        }
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtil.deleteCookie(REFRESH_TOKEN_COOKIE_NAME, refreshCookieSecure);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ResponseCookie isLoggedInCookie = CookieUtil.deleteCookie(IS_LOGGED_IN_COOKIE_NAME, refreshCookieSecure);
        response.addHeader(HttpHeaders.SET_COOKIE, isLoggedInCookie.toString());
    }

    private boolean isDefinitiveAuthFailure(Exception exception) {
        if (exception instanceof JwtException) {
            return true;
        }
        if (!(exception instanceof CustomException customException)) {
            return false;
        }
        HttpStatus status = customException.getErrorCode().getStatus();
        return status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN;
    }
}
