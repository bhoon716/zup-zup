package bhoon.sugang_helper.common.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

record RefreshTokenRecord(String tokenFamily, String tokenHash, String legacyTokenHash, String serializedValue) {

    private static final String VERSION = "v2";

    static RefreshTokenRecord create(String tokenFamily, String tokenHash, String legacyTokenHash) {
        String value = VERSION + ":" + tokenFamily + ":" + tokenHash;
        return new RefreshTokenRecord(tokenFamily, tokenHash, legacyTokenHash,
                legacyTokenHash == null ? value : value + ":" + legacyTokenHash);
    }

    static RefreshTokenRecord parse(String value) {
        if (value == null) {
            return null;
        }
        String[] values = value.split(":", 4);
        if ((values.length != 3 && values.length != 4) || !VERSION.equals(values[0])
                || !values[2].matches("[0-9a-f]{64}")
                || (values.length == 4 && !values[3].matches("[0-9a-f]{64}"))) {
            return null;
        }
        String legacyTokenHash = values.length == 4 ? values[3] : null;
        return new RefreshTokenRecord(values[1], values[2], legacyTokenHash, value);
    }

    boolean matchesLegacyToken(String hash) {
        return legacyTokenHash != null && constantTimeEquals(legacyTokenHash, hash);
    }

    static boolean constantTimeEquals(String first, String second) {
        return first != null && second != null && MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }
}
