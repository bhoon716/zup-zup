package bhoon.sugang_helper.academic.crawling.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCrawlTargetResponse {

    private String year;

    private String semester;
}
