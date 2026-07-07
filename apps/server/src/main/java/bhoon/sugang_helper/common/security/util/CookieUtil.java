package bhoon.sugang_helper.common.security.util;

import org.springframework.http.ResponseCookie;
import static bhoon.sugang_helper.common.security.constant.SecurityConstant.REFRESH_TOKEN_COOKIE_NAME;

public class CookieUtil {

    private CookieUtil() {
    }

    public static ResponseCookie createCookie(String name, String value, long maxAge, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(name.equals(REFRESH_TOKEN_COOKIE_NAME))
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie deleteCookie(String name, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(name.equals(REFRESH_TOKEN_COOKIE_NAME))
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
