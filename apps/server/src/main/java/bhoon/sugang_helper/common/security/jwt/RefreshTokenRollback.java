package bhoon.sugang_helper.common.security.jwt;

import static bhoon.sugang_helper.common.security.jwt.RefreshTokenRecord.constantTimeEquals;

import bhoon.sugang_helper.common.redis.RedisService;
import java.time.Duration;

final class RefreshTokenRollback {

    private RefreshTokenRollback() {
    }

    static boolean rollback(RedisService redisService, String key, String presentedToken,
                            String tokenFamily, String presentedTokenHash, String replacementTokenHash,
                            String revocationKey, Duration duration) {
        if (tokenFamily != null && redisService.hasKey(revocationKey)) {
            return false;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = RefreshTokenRecord.parse(currentValue);
        if (currentRecord == null || !constantTimeEquals(currentRecord.tokenHash(), replacementTokenHash)) {
            return false;
        }

        String previousRecord = previousRecord(currentRecord, presentedToken, tokenFamily, presentedTokenHash);
        if (previousRecord == null) {
            return false;
        }
        if (!redisService.compareAndSetValues(key, currentRecord.serializedValue(), previousRecord, duration)) {
            return false;
        }
        if (tokenFamily != null && redisService.hasKey(revocationKey)) {
            redisService.compareAndDeleteValues(key, previousRecord);
            return false;
        }
        return true;
    }

    private static String previousRecord(RefreshTokenRecord currentRecord, String presentedToken, String tokenFamily,
                                         String presentedTokenHash) {
        if (tokenFamily == null) {
            return currentRecord.matchesLegacyToken(presentedTokenHash)
                    ? presentedToken
                    : null;
        }
        if (!currentRecord.tokenFamily().equals(tokenFamily)) {
            return null;
        }
        return RefreshTokenRecord.create(tokenFamily, presentedTokenHash, currentRecord.legacyTokenHash())
                .serializedValue();
    }
}
