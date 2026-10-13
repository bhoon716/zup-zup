package bhoon.sugang_helper.campus.schedule.dto.response;

import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleResponse {

    private Long id;

    private String scheduleType;

    private LocalDate startDate;

    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @JsonProperty("dDay")
    private String dDay;

    /**
     * 엔티티를 전달받아 현재 날짜 기준의 D-Day를 계산한 후 응답 프록시로 변환합니다.
     */
    public static ScheduleResponse from(Schedule schedule) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();

        String dDayStr;
        if (startDate != null && endDate != null && !today.isBefore(startDate) && !today.isAfter(endDate)) {
            dDayStr = "D-Day";
        } else if (startDate != null && today.isBefore(startDate)) {
            long daysBetween = ChronoUnit.DAYS.between(today, startDate);
            dDayStr = "D-" + daysBetween;
        } else if (endDate != null && today.isAfter(endDate)) {
            long daysAfter = ChronoUnit.DAYS.between(endDate, today);
            dDayStr = "D+" + daysAfter;
        } else if (startDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(today, startDate);
            if (daysBetween == 0) {
                dDayStr = "D-Day";
            } else if (daysBetween > 0) {
                dDayStr = "D-" + daysBetween;
            } else {
                dDayStr = "D+" + Math.abs(daysBetween);
            }
        } else {
            dDayStr = "D-Day";
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
