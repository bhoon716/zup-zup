package bhoon.sugang_helper.domain.course.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 크롤링 타겟(년도/학기) 요청")
public class AdminCrawlTargetRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "년도는 4자리 숫자여야 합니다.")
    @Schema(description = "크롤링 대상 년도", example = "2025")
    private String year;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9]{2,20}", message = "학기 코드는 영문/숫자 2~20자리여야 합니다.")
    @Schema(description = "크롤링 대상 학기 코드", example = "U211600020")
    private String semester;

    public AdminCrawlTargetRequest(String year, String semester) {
        this.year = year;
        this.semester = semester;
    }
}
