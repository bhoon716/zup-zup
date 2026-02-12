package bhoon.sugang_helper.domain.timetable.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomScheduleRequest {

    @NotBlank(message = "일정 제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "요일을 입력해주세요.")
    private String dayOfWeek;

    @NotNull(message = "시작 시간을 입력해주세요.")
    private LocalTime startTime;

    @NotNull(message = "종료 시간을 입력해주세요.")
    private LocalTime endTime;

    private String color;

    public CustomScheduleRequest(String title, String dayOfWeek, LocalTime startTime, LocalTime endTime, String color) {
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }
}
