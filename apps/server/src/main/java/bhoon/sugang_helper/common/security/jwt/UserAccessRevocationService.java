package bhoon.sugang_helper.common.security.jwt;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserAccessRevocationService {

    private final RedisService redisService;
    private final Duration accessLifetime;

    public UserAccessRevocationService(RedisService redisService,
                                       @Value("${jwt.access.expiration}") long accessTokenExpiration) {
        this.redisService = redisService;
        this.accessLifetime = Duration.ofMillis(accessTokenExpiration);
    }

    public void revoke(Long userId) {
        redisService.setValues(key(userId), SecurityConstant.LOGOUT_VALUE, accessLifetime);
    }

    public boolean isRevoked(Long userId) {
        return redisService.hasKey(key(userId));
    }

    private String key(Long userId) {
        return SecurityConstant.REDIS_USER_ACCESS_REVOCATION_PREFIX + userId;
    }
}
