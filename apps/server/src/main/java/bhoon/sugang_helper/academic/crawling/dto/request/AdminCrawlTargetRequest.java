package bhoon.sugang_helper.academic.crawling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminCrawlTargetRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "년도는 4자리 숫자여야 합니다.")
    private String year;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9]{2,20}", message = "학기 코드는 영문/숫자 2~20자리여야 합니다.")
    private String semester;

    public AdminCrawlTargetRequest(String year, String semester) {
        this.year = year;
        this.semester = semester;
    }
}
