package bhoon.sugang_helper.campus.schedule.dto.response;

import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleResponse {

    private final Long id;

    private final String scheduleType;

    private final LocalDate startDate;

    private final LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private final LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private final LocalTime endTime;

    @JsonProperty("dDay")
    private final String dDay;

    private final boolean ddayEnabled;

    private final LocalDateTime deletedAt;

    public static ScheduleResponse from(Schedule schedule, LocalDate today) {
        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();

        String dDayStr;
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            dDayStr = "D-Day";
        } else if (today.isBefore(startDate)) {
            long daysBetween = ChronoUnit.DAYS.between(today, startDate);
            dDayStr = "D-" + daysBetween;
        } else {
            long daysAfter = ChronoUnit.DAYS.between(endDate, today);
            dDayStr = "D+" + daysAfter;
        }

        return from(schedule, dDayStr);
    }

    public static ScheduleResponse fromDday(Schedule schedule, LocalDateTime now) {
        if (schedule.isOngoingAt(now)) {
            return from(schedule, "진행 중");
        }
        return from(schedule, now.toLocalDate());
    }

    private static ScheduleResponse from(Schedule schedule, String dDayStr) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getScheduleType(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                dDayStr,
                schedule.isDdayEnabled(),
                schedule.getDeletedAt());
    }
}
