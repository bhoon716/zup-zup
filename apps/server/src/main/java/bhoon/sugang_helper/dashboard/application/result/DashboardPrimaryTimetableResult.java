package bhoon.sugang_helper.dashboard.application.result;

import bhoon.sugang_helper.planning.timetable.contract.PrimaryTimetableQuery.PrimaryTimetableSnapshot;
import bhoon.sugang_helper.planning.timetable.contract.PrimaryTimetableQuery.CourseSnapshot;
import bhoon.sugang_helper.planning.timetable.contract.PrimaryTimetableQuery.CustomScheduleSnapshot;
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
