package bhoon.sugang_helper.planning.api;

import java.util.List;

public record TimetableCourseSnapshot(
        String courseKey,
        String name,
        String professor,
        String classTime,
        String credits,
        String classification,
        String classroom,
        List<TimetableCourseScheduleSnapshot> schedules) {
}
