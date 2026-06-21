package bhoon.sugang_helper.crawling.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "검색 기본 학기 응답")
public class SearchDefaultSemesterResponse {

    @Schema(description = "검색 기본 학기 코드", example = "U211600010")
    private String semester;
}
