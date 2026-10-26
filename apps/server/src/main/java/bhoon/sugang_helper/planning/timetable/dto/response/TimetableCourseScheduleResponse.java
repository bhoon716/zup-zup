package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.academic.course.dto.CourseScheduleSummary;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimetableCourseScheduleResponse {
    private String dayOfWeek;
    private String startTime;
    private String endTime;

    public static TimetableCourseScheduleResponse of(CourseScheduleSummary schedule) {
        return TimetableCourseScheduleResponse.builder()
                .dayOfWeek(schedule.dayOfWeek())
                .startTime(schedule.startTime())
                .endTime(schedule.endTime())
                .build();
    }
}
