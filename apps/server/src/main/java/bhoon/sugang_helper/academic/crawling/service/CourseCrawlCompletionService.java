package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.service.CourseCatalogSynchronizationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수집된 강의 반영과 크롤링 성공 기록을 하나의 트랜잭션으로 완료합니다.
 */
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
