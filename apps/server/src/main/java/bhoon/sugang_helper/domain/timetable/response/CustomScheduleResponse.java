package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.timetable.entity.CustomSchedule;
import bhoon.sugang_helper.domain.timetable.entity.CustomScheduleTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "커스텀 일정 응답 DTO")
public class CustomScheduleResponse {
    @Schema(description = "일정 ID", example = "10")
    private Long id;
    @Schema(description = "일정 제목", example = "동아리 모임")
    private String title;
    @Schema(description = "교수명", example = "홍길동")
    private String professor;
    @Schema(description = "시간대 목록")
    private List<CustomScheduleTimeResponse> schedules;

    @Getter
    @Builder
    public static class CustomScheduleTimeResponse {
        @Schema(description = "일정 ID", example = "10")
        private Long id;
        @Schema(description = "요일", example = "MONDAY")
        private String dayOfWeek;
        @Schema(description = "시작 시간", example = "18:00")
        private LocalTime startTime;
        @Schema(description = "종료 시간", example = "19:30")
        private LocalTime endTime;
        @Schema(description = "강의실", example = "공학 1호관 101호")
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
}
