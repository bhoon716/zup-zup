package bhoon.sugang_helper.common.security.jwt;

import bhoon.sugang_helper.common.redis.RedisService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

final class RefreshTokenRollback {

    private static final String RECORD_VERSION = "v2";

    private RefreshTokenRollback() {
    }

    static boolean rollback(RedisService redisService, String key, String presentedToken, String replacementToken,
                            String tokenFamily, String presentedTokenHash, String replacementTokenHash,
                            String revocationKey, Duration duration) {
        if (tokenFamily != null && redisService.hasKey(revocationKey)) {
            return false;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = parseRecord(currentValue);
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
            return currentRecord.legacyTokenHash() != null
                    && constantTimeEquals(currentRecord.legacyTokenHash(), presentedTokenHash)
                    ? presentedToken
                    : null;
        }
        if (!currentRecord.tokenFamily().equals(tokenFamily)) {
            return null;
        }
        String previousRecord = RECORD_VERSION + ":" + tokenFamily + ":" + presentedTokenHash;
        return currentRecord.legacyTokenHash() == null
                ? previousRecord
                : previousRecord + ":" + currentRecord.legacyTokenHash();
    }

    private static RefreshTokenRecord parseRecord(String value) {
        if (value == null) {
            return null;
        }
        String[] values = value.split(":", 4);
        if ((values.length != 3 && values.length != 4) || !RECORD_VERSION.equals(values[0])
                || !values[2].matches("[0-9a-f]{64}")
                || (values.length == 4 && !values[3].matches("[0-9a-f]{64}"))) {
            return null;
        }
        String legacyTokenHash = values.length == 4 ? values[3] : null;
        return new RefreshTokenRecord(values[1], values[2], legacyTokenHash, value);
    }

    private static boolean constantTimeEquals(String first, String second) {
        return first != null && second != null && MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }

    private record RefreshTokenRecord(String tokenFamily, String tokenHash, String legacyTokenHash,
                                      String serializedValue) {
    }
}
