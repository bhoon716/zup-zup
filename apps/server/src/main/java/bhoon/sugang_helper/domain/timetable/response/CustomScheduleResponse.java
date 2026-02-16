package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.timetable.entity.CustomSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
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
    @Schema(description = "요일", example = "MONDAY")
    private String dayOfWeek;
    @Schema(description = "시작 시간", example = "18:00")
    private LocalTime startTime;
    @Schema(description = "종료 시간", example = "19:30")
    private LocalTime endTime;
    @Schema(description = "표시 색상", example = "#3b82f6")
    private String color;

    public static CustomScheduleResponse of(CustomSchedule schedule) {
        return CustomScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .color(schedule.getColor())
                .build();
    }
}
