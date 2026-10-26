package bhoon.sugang_helper.academic.review.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.academic.review.service.CourseReviewService;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.web.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourseReviewControllerValidationTest {

    private static final String COURSE_KEY = "COURSE-001";
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

    @Test
    void rejectsInvalidPageWithoutInvokingService() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH).param("page", "-1"))
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
        PageRequest pageRequest = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(reviewService.getReviews(COURSE_KEY, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        mockMvc.perform(get(REVIEWS_PATH))
                .andExpect(status().isOk());

        verify(reviewService).getReviews(COURSE_KEY, pageRequest);
    }
    @ParameterizedTest
    @ValueSource(strings = {"1", "100", "-1", "0", "invalid"})
    void ignoresClientSizeAndPreservesPageAndSort(String size) throws Exception {
        PageRequest pageRequest = PageRequest.of(2, 30, Sort.by(Sort.Direction.ASC, "createdAt"));
        when(reviewService.getReviews(COURSE_KEY, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 100));
        mockMvc.perform(get(REVIEWS_PATH).param("size", size).param("page", "2")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.totalElements").value(100));
        verify(reviewService).getReviews(COURSE_KEY, pageRequest);
    }
}
