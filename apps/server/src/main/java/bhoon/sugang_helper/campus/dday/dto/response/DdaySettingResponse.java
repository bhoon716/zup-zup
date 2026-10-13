package bhoon.sugang_helper.campus.dday.dto.response;

import bhoon.sugang_helper.campus.dday.entity.DdaySetting;
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
public class DdaySettingResponse {

    private Long id;

    private String title;

    private LocalDate targetDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime targetTime;

    @JsonProperty("dDay")
    private String dDay;

    public static DdaySettingResponse from(DdaySetting setting) {
        return from(setting, LocalDate.now());
    }

    public static DdaySettingResponse from(DdaySetting setting, LocalDate baseDate) {
        long daysBetween = ChronoUnit.DAYS.between(baseDate, setting.getTargetDate());

        String dDayStr;
        if (daysBetween == 0) {
            dDayStr = "D-Day";
        } else if (daysBetween > 0) {
            dDayStr = "D-" + daysBetween;
        } else {
            dDayStr = "D+" + Math.abs(daysBetween);
        }

        return DdaySettingResponse.builder()
                .id(setting.getId())
                .title(setting.getTitle())
                .targetDate(setting.getTargetDate())
                .targetTime(setting.getTargetTime())
                .dDay(dDayStr)
                .build();
    }
}
