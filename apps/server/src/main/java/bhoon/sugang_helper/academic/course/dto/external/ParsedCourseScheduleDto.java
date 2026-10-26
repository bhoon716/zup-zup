package bhoon.sugang_helper.academic.course.dto.external;

import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import java.time.LocalTime;




public record ParsedCourseScheduleDto(
        CourseDayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
