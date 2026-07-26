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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
