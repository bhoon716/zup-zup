package bhoon.sugang_helper.common.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class PageableGuardTest {

    @Test
    void rejectsOversizedPage() {
        assertThatThrownBy(() -> PageableGuard.requireBounded(PageRequest.of(0, 101)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void rejectsExcessiveOffset() {
        assertThatThrownBy(() -> PageableGuard.requireBounded(PageRequest.of(101, 100)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void acceptsBoundaryPage() {
        org.assertj.core.api.Assertions.assertThat(PageableGuard.requireBounded(PageRequest.of(100, 100)))
                .isEqualTo(PageRequest.of(100, 100));
    }
}
