package bhoon.sugang_helper.dashboard.dto;

import bhoon.sugang_helper.planning.timetable.dto.PrimaryTimetableSnapshot;
import bhoon.sugang_helper.planning.timetable.dto.TimetableCourseSnapshot;
import bhoon.sugang_helper.planning.timetable.dto.TimetableCustomScheduleSnapshot;
import java.util.List;

public record DashboardPrimaryTimetableResult(
        Long id,
        String name,
        boolean isPrimary,
        List<TimetableCourseSnapshot> courses,
        List<TimetableCustomScheduleSnapshot> customSchedules,
        String totalCredits) {

    public static DashboardPrimaryTimetableResult from(PrimaryTimetableSnapshot timetable) {
        return new DashboardPrimaryTimetableResult(
                timetable.id(),
                timetable.name(),
                timetable.isPrimary(),
                timetable.courses(),
                timetable.customSchedules(),
                timetable.totalCredits());
    }
}
