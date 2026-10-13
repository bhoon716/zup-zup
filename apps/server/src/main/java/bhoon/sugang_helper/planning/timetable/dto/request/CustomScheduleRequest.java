package bhoon.sugang_helper.planning.timetable.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomScheduleRequest {

    @NotBlank(message = "일정 제목을 입력해주세요.")
    private String title;

    private String professor;

    @NotEmpty(message = "최소 하나 이상의 시간대를 선택해주세요.")
    @Valid
    private List<CustomScheduleTimeRequest> schedules;

    public CustomScheduleRequest(String title, String professor, List<CustomScheduleTimeRequest> schedules) {
        this.title = title;
        this.professor = professor;
        this.schedules = schedules;
    }

    @Getter
    @NoArgsConstructor
    public static class CustomScheduleTimeRequest {
        @NotBlank(message = "요일을 입력해주세요.")
        private String dayOfWeek;

        @NotNull(message = "시작 시간을 입력해주세요.")
        private LocalTime startTime;

        @NotNull(message = "종료 시간을 입력해주세요.")
        private LocalTime endTime;

        private String classroom;

        public CustomScheduleTimeRequest(String dayOfWeek, LocalTime startTime, LocalTime endTime, String classroom) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.classroom = classroom;
        }
    }
}
