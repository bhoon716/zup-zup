package bhoon.sugang_helper.course.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.course.application.CourseSearchCondition;
import bhoon.sugang_helper.course.application.CourseService;
import bhoon.sugang_helper.crawling.application.CourseCrawlerTargetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
