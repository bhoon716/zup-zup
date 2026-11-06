package bhoon.sugang_helper.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;

class PageableGuardTest {

    @Test
    @DisplayName("거부된 페이지 요청의 경고는 최종 API 처리기에서만 남긴다")
    void rejectedPageIsWarnedOnlyByTheFinalApiHandler() {
        // given
        Logger guard = (Logger) LoggerFactory.getLogger(PageableGuard.class);
        Logger handler = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        Level previous = guard.getLevel();
        guard.setLevel(Level.DEBUG);
        events.start();
        guard.addAppender(events);
        handler.addAppender(events);

        // when & then
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
    @DisplayName("허용 범위를 넘는 페이지 offset을 거부한다")
    void rejectsExcessiveOffset() {
        // when & then
        assertThatThrownBy(() -> PageableGuard.requireBounded(PageRequest.of(101, 100)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("페이지 없는 요청은 잘못된 입력으로 거부한다")
    void rejectsUnpagedRequestWithInvalidInput() {
        // when & then
        assertThatThrownBy(() -> PageableGuard.requireBounded(Pageable.unpaged()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("허용 경계의 페이지 요청을 수락한다")
    void acceptsBoundaryPage() {
        // when & then
        assertThat(PageableGuard.requireBounded(PageRequest.of(100, 100)))
                .isEqualTo(PageRequest.of(100, 100));
    }

    @ParameterizedTest
    @DisplayName("API 크기는 고정하고 페이지 번호와 정렬은 보존한다")
    @ValueSource(ints = {1, 30, 100, 2000})
    void fixesApiSizeAndPreservesPageAndSort(int requestedSize) {
        // when
        Pageable page = PageableGuard.fixedSize(PageRequest.of(2, requestedSize, Sort.by("createdAt").ascending()));

        // then
        assertThat(page.getPageSize()).isEqualTo(30);
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getOffset()).isEqualTo(60);
        assertThat(page.getSort()).isEqualTo(Sort.by("createdAt").ascending());
    }

    @Test
    @DisplayName("고정 크기에서도 실제 offset을 검사하고 페이지 없는 요청을 거부한다")
    void fixedSizeChecksActualOffsetAndRejectsUnpaged() {
        // when & then
        assertThatThrownBy(() -> PageableGuard.fixedSize(PageRequest.of(334, 1)))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> PageableGuard.fixedSize(Pageable.unpaged()))
                .isInstanceOf(CustomException.class);
    }
}
