package bhoon.sugang_helper.campus.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleSummary(
        Long id,
        String scheduleType,
        LocalDate startDate,
        LocalDate endDate,
        @JsonFormat(pattern = ScheduleSummary.TIME_FORMAT) LocalTime startTime,
        @JsonFormat(pattern = ScheduleSummary.TIME_FORMAT) LocalTime endTime,
        @JsonProperty("dDay") String dDay) {

    private static final String TIME_FORMAT = "HH:mm";
}
