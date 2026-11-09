package bhoon.sugang_helper.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.api.alerts.SlackAlertCategory;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

class GlobalExceptionHandlerInputTest {

    private SlackAlertService alerts;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        alerts = Mockito.mock(SlackAlertService.class);
        mvc = MockMvcBuilders.standaloneSetup(new InputController())
                .setControllerAdvice(new GlobalExceptionHandler(alerts)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/input/path/not-a-number", "/input/query?count=not-a-number",
            "/input/query", "/input/constrained?count=0"})
    @DisplayName("경로와 쿼리 입력 오류는 추적 가능한 400이며 서버 장애 알림을 보내지 않는다")
    void invalidRequestParametersReturnBadRequestWithoutAlert(String path) throws Exception {
        // when & then
        mvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getHeader("X-Error-Id")).isNotBlank());
        verifyNoInteractions(alerts);
    }

    @Test
    @DisplayName("필수 멀티파트 누락은 서버 장애 알림 없이 400을 반환한다")
    void missingPartReturnsBadRequestWithoutAlert() throws Exception {
        // when & then
        mvc.perform(multipart("/input/part"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));
        verifyNoInteractions(alerts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/input/bug", "/input/invalid-return"})
    @DisplayName("구현 오류와 반환값 검증 실패는 500과 서버 장애 알림을 유지한다")
    void actualServerFailuresStillAlert(String path) throws Exception {
        // when & then
        mvc.perform(get(path))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()));
        verify(alerts).alert(eq(SlackAlertCategory.SERVER_5XX), eq(ErrorCode.INTERNAL_ERROR.getCode()),
                any(Exception.class), any(String.class));
    }

    @RestController
    static class InputController {

        @GetMapping("/input/path/{id}")
        public long path(@PathVariable("id") long id) {
            return id;
        }

        @GetMapping("/input/query")
        public int query(@RequestParam("count") int count) {
            return count;
        }

        @GetMapping("/input/constrained")
        public int constrained(@RequestParam("count") @Min(1) int count) {
            return count;
        }

        @PostMapping("/input/part")
        public String part(@RequestPart("file") MultipartFile file) {
            return "ok";
        }

        @GetMapping("/input/bug")
        public String bug() {
            throw new IllegalArgumentException("implementation bug");
        }

        @NotNull
        @GetMapping("/input/invalid-return")
        public String invalidReturn() {
            return null;
        }
    }
}
