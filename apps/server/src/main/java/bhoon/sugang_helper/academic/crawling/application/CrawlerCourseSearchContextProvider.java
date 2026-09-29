package bhoon.sugang_helper.academic.crawling.application;

import bhoon.sugang_helper.academic.crawling.contract.CrawlTargetInfo;

import bhoon.sugang_helper.academic.course.contract.CourseSearchContext;
import bhoon.sugang_helper.academic.course.contract.CourseSearchContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlerCourseSearchContextProvider implements CourseSearchContextProvider {

    private final CourseCrawlerTargetService crawlerTargetService;
    private final CrawlerRunStateService crawlerRunStateService;

    @Override
    @Transactional(readOnly = true)
    public CourseSearchContext getCurrentContext() {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        return new CourseSearchContext(
                target.year(),
                target.semester(),
                crawlerRunStateService.findLastSuccessAt().orElse(null));
    }
}
