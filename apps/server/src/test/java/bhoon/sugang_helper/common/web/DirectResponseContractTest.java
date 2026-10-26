package bhoon.sugang_helper.common.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.academic.review.controller.CourseReviewController;
import bhoon.sugang_helper.academic.review.service.CourseReviewService;
import bhoon.sugang_helper.identity.auth.controller.AuthController;
import bhoon.sugang_helper.identity.auth.service.AuthService;
import bhoon.sugang_helper.planning.timetable.controller.TimetableController;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableResponse;
import bhoon.sugang_helper.planning.timetable.service.TimetableService;
import bhoon.sugang_helper.support.feedback.controller.FeedbackController;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.CsrfTokenArgumentResolver;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DirectResponseContractTest {

    @Mock private TimetableService timetables;
    @Mock private AuthService auth;
    @Mock private FeedbackService feedback;
    @Mock private CourseReviewService reviews;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TimetableController(timetables), new AuthController(auth),
                        new FeedbackController(feedback), new CourseReviewController(reviews))
                .setCustomArgumentResolvers(new CsrfTokenArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsArrayWithoutSuccessEnvelope() throws Exception {
        given(timetables.getMyTimetables()).willReturn(List.of(TimetableResponse.builder().id(7L).name("기본").build()));
        mvc.perform(get("/api/v1/timetables"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(7)).andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void missingPrimaryTimetableKeeps200AndEmptyBody() throws Exception {
        mvc.perform(get("/api/v1/timetables/primary"))
                .andExpect(status().isOk()).andExpect(content().string(""));
    }

    @Test
    void csrfReturnsItsTokenObjectWithoutSuccessEnvelope() throws Exception {
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "masked-csrf");
        mvc.perform(get("/api/auth/csrf").requestAttr(CsrfToken.class.getName(), token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("masked-csrf"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void logoutKeeps200AndCookieDeletionWithNoBody() throws Exception {
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.addHeader(HttpHeaders.SET_COOKIE, "SESSION=; Max-Age=0; Path=/");
            return null;
        }).when(auth).logout(any(), any());
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk()).andExpect(content().string(""))
                .andExpect(cookie().maxAge("SESSION", 0)).andExpect(cookie().value("SESSION", ""));
    }

    @Test
    void feedbackCreationReturnsNumericIdWith201() throws Exception {
        given(feedback.createFeedbackForCurrentUser(any(), any(), any())).willReturn(42L);
        MockMultipartFile request = new MockMultipartFile("feedback", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"type\":\"BUG\",\"title\":\"문의\",\"content\":\"내용\"}".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/feedbacks").file(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$").value(42));
    }

    @Test
    void validationErrorKeepsCommonErrorAndCorrelationHeader() throws Exception {
        mvc.perform(post("/api/v1/courses/C1/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("G002"))
                .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(header().exists("X-Error-Id"));
    }
}
