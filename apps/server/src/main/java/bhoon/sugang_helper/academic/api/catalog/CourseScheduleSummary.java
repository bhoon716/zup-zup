package bhoon.sugang_helper.academic.api.catalog;

import java.time.LocalTime;

public record CourseScheduleSummary(String dayOfWeek, String startTime, String endTime) {

    public static CourseScheduleSummary of(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return new CourseScheduleSummary(dayOfWeek, startTime.toString(), endTime.toString());
    }
}
