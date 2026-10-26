package bhoon.sugang_helper.academic.crawling.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.course.dto.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.service.CourseCatalogSynchronizationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseCrawlCompletionServiceTest {

    @Mock
    private CourseCatalogSynchronizationService courseCatalogSynchronizationService;
    @Mock
    private CrawlerRunStateService crawlerRunStateService;
    @InjectMocks
    private CourseCrawlCompletionService completionService;

    @Test
    void recordsSuccessOnlyAfterCatalogSynchronizationCompletes() {
        List<ParsedCourseDto> parsedCourses = List.of();

        completionService.complete(parsedCourses);

        var ordered = inOrder(courseCatalogSynchronizationService, crawlerRunStateService);
        ordered.verify(courseCatalogSynchronizationService).synchronize(parsedCourses);
        ordered.verify(crawlerRunStateService).markSuccess();
    }

    @Test
    void doesNotRecordSuccessWhenCatalogSynchronizationFails() {
        List<ParsedCourseDto> parsedCourses = List.of();
        IllegalStateException failure = new IllegalStateException("catalog write failed");
        doThrow(failure).when(courseCatalogSynchronizationService).synchronize(parsedCourses);

        assertThatThrownBy(() -> completionService.complete(parsedCourses))
                .isSameAs(failure);
        verify(crawlerRunStateService, never()).markSuccess();
    }
}
