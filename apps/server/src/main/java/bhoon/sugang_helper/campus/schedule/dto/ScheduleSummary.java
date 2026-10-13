package bhoon.sugang_helper.campus.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleSummary(
        Long id,
        String scheduleType,
        LocalDate startDate,
        LocalDate endDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @JsonProperty("dDay") String dDay) {
}
