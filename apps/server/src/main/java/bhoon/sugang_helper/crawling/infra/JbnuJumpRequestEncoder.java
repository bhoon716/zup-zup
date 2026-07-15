package bhoon.sugang_helper.crawling.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * JUMP의 BASIC 요청 형식(DataMap 직렬화 후 AES-CBC)을 생성합니다.
 */
final class JbnuJumpRequestEncoder {

    private static final String KEY_MARK = "전북대학교 JUMP시스템";
    private static final String DATA_MAP_PREFIX = "@d1#";

    private JbnuJumpRequestEncoder() {
    }

    static String encode(Map<String, String> values) {
        try {
            String store = derivedStore();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(store.getBytes(StandardCharsets.UTF_8), "AES"),
                    new IvParameterSpec(iv(store)));
            String encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(serialize(values)
                    .getBytes(StandardCharsets.UTF_8)));
            return "xb_data=" + encodeComponent(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode JUMP request", e);
        }
    }

    static String derivedStore() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(KEY_MARK.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.substring(5, 37);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static byte[] iv(String store) {
        return new StringBuilder(store).reverse().substring(0, 16).getBytes(StandardCharsets.UTF_8);
    }

    private static String serialize(Map<String, String> values) {
        StringBuilder payload = new StringBuilder();
        values.forEach((key, value) -> payload.append(encodeComponent(DATA_MAP_PREFIX + key))
                .append('=')
                .append(encodeComponent(value == null ? "" : value))
                .append('&'));
        payload.append(encodeComponent("@d#"))
                .append('=')
                .append(encodeComponent(DATA_MAP_PREFIX))
                .append('&')
                .append(encodeComponent(DATA_MAP_PREFIX))
                .append('=')
                .append(encodeComponent("dmReqKey"))
                .append('&')
                .append(encodeComponent(DATA_MAP_PREFIX + "tp"))
                .append('=')
                .append(encodeComponent("dm"));
        return payload.toString();
    }

    private static String encodeComponent(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length);
        for (byte byteValue : bytes) {
            int unsigned = byteValue & 0xff;
            if (isComponentSafe(unsigned)) {
                encoded.append((char) unsigned);
            } else {
                encoded.append('%')
                        .append(Character.toUpperCase(Character.forDigit(unsigned >>> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(unsigned & 0x0f, 16)));
            }
        }
        return encoded.toString();
    }

    private static boolean isComponentSafe(int value) {
        return value >= 'a' && value <= 'z'
                || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9'
                || value == '-' || value == '_' || value == '.' || value == '!'
                || value == '~' || value == '*' || value == '\'' || value == '(' || value == ')';
    }
}
