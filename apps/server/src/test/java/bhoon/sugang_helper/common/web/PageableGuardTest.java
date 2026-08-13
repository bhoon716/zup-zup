package bhoon.sugang_helper.common.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    void rejectsUnpagedRequestWithInvalidInput() {
        assertThatThrownBy(() -> PageableGuard.requireBounded(Pageable.unpaged()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void acceptsBoundaryPage() {
        org.assertj.core.api.Assertions.assertThat(PageableGuard.requireBounded(PageRequest.of(100, 100)))
                .isEqualTo(PageRequest.of(100, 100));
    }
}
