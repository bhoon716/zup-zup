package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.academic.api.catalog.CourseScheduleSummary;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimetableCourseScheduleResponse {
    private final String dayOfWeek;
    private final String startTime;
    private final String endTime;

    public static TimetableCourseScheduleResponse from(CourseScheduleSummary schedule) {
        return new TimetableCourseScheduleResponse(
                schedule.dayOfWeek(),
                schedule.startTime(),
                schedule.endTime());
    }
}
