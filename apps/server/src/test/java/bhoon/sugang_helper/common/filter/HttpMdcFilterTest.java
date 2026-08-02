package bhoon.sugang_helper.common.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

class HttpMdcFilterTest {

    private HttpMdcFilter httpMdcFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        httpMdcFilter = new HttpMdcFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        MDC.clear();
    }

    @Test
    @DisplayName("X-Correlation-Id 헤더가 없으면 새로운 UUID를 생성하여 MDC 및 응답 헤더에 설정한다")
    void generateNewCorrelationIdWhenHeaderMissing() throws Exception {
        // given
        when(request.getHeader(HttpMdcFilter.HEADER_CORRELATION_ID)).thenReturn(null);

        doAnswer(invocation -> {
            String mdcValue = MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID);
            assertThat(mdcValue).isNotNull().isNotBlank();
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        httpMdcFilter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(HttpMdcFilter.HEADER_CORRELATION_ID), captor.capture());
        assertThat(captor.getValue()).isNotNull().isNotBlank();
        assertThat(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("수신된 X-Correlation-Id 헤더를 재사용하여 MDC 및 응답 헤더에 설정한다")
    void reuseExistingCorrelationIdFromHeader() throws Exception {
        // given
        String existingId = "test-correlation-1234";
        when(request.getHeader(HttpMdcFilter.HEADER_CORRELATION_ID)).thenReturn(existingId);

        doAnswer(invocation -> {
            String mdcValue = MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID);
            assertThat(mdcValue).isEqualTo(existingId);
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        httpMdcFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setHeader(HttpMdcFilter.HEADER_CORRELATION_ID, existingId);
        assertThat(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("허용된 최대 길이의 Correlation ID는 MDC와 응답 헤더에 그대로 전파한다")
    void acceptCorrelationIdAtMaximumLength() throws Exception {
        // given
        String maxLengthId = "a".repeat(HttpMdcFilter.MAX_CORRELATION_ID_LENGTH);
        when(request.getHeader(HttpMdcFilter.HEADER_CORRELATION_ID)).thenReturn(maxLengthId);

        doAnswer(invocation -> {
            assertThat(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID)).isEqualTo(maxLengthId);
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        httpMdcFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setHeader(HttpMdcFilter.HEADER_CORRELATION_ID, maxLengthId);
        assertThat(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("최대 길이를 초과한 Correlation ID는 서버 생성 ID로 대체한다")
    void replaceOversizedCorrelationIdWithGeneratedId() throws Exception {
        // given
        String oversizedId = "a".repeat(HttpMdcFilter.MAX_CORRELATION_ID_LENGTH + 1);
        AtomicReference<String> mdcValue = new AtomicReference<>();
        when(request.getHeader(HttpMdcFilter.HEADER_CORRELATION_ID)).thenReturn(oversizedId);

        doAnswer(invocation -> {
            mdcValue.set(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID));
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        httpMdcFilter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(HttpMdcFilter.HEADER_CORRELATION_ID), captor.capture());
        String resolvedId = captor.getValue();
        assertThat(resolvedId)
                .isNotEqualTo(oversizedId)
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        assertThat(mdcValue).hasValue(resolvedId);
        assertThat(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"contains space", "contains/slash", "contains,comma"})
    @DisplayName("허용되지 않은 문자가 포함된 Correlation ID는 서버 생성 ID로 대체한다")
    void replaceMalformedCorrelationIdWithGeneratedId(String malformedId) throws Exception {
        // given
        when(request.getHeader(HttpMdcFilter.HEADER_CORRELATION_ID)).thenReturn(malformedId);

        // when
        httpMdcFilter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(HttpMdcFilter.HEADER_CORRELATION_ID), captor.capture());
        assertThat(captor.getValue()).isNotEqualTo(malformedId);
        assertThat(captor.getValue()).hasSize(36);
        assertThat(MDC.get(HttpMdcFilter.MDC_KEY_CORRELATION_ID)).isNull();
    }
}
