package bhoon.sugang_helper.domain.timetable.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "시간표 강의 추가 요청 DTO")
public class TimetableCourseRequest {

    @Schema(description = "강의 키", example = "2026:10:12345:01")
    @NotBlank(message = "강좌 키를 입력해주세요.")
    private String courseKey;
}
