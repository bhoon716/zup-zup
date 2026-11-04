package bhoon.sugang_helper.planning.timetable.dto.response;

import java.util.List;

public record TimetableCustomScheduleSnapshot(Long id, String title, String professor, List<TimetableCustomScheduleTimeSnapshot> schedules) {
}
