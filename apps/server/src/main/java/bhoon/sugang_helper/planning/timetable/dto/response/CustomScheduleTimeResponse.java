package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.entity.CustomScheduleTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomScheduleTimeResponse {
    private final Long id;
    private final String dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String classroom;

    public static CustomScheduleTimeResponse from(CustomScheduleTime time) {
        return new CustomScheduleTimeResponse(
                time.getId(),
                time.getDayOfWeek(),
                time.getStartTime(),
                time.getEndTime(),
                time.getClassroom());
    }
}
