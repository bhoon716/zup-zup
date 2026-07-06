package bhoon.sugang_helper.dday.application;

import bhoon.sugang_helper.dday.domain.DdaySetting;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "D-Day 설정 응답 DTO")
public class DdaySettingResponse {

    @Schema(description = "D-Day ID", example = "1")
    private Long id;

    @Schema(description = "D-Day 명칭", example = "1학기 종강")
    private String title;

    @Schema(description = "목표 날짜", example = "2026-06-19")
    private LocalDate targetDate;

    @Schema(description = "목표 시간", type = "string", example = "18:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime targetTime;

    @Schema(description = "D-Day 남은 일수 계산 결과", example = "D-15")
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
