package bhoon.sugang_helper.common.error;

import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.alert.SlackAlertCategory;
import bhoon.sugang_helper.common.alert.SlackAlertService;
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

        handler.handleAny(request, exception);

        verify(slackAlertService).alert(SlackAlertCategory.SERVER_5XX, "G001", exception);
    }
}
