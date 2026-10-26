package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.entity.CustomScheduleTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomScheduleTimeResponse {
    private Long id;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroom;

    public static CustomScheduleTimeResponse of(CustomScheduleTime time) {
        return CustomScheduleTimeResponse.builder()
                .id(time.getId())
                .dayOfWeek(time.getDayOfWeek())
                .startTime(time.getStartTime())
                .endTime(time.getEndTime())
                .classroom(time.getClassroom())
                .build();
    }
}
