package bhoon.sugang_helper.domain.schedule.response;

import bhoon.sugang_helper.domain.schedule.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @Schema(description = "일정 구분", example = "장바구니(예비)")
    private String scheduleType;

    @Schema(description = "시작일", example = "2026-02-02")
    private LocalDate startDate;

    @Schema(description = "종료일", example = "2026-02-04")
    private LocalDate endDate;

    @Schema(description = "시작 시간", type = "string", example = "10:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Schema(description = "종료 시간", type = "string", example = "18:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Schema(description = "D-Day", example = "D-3")
    @JsonProperty("dDay")
    private String dDay;

    /**
     * 엔티티를 전달받아 현재 날짜 기준의 D-Day를 계산한 후 응답 프록시로 변환합니다.
     */
    public static ScheduleResponse from(Schedule schedule) {
        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(today, schedule.getStartDate());

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
                .scheduleType(schedule.getScheduleType())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .dDay(dDayStr)
                .build();
    }
}
