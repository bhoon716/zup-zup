package bhoon.sugang_helper.domain.schedule.response;

import bhoon.sugang_helper.domain.schedule.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "일정 응답 DTO")
public class ScheduleResponse {

    @Schema(description = "일정 ID", example = "1")
    private Long id;

    @Schema(description = "일정 이름", example = "2026년 1학기 수강신청")
    private String title;

    @Schema(description = "일정 날짜", example = "2026-02-14")
    private LocalDate scheduleDate;

    @Schema(description = "일정 시간", example = "10:00:00")
    private LocalTime scheduleTime;

    @Schema(description = "D-Day", example = "D-3")
    @JsonProperty("dDay")
    private String dDay;

    /**
     * 엔티티를 전달받아 현재 날짜 기준의 D-Day를 계산한 후 응답 프록시로 변환합니다.
     */
    public static ScheduleResponse from(Schedule schedule) {
        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(today, schedule.getScheduleDate());

        String dDayStr;
        if (daysBetween == 0) {
            dDayStr = "D-Day";
        } else if (daysBetween > 0) {
            dDayStr = "D-" + daysBetween;
        } else {
            dDayStr = "D+" + Math.abs(daysBetween);
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .scheduleDate(schedule.getScheduleDate())
                .scheduleTime(schedule.getScheduleTime())
                .dDay(dDayStr)
                .build();
    }
}
