package bhoon.sugang_helper.common.security.util;

import bhoon.sugang_helper.common.error.CustomException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
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

    public static Throwable safeStackTrace(Throwable original) {
        if (original == null) {
            return null;
        }
        Map<Throwable, Throwable> copies = new IdentityHashMap<>();
        Queue<Throwable> pending = new ArrayDeque<>();
        Throwable result = copyStackTrace(original);
        copies.put(original, result);
        pending.add(original);
        while (!pending.isEmpty()) {
            Throwable source = pending.remove();
            Throwable target = copies.get(source);
            Throwable cause = source.getCause();
            if (cause != null && cause != source) {
                target.initCause(copyStackTrace(cause, copies, pending));
            }
            for (Throwable suppressed : source.getSuppressed()) {
                if (suppressed != source) {
                    target.addSuppressed(copyStackTrace(suppressed, copies, pending));
                }
            }
        }
        return result;
    }

    private static Throwable copyStackTrace(Throwable source, Map<Throwable, Throwable> copies,
                                             Queue<Throwable> pending) {
        Throwable copy = copies.get(source);
        if (copy == null) {
            copy = copyStackTrace(source);
            copies.put(source, copy);
            pending.add(source);
        }
        return copy;
    }

    private static Throwable copyStackTrace(Throwable source) {
        Throwable copy = new Throwable(source.getClass().getName() + " (원문 메시지 생략)");
        copy.setStackTrace(source.getStackTrace());
        return copy;
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
