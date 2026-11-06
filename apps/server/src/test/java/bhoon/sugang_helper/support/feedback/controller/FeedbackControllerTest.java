package bhoon.sugang_helper.support.feedback.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FeedbackController(feedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("잘못된 피드백 JSON은 잘못된 요청으로 응답한다")
    void malformedFeedbackJsonReturnsBadRequest() throws Exception {
        // given
        MockMultipartFile feedback = new MockMultipartFile(
                "feedback", "feedback.json", MediaType.APPLICATION_JSON_VALUE,
                "{\"type\":\"BUG\"".getBytes(StandardCharsets.UTF_8));

        // when & then
        mockMvc.perform(multipart("/api/v1/feedbacks").file(feedback))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"));

        verifyNoInteractions(feedbackService);
    }
}
