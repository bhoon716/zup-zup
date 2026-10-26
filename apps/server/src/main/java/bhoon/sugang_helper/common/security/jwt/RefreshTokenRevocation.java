package bhoon.sugang_helper.common.security.jwt;

import static bhoon.sugang_helper.common.security.jwt.RefreshTokenRecord.constantTimeEquals;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import java.time.Duration;

final class RefreshTokenRevocation {

    private RefreshTokenRevocation() {
    }

    static void revoke(RedisService redisService, String key, String presentedToken, String tokenFamily,
                       String presentedTokenHash, String revocationKey, Duration duration) {
        if (tokenFamily != null) {
            redisService.setValues(revocationKey, SecurityConstant.LOGOUT_VALUE, duration);
        }

        String storedValue = redisService.getValues(key);
        if (storedValue == null || storedValue.isBlank()) {
            return;
        }

        RefreshTokenRecord storedRecord = RefreshTokenRecord.parse(storedValue);
        if (storedRecord == null) {
            if (constantTimeEquals(storedValue, presentedToken)) {
                redisService.compareAndDeleteValues(key, storedValue);
            }
            return;
        }

        boolean matchingLegacyToken = tokenFamily == null && storedRecord.matchesLegacyToken(presentedTokenHash);
        if (matchingLegacyToken || storedRecord.tokenFamily().equals(tokenFamily)) {
            revokeFamily(redisService, key, storedRecord);
        }
    }

    static void revokeFamily(RedisService redisService, String key, RefreshTokenRecord expectedRecord) {
        if (redisService.compareAndDeleteValues(key, expectedRecord.serializedValue())) {
            return;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = RefreshTokenRecord.parse(currentValue);
        if (currentRecord != null && currentRecord.tokenFamily().equals(expectedRecord.tokenFamily())) {
            redisService.compareAndDeleteValues(key, currentRecord.serializedValue());
        }
    }

}
