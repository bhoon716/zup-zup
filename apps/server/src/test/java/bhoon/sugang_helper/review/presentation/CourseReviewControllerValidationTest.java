package bhoon.sugang_helper.review.presentation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.error.GlobalExceptionHandler;
import bhoon.sugang_helper.review.application.CourseReviewService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourseReviewControllerValidationTest {

    private static final String REVIEWS_PATH = "/api/v1/courses/COURSE-001/reviews";

    @Mock
    private CourseReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @CsvSource({"page, -1", "size, 0"})
    void rejectsInvalidPaginationWithoutInvokingService(String parameter, String value) throws Exception {
        mockMvc.perform(get(REVIEWS_PATH).param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));

        verifyNoInteractions(reviewService);
    }

    @Test
    void rejectsUnsupportedSortDirectionWithoutInvokingService() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH).param("sort", "createdAt,unsupported"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));

        verifyNoInteractions(reviewService);
    }

    @Test
    void rejectsBlankSortPropertyWithoutInvokingService() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH).param("sort", ",desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));

        verifyNoInteractions(reviewService);
    }

    @Test
    void acceptsDefaultPaginationAndSort() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(reviewService.getReviews("COURSE-001", pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        mockMvc.perform(get(REVIEWS_PATH))
                .andExpect(status().isOk());

        verify(reviewService).getReviews("COURSE-001", pageRequest);
    }
}
