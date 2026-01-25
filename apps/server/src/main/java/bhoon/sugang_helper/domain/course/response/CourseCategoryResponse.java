package bhoon.sugang_helper.domain.course.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "교양 영역 및 상세구분 목록 응답 DTO")
public class CourseCategoryResponse {

    @Schema(description = "교양영역구분", example = "기초")
    private String category;

    @Schema(description = "교양영역상세구분 목록", example = "[\"공통기초\", \"의사소통\"]")
    private List<String> details;
}
