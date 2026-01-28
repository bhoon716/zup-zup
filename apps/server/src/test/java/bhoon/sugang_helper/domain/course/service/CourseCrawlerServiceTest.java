package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.cdimascio.dotenv.Dotenv;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CourseSeatHistoryRepository courseSeatHistoryRepository;

    private JbnuCourseApiClient apiClient;
    private JbnuCourseParser courseParser;
    private CourseCrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        apiClient = new JbnuCourseApiClient();
        courseParser = new JbnuCourseParser();
        crawlerService = new CourseCrawlerService(courseRepository, apiClient, courseParser, eventPublisher,
                courseSeatHistoryRepository);
    }

    @Test
    @Tag("manual")
    @DisplayName("Crawl and Save - Real Network Integration")
    void crawlAndSave_RealIntegration() {
        // Given
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String realApiUrl = dotenv.get("JBNU_API_URL");

        if (realApiUrl == null || realApiUrl.isBlank()) {
            realApiUrl = System.getenv("JBNU_API_URL");
        }

        if (realApiUrl == null || realApiUrl.isBlank()) {
            throw new IllegalStateException(
                    "JBNU_API_URL environment variable is required for manual tests.\n" +
                            "Please check your .env file or system environment variables.");
        }

        ReflectionTestUtils.setField(apiClient, "apiUrl", realApiUrl);
        given(courseRepository.findByCourseKey(anyString())).willReturn(Optional.empty());

        // When
        crawlerService.crawlAndSaveCourses();

        // Then
        verify(courseRepository, atLeastOnce()).save(any(Course.class));
    }
}
