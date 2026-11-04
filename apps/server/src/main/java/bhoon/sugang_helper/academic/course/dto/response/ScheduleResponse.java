package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.CourseSchedule;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleResponse {

    private final String dayOfWeek;

    private final String startTime;

    private final String endTime;







    public static ScheduleResponse from(CourseSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return ScheduleResponse.builder()
                .dayOfWeek(schedule.getDayOfWeek().name())
                .startTime(schedule.getStartTime().format(formatter))
                .endTime(schedule.getEndTime().format(formatter))
                .build();
    }
}
