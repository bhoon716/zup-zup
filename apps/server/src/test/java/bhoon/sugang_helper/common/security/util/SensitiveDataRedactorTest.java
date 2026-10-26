package bhoon.sugang_helper.common.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataRedactorTest {

    @Test
    void safeStackTraceCopiesCyclesAndSuppressedFailuresWithoutOriginalMessages() {
        Exception first = new IllegalStateException("first-secret");
        Exception second = new IllegalArgumentException("second-secret");
        first.initCause(second);
        second.initCause(first);
        first.addSuppressed(second);
        Throwable safe = SensitiveDataRedactor.safeStackTrace(first);
        assertThat(safe).isNotSameAs(first);
        assertThat(safe.getCause().getCause()).isSameAs(safe);
        assertThat(safe.getSuppressed()).containsExactly(safe.getCause());
        assertThat(safe.getMessage()).contains(IllegalStateException.class.getName()).doesNotContain("first-secret");
        assertThat(safe.getCause().getMessage()).contains(IllegalArgumentException.class.getName())
                .doesNotContain("second-secret");
        assertThat(safe.getStackTrace()).containsExactly(first.getStackTrace());
        assertThat(safe.getCause().getStackTrace()).containsExactly(second.getStackTrace());
        assertThat(first.getMessage()).isEqualTo("first-secret");
        assertThat(SensitiveDataRedactor.safeStackTrace(null)).isNull();
    }
}
