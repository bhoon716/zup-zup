package bhoon.sugang_helper.planning.timetable.dto;

import java.time.LocalTime;

public record TimetableCustomScheduleTimeSnapshot(
        Long id,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String classroom) {
}
