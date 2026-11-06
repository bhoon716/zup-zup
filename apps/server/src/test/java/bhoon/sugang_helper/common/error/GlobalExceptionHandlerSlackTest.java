package bhoon.sugang_helper.common.error;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerSlackTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("서버 오류만 Slack 알림 서비스에 전달한다")
    void sendsOnlyServerErrorsToSlackAlertService() {
        // given
        SlackAlertService slackAlertService = Mockito.mock(SlackAlertService.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(slackAlertService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses");
        RuntimeException exception = new IllegalStateException("database unavailable");

        handler.handleCustomException(request, new CustomException(ErrorCode.INVALID_INPUT));
        handler.handleAny(request, exception);

        // when & then
        verify(slackAlertService).alert(SlackAlertCategory.SERVER_5XX, "G001", exception,
                "failureType=IllegalStateException");
        verifyNoMoreInteractions(slackAlertService);
    }

    @Test
    @DisplayName("추가 진단에는 최종 원인의 타입만 전달한다")
    void forwardsOnlyTheRootCauseTypeAsAdditionalDiagnosticMetadata() {
        // given
        SlackAlertService alerts = Mockito.mock(SlackAlertService.class);
        RuntimeException failure = new IllegalStateException("token=private-token",
                new IllegalArgumentException("email=private@example.com"));

        new GlobalExceptionHandler(alerts).handleAny(new MockHttpServletRequest("GET", "/api/test"), failure);

        // when & then
        verify(alerts).alert(SlackAlertCategory.SERVER_5XX, "G001", failure,
                "failureType=IllegalArgumentException");
    }
}
