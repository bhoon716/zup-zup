package bhoon.sugang_helper.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminAuditContentFingerprintTest {

    @Test
    @DisplayName("답변 지문은 원문을 저장하지 않는 키 기반 HMAC-SHA-256 값이다.")
    void summarize_usesKeyedHmacFingerprint() {
        String content = "비밀 답변 \"alice@example.com\"";
        AdminAuditContentFingerprint first = new AdminAuditContentFingerprint("first-secret");
        AdminAuditContentFingerprint sameKey = new AdminAuditContentFingerprint("first-secret");
        AdminAuditContentFingerprint differentKey = new AdminAuditContentFingerprint("second-secret");

        ContentSummary summary = first.summarize(content);

        assertThat(summary.length()).isEqualTo(content.length());
        assertThat(summary.fingerprint()).startsWith("hmac-sha256:").doesNotContain(content);
        assertThat(sameKey.summarize(content).fingerprint()).isEqualTo(summary.fingerprint());
        assertThat(differentKey.summarize(content).fingerprint()).isNotEqualTo(summary.fingerprint());
    }
}
