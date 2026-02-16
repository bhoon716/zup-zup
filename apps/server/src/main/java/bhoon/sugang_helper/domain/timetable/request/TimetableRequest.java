package bhoon.sugang_helper.domain.timetable.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "시간표 생성 요청 DTO")
public class TimetableRequest {

    @Schema(description = "시간표 이름", example = "2026-1학기")
    @NotBlank(message = "시간표 이름을 입력해주세요.")
    @Size(max = 50, message = "시간표 이름은 50자 이내여야 합니다.")
    private String name;

    @Schema(description = "대표 시간표 여부", example = "true")
    private boolean isPrimary;

    public TimetableRequest(String name, boolean isPrimary) {
        this.name = name;
        this.isPrimary = isPrimary;
    }
}
