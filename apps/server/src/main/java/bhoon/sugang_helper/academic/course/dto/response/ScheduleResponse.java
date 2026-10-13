package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.CourseSchedule;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleResponse {

    private final String dayOfWeek;

    private final String startTime;

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
