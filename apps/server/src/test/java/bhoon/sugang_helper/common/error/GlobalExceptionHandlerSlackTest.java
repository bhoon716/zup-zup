package bhoon.sugang_helper.common.error;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import bhoon.sugang_helper.common.alert.SlackAlertCategory;
import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
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
    void sendsOnlyServerErrorsToSlackAlertService() {
        SlackAlertService slackAlertService = Mockito.mock(SlackAlertService.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(slackAlertService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses");
        RuntimeException exception = new IllegalStateException("database unavailable");

        handler.handleCustomException(request, new CustomException(ErrorCode.INVALID_INPUT));
        handler.handleAny(request, exception);

        verify(slackAlertService).alert(SlackAlertCategory.SERVER_5XX, "G001", exception,
                "failureType=IllegalStateException");
        verifyNoMoreInteractions(slackAlertService);
    }

    @Test
    void forwardsOnlyTheRootCauseTypeAsAdditionalDiagnosticMetadata() {
        SlackAlertService alerts = Mockito.mock(SlackAlertService.class);
        RuntimeException failure = new IllegalStateException("token=private-token",
                new IllegalArgumentException("email=private@example.com"));

        new GlobalExceptionHandler(alerts).handleAny(new MockHttpServletRequest("GET", "/api/test"), failure);

        verify(alerts).alert(SlackAlertCategory.SERVER_5XX, "G001", failure,
                "failureType=IllegalArgumentException");
    }
}
