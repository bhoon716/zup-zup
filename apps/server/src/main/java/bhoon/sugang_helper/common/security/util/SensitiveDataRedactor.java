package bhoon.sugang_helper.common.security.util;

import bhoon.sugang_helper.common.error.CustomException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class SensitiveDataRedactor {

    private static final Pattern SENSITIVE_PATH_SEGMENT = Pattern.compile(
            "(?i)(/(?:token|tokens|authorization|secret|password|cookie)/)[^/?]+");

    private SensitiveDataRedactor() {
        throw new IllegalStateException("Utility class");
    }

    public static String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "<none>";
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to fingerprint sensitive data", e);
        }
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<none>";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "<masked>";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    public static String failureCode(Throwable throwable) {
        if (throwable instanceof CustomException customException) {
            return customException.getErrorCode().getCode();
        }
        return "UNEXPECTED";
    }

    public static String exceptionType(Throwable throwable) {
        return throwable == null ? "Unknown" : throwable.getClass().getSimpleName();
    }

    public static String redactPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return SENSITIVE_PATH_SEGMENT.matcher(path).replaceAll("$1[REDACTED]");
    }
}
