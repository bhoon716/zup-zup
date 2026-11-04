package bhoon.sugang_helper.campus.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleRequest {

    @NotBlank(message = "일정 구분은 필수입니다.")
    @Size(max = 255, message = "일정 구분은 255자 이하여야 합니다.")
    private String scheduleType;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Boolean ddayEnabled;

    public ScheduleRequest(String scheduleType, LocalDate startDate, LocalDate endDate,
                           LocalTime startTime, LocalTime endTime) {
        this(scheduleType, startDate, endDate, startTime, endTime, null);
    }

    public ScheduleRequest(String scheduleType, LocalDate startDate, LocalDate endDate,
                           LocalTime startTime, LocalTime endTime, Boolean ddayEnabled) {
        this.scheduleType = scheduleType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ddayEnabled = ddayEnabled;
    }

    @JsonIgnore
    @AssertTrue(message = "종료 일시는 시작 일시보다 빠를 수 없습니다.")
    public boolean isValidPeriod() {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return false;
        }
        return !endDate.atTime(endTime == null ? LocalTime.MAX : endTime)
                .isBefore(startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime));
    }
}
