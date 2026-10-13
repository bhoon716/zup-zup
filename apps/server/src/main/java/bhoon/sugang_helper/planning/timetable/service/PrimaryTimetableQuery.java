package bhoon.sugang_helper.planning.timetable.service;

import java.util.List;
import java.util.Optional;

public interface PrimaryTimetableQuery {

    Optional<PrimaryTimetableSnapshot> findPrimaryForCurrentUser();

    record PrimaryTimetableSnapshot(
            Long id,
            String name,
            boolean isPrimary,
            List<CourseSnapshot> courses,
            List<CustomScheduleSnapshot> customSchedules,
            String totalCredits) {
    }

    record CourseSnapshot(
            String courseKey,
            String name,
            String professor,
            String classTime,
            String credits,
            String classification,
            String classroom,
            List<CourseScheduleSnapshot> schedules) {
    }

    record CourseScheduleSnapshot(String dayOfWeek, String startTime, String endTime) {
    }

    record CustomScheduleSnapshot(Long id, String title, String professor, List<CustomScheduleTimeSnapshot> schedules) {
    }

    record CustomScheduleTimeSnapshot(
            Long id,
            String dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            String classroom) {
    }
}
