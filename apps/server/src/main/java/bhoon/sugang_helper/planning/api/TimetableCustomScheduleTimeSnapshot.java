package bhoon.sugang_helper.planning.api;

import java.time.LocalTime;

public record TimetableCustomScheduleTimeSnapshot(
        Long id,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String classroom) {
}
