package bhoon.sugang_helper.domain.course.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 크롤링 타겟(년도/학기) 응답")
public class AdminCrawlTargetResponse {

    @Schema(description = "크롤링 대상 년도", example = "2025")
    private String year;

    @Schema(description = "크롤링 대상 학기 코드", example = "U211600020")
    private String semester;
}
