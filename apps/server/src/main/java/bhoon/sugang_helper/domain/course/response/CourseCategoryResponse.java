package bhoon.sugang_helper.domain.course.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "교양 카테고리 응답 DTO")
public class CourseCategoryResponse {

    @Schema(description = "교양 카테고리", example = "균형교양")
    private String category;

    @Schema(description = "해당 카테고리의 상세 목록")
    private List<String> details;
}
