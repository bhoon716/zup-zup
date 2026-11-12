package bhoon.sugang_helper.support.feedback.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("내 문의의 두 번째 페이지는 최신순으로 10건씩 조회한다")
    void myFeedbacksUseFixedPageSizeAndStableDefaultOrder() throws Exception {
        // given
        PageRequest expectedPage = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(feedbackService.getMyFeedbacksForCurrentUser(expectedPage))
                .thenReturn(new PageImpl<>(List.of(), expectedPage, 23));

        // when & then
        mockMvc.perform(get("/api/v1/feedbacks/me").param("page", "1").param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(23))
                .andExpect(jsonPath("$.totalPages").value(3));
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
