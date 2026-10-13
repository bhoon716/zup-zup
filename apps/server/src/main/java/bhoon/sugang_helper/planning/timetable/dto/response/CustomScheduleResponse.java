package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import bhoon.sugang_helper.planning.timetable.entity.CustomScheduleTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomScheduleResponse {
    private Long id;
    private String title;
    private String professor;
    private List<CustomScheduleTimeResponse> schedules;

    public static CustomScheduleResponse of(CustomSchedule schedule) {
        return CustomScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .professor(schedule.getProfessor())
                .schedules(schedule.getTimes().stream()
                        .map(CustomScheduleTimeResponse::of)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class CustomScheduleTimeResponse {
        private Long id;
        private String dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String classroom;

        public static CustomScheduleTimeResponse of(CustomScheduleTime time) {
            return CustomScheduleTimeResponse.builder()
                    .id(time.getId())
                    .dayOfWeek(time.getDayOfWeek())
                    .startTime(time.getStartTime())
                    .endTime(time.getEndTime())
                    .classroom(time.getClassroom())
                    .build();
        }
    }
}
