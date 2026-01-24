package bhoon.sugang_helper.domain.course.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "과목 검색 조건 DTO")
public class CourseSearchCondition {
    @Schema(description = "과목명", example = "우리생활과화학")
    private String name;

    @Schema(description = "교수명", example = "김혜진")
    private String professor;

    @Schema(description = "과목 코드", example = "0000130844")
    private String subjectCode;
}
