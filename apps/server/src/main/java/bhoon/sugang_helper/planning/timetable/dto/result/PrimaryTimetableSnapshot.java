package bhoon.sugang_helper.planning.timetable.dto.result;

import bhoon.sugang_helper.planning.timetable.dto.response.TimetableCourseSnapshot;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableCustomScheduleSnapshot;

import java.util.List;

public record PrimaryTimetableSnapshot(
        Long id,
        String name,
        boolean isPrimary,
        List<TimetableCourseSnapshot> courses,
        List<TimetableCustomScheduleSnapshot> customSchedules,
        String totalCredits) {
}
