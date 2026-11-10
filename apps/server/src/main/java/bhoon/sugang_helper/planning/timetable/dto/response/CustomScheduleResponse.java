package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomScheduleResponse {
    private final Long id;
    private final String title;
    private final String professor;
    private final List<CustomScheduleTimeResponse> schedules;

    public static CustomScheduleResponse from(CustomSchedule schedule) {
        return new CustomScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getProfessor(),
                schedule.getTimes().stream()
                        .map(CustomScheduleTimeResponse::from)
                        .collect(Collectors.toList()));
    }
}
