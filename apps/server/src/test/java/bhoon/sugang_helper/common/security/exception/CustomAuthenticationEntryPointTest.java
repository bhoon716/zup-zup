package bhoon.sugang_helper.common.security.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class CustomAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

    @Test
    @DisplayName("인증 실패 시 표준 에러 응답을 반환한다")
    void commence_returnsStandardErrorResponse() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new AuthenticationException("missing token") {});

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(json.get("code").asText()).isEqualTo("A001");
        assertThat(json.get("message").asText()).isEqualTo("유효하지 않은 토큰입니다.");
        assertThat(json.get("path").asText()).isEqualTo("/api/v1/me");
        assertThat(json.get("details").asText()).isEqualTo("missing token");
    }
}
