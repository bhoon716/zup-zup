package bhoon.sugang_helper.common.security.constant;

public class SecurityConstant {

    public static final String ACCESS_TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String CLAIM_ROLE = "role";
    public static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 14 * 24 * 60 * 60; // 2주

    public static final String REDIS_REFRESH_TOKEN_PREFIX = "RT:";
    public static final String REDIS_BLACKLIST_PREFIX = "BL:";
    public static final String LOGOUT_VALUE = "logout";
    public static final String CLAIM_EMAIL = "email";

    private SecurityConstant() {
    }
}
