package bhoon.sugang_helper.planning.timetable.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
}
