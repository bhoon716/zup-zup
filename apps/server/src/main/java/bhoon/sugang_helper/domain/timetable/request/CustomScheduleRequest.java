package bhoon.sugang_helper.domain.timetable.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "시간표 커스텀 일정 추가 요청 DTO")
public class CustomScheduleRequest {

    @Schema(description = "일정 제목", example = "동아리 모임")
    @NotBlank(message = "일정 제목을 입력해주세요.")
    private String title;

    @Schema(description = "요일", example = "MONDAY")
    @NotBlank(message = "요일을 입력해주세요.")
    private String dayOfWeek;

    @Schema(description = "시작 시간", example = "18:00:00")
    @NotNull(message = "시작 시간을 입력해주세요.")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "19:30:00")
    @NotNull(message = "종료 시간을 입력해주세요.")
    private LocalTime endTime;

    @Schema(description = "표시 색상", example = "#3b82f6")
    private String color;

    public CustomScheduleRequest(String title, String dayOfWeek, LocalTime startTime, LocalTime endTime, String color) {
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }
}
