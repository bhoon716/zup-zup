package bhoon.sugang_helper.notification.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Canonical delivery key를 provider 제약에 맞는 짧고 불투명한 key로 변환합니다.
 */
public final class NotificationDeliveryIdempotencyKey {

    private static final int PROVIDER_KEY_LENGTH = 22;

    private NotificationDeliveryIdempotencyKey() {
        throw new IllegalStateException("Utility class");
    }

    public static String providerKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
                    .substring(0, PROVIDER_KEY_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for notification idempotency keys", exception);
        }
    }
}
