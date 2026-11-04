package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.service.CourseCatalogSynchronizationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
@RequiredArgsConstructor
public class CourseCrawlCompletionService {

    private final CourseCatalogSynchronizationService courseCatalogSynchronizationService;
    private final CrawlerRunStateService crawlerRunStateService;

    @Transactional
    public void complete(List<ParsedCourseDto> parsedCourses) {
        courseCatalogSynchronizationService.synchronize(parsedCourses);
        crawlerRunStateService.markSuccess();
    }
}
