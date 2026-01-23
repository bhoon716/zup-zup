package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JbnuCourseApiClient apiClient;
    private CourseCrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        apiClient = new JbnuCourseApiClient();
        crawlerService = new CourseCrawlerService(courseRepository, apiClient, eventPublisher);
    }

    @Test
    @DisplayName("Crawl and Save - Real Network Integration")
    void crawlAndSave_RealIntegration() {
        // Given
        given(courseRepository.findById(anyString())).willReturn(Optional.empty());

        // When
        crawlerService.crawlAndSaveCourses();

        // Then
        verify(courseRepository, atLeastOnce()).save(any(Course.class));
    }
}
