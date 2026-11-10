package bhoon.sugang_helper.academic.crawling.dto.response;

import bhoon.sugang_helper.academic.crawling.entity.CrawlerSetting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminCrawlTargetResponse {

    private final String year;

    private final String semester;

    public static AdminCrawlTargetResponse from(CrawlerSetting setting) {
        return new AdminCrawlTargetResponse(setting.getTargetYear(), setting.getTargetSemester());
    }
}
