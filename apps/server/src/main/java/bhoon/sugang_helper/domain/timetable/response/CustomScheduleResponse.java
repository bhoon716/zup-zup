package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.timetable.entity.CustomSchedule;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomScheduleResponse {
    private Long id;
    private String title;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String color;

    public static CustomScheduleResponse of(CustomSchedule schedule) {
        return CustomScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .color(schedule.getColor())
                .build();
    }
}
