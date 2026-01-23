package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private JbnuCourseApiClient apiClient;
    private CourseCrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        apiClient = new JbnuCourseApiClient(); // Real Client
        crawlerService = new CourseCrawlerService(courseRepository, apiClient);
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
