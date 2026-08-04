package bhoon.sugang_helper.course.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.course.application.CourseSearchCondition;
import bhoon.sugang_helper.course.application.CourseService;
import bhoon.sugang_helper.crawling.application.CourseCrawlerTargetService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CourseControllerValidationTest {

    @Mock
    private CourseService courseService;

    @Mock
    private CourseCrawlerTargetService crawlerTargetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(courseService, crawlerTargetService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void rejectsOversizedTextAndDoesNotInvokeSearch() throws Exception {
        mockMvc.perform(post("/api/v1/courses/search")
                        .contentType("application/json")
                        .content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(courseService, never()).searchCourses(any(CourseSearchCondition.class), any());
    }

    @Test
    void rejectsUnknownTargetGrade() throws Exception {
        mockMvc.perform(post("/api/v1/courses/search")
                        .contentType("application/json")
                        .content("{\"targetGrades\":[\"NOT_A_GRADE\"]}"))
                .andExpect(status().isBadRequest());

        verify(courseService, never()).searchCourses(any(CourseSearchCondition.class), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"popular", "wishlist", "wishlistCount", "wishlist_count", "wished"})
    void acceptsWishlistSortAliasesAsCanonicalPopularSort(String sortAlias) throws Exception {
        given(courseService.searchCourses(any(CourseSearchCondition.class), any()))
                .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 30), false));

        mockMvc.perform(post("/api/v1/courses/search")
                        .contentType("application/json")
                        .content("{\"sortBy\":\"" + sortAlias + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CourseSearchCondition> conditionCaptor = ArgumentCaptor.forClass(CourseSearchCondition.class);
        verify(courseService).searchCourses(conditionCaptor.capture(), any());
        assertThat(conditionCaptor.getValue().getSortBy()).isEqualTo("popular");
    }
}
