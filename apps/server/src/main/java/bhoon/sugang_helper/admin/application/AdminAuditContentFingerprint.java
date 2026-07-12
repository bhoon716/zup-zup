package bhoon.sugang_helper.admin.application;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 원문을 복원하거나 짧은 문구를 대입 확인할 수 없도록 키 기반 HMAC 지문을 만듭니다.
 */
@Component
public class AdminAuditContentFingerprint {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKey;

    public AdminAuditContentFingerprint(@Value("${app.admin-audit.fingerprint-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("관리자 감사 로그 HMAC secret이 비어 있습니다.");
        }
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public ContentSummary summarize(String content) {
        String safeContent = content == null ? "" : content;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            String fingerprint = "hmac-sha256:" + HexFormat.of().formatHex(
                    mac.doFinal(safeContent.getBytes(StandardCharsets.UTF_8)));
            return new ContentSummary(safeContent.length(), fingerprint);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA-256 지문을 생성할 수 없습니다.", exception);
        }
    }
}
