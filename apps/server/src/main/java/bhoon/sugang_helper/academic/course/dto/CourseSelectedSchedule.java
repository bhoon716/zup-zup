package bhoon.sugang_helper.academic.course.dto;

import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import lombok.Builder;

@Builder
public record CourseSelectedSchedule(
        CourseDayOfWeek dayOfWeek,
        String startTime,
        String endTime
) {
}
