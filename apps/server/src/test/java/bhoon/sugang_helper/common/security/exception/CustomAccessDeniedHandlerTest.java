package bhoon.sugang_helper.common.security.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class CustomAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();

    @Test
    @DisplayName("접근 거부 시 표준 에러 응답을 반환한다")
    void handle_returnsStandardErrorResponse() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/schedules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(json.get("status").asInt()).isEqualTo(403);
        assertThat(json.get("code").asText()).isEqualTo("A002");
        assertThat(json.get("message").asText()).isEqualTo("접근 권한이 없습니다.");
        assertThat(json.get("path").asText()).isEqualTo("/api/v1/admin/schedules");
        assertThat(json.get("details").asText()).isEqualTo("denied");
    }
}
