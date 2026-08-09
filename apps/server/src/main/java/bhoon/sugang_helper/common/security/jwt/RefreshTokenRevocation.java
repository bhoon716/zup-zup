package bhoon.sugang_helper.common.security.jwt;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.constant.SecurityConstant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

final class RefreshTokenRevocation {

    private static final String RECORD_VERSION = "v2";

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

        RefreshTokenRecord storedRecord = parseRecord(storedValue);
        if (storedRecord == null) {
            if (constantTimeEquals(storedValue, presentedToken)) {
                redisService.compareAndDeleteValues(key, storedValue);
            }
            return;
        }

        boolean matchingLegacyToken = tokenFamily == null && storedRecord.legacyTokenHash() != null
                && constantTimeEquals(storedRecord.legacyTokenHash(), presentedTokenHash);
        if (matchingLegacyToken || storedRecord.tokenFamily().equals(tokenFamily)) {
            revokeFamily(redisService, key, storedRecord);
        }
    }

    private static void revokeFamily(RedisService redisService, String key, RefreshTokenRecord expectedRecord) {
        if (redisService.compareAndDeleteValues(key, expectedRecord.serializedValue())) {
            return;
        }

        String currentValue = redisService.getValues(key);
        RefreshTokenRecord currentRecord = parseRecord(currentValue);
        if (currentRecord != null && currentRecord.tokenFamily().equals(expectedRecord.tokenFamily())) {
            redisService.compareAndDeleteValues(key, currentRecord.serializedValue());
        }
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
