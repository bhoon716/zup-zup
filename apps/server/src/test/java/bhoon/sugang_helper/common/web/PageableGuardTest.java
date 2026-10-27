package bhoon.sugang_helper.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

class PageableGuardTest {

    @Test
    void rejectedPageIsWarnedOnlyByTheFinalApiHandler() {
        Logger guard = (Logger) LoggerFactory.getLogger(PageableGuard.class);
        Logger handler = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        Level previous = guard.getLevel();
        guard.setLevel(Level.DEBUG);
        events.start();
        guard.addAppender(events);
        handler.addAppender(events);
        try {
            try {
                PageableGuard.requireBounded(PageRequest.of(0, 101));
            } catch (CustomException failure) {
                new GlobalExceptionHandler().handleCustomException(
                        new MockHttpServletRequest("GET", "/api/test"), failure);
            }
            assertThat(events.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.WARN);
            assertThat(events.list.getLast().getFormattedMessage()).contains("[API_ERROR]", "code=G002");
        } finally {
            guard.detachAppender(events);
            handler.detachAppender(events);
            guard.setLevel(previous);
            events.stop();
        }
    }

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
        assertThat(PageableGuard.requireBounded(PageRequest.of(100, 100)))
                .isEqualTo(PageRequest.of(100, 100));
    }
    @ParameterizedTest
    @ValueSource(ints = {1, 30, 100, 2000})
    void fixesApiSizeAndPreservesPageAndSort(int requestedSize) {
        Pageable page = PageableGuard.fixedSize(PageRequest.of(2, requestedSize, Sort.by("createdAt").ascending()));
        assertThat(page.getPageSize()).isEqualTo(30);
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getOffset()).isEqualTo(60);
        assertThat(page.getSort()).isEqualTo(Sort.by("createdAt").ascending());
    }

    @Test
    void fixedSizeChecksActualOffsetAndRejectsUnpaged() {
        assertThatThrownBy(() -> PageableGuard.fixedSize(PageRequest.of(334, 1)))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> PageableGuard.fixedSize(Pageable.unpaged()))
                .isInstanceOf(CustomException.class);
    }
}
