package bhoon.sugang_helper.domain.course.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "검색 기본 학기 요청")
public class SearchDefaultSemesterRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9]{2,20}", message = "학기 코드는 영문/숫자 2~20자리여야 합니다.")
    @Schema(description = "검색 기본 학기 코드", example = "U211600010")
    private String semester;

    public SearchDefaultSemesterRequest(String semester) {
        this.semester = semester;
    }
}
