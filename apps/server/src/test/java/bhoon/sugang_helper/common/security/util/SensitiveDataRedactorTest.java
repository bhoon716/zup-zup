package bhoon.sugang_helper.common.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveDataRedactorTest {

    @Test
    @DisplayName("안전한 스택은 원문 메시지 없이 순환과 억제된 예외를 복사한다")
    void safeStackTraceCopiesCyclesAndSuppressedFailuresWithoutOriginalMessages() {
        // given
        Exception first = new IllegalStateException("first-secret");
        Exception second = new IllegalArgumentException("second-secret");
        first.initCause(second);
        second.initCause(first);
        first.addSuppressed(second);

        // when
        Throwable safe = SensitiveDataRedactor.safeStackTrace(first);

        // then
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
