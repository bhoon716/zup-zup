package bhoon.sugang_helper.academic.course.dto.external;

import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import java.time.LocalTime;

/**
 * 강의 시간표 정보를 담는 DTO입니다.
 */
public record ParsedCourseScheduleDto(
        CourseDayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
