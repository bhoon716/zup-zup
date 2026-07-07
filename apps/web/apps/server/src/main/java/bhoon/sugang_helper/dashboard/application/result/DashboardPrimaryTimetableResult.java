package bhoon.sugang_helper.dashboard.application.result;

import bhoon.sugang_helper.timetable.application.result.CustomScheduleResponse;
import bhoon.sugang_helper.timetable.application.result.TimetableCourseResponse;
import bhoon.sugang_helper.timetable.application.result.TimetableDetailResponse;
import java.util.List;

public record DashboardPrimaryTimetableResult(
        Long id,
        String name,
        boolean isPrimary,
        List<TimetableCourseResponse> courses,
        List<CustomScheduleResponse> customSchedules,
        String totalCredits) {

    public static DashboardPrimaryTimetableResult from(TimetableDetailResponse timetable) {
        return new DashboardPrimaryTimetableResult(
                timetable.getId(),
                timetable.getName(),
                timetable.isPrimary(),
                timetable.getCourses(),
                timetable.getCustomSchedules(),
                timetable.getTotalCredits());
    }
}
