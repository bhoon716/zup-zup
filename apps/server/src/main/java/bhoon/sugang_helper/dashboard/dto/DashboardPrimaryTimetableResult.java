package bhoon.sugang_helper.dashboard.dto;

import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.CourseSnapshot;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.CustomScheduleSnapshot;
import bhoon.sugang_helper.planning.timetable.service.PrimaryTimetableQuery.PrimaryTimetableSnapshot;
import java.util.List;

public record DashboardPrimaryTimetableResult(
        Long id,
        String name,
        boolean isPrimary,
        List<CourseSnapshot> courses,
        List<CustomScheduleSnapshot> customSchedules,
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
