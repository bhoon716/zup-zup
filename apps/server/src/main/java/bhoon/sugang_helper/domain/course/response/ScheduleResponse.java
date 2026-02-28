package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "강의 시간 정보 응답 DTO")
public class ScheduleResponse {

    @Schema(description = "요일", example = "MONDAY")
    private final String dayOfWeek;

    @Schema(description = "시작 시간", example = "09:00")
    private final String startTime;

    @Schema(description = "종료 시간", example = "10:00")
    private final String endTime;

    /**
     * 강의 일정 정보를 ScheduleResponse DTO로 변환
     * 
     * @param schedule 강의 일정 엔티티
     * @return HH:mm 형식의 시간이 포함된 DTO
     */
    public static ScheduleResponse from(CourseSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return ScheduleResponse.builder()
                .dayOfWeek(schedule.getDayOfWeek().name())
                .startTime(schedule.getStartTime().format(formatter))
                .endTime(schedule.getEndTime().format(formatter))
                .build();
    }
}
