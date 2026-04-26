package bhoon.sugang_helper.domain.course.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 단과대학 및 하위 학과 계층 구조 응답 DTO
 */
@Getter
@Builder
@Schema(description = "단과대학 및 하위 학과 계층 구조 응답")
public class CollegeHierarchyResponse {
    @Schema(description = "단과대 ID", example = "1")
    private Long id;

    @Schema(description = "단과대 명칭", example = "공과대학")
    private String name;

    @Schema(description = "하위 학과 목록")
    private List<DepartmentResponse> departments;

    @Getter
    @Builder
    @Schema(description = "학과 정보 응답")
    public static class DepartmentResponse {
        @Schema(description = "학과 ID", example = "101")
        private Long id;

        @Schema(description = "학과 명칭", example = "소프트웨어공학과")
        private String name;
    }
}
