package bhoon.sugang_helper.planning.api;

import java.util.List;

public record PrimaryTimetableSnapshot(
        Long id,
        String name,
        boolean isPrimary,
        List<TimetableCourseSnapshot> courses,
        List<TimetableCustomScheduleSnapshot> customSchedules,
        String totalCredits) {
}
