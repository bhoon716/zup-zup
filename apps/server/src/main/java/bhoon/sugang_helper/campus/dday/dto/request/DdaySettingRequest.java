package bhoon.sugang_helper.campus.dday.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DdaySettingRequest {

    @NotBlank(message = "D-Day 명칭은 필수입니다.")
    private String title;

    @NotNull(message = "목표 날짜는 필수입니다.")
    private LocalDate targetDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime targetTime;

    public DdaySettingRequest(String title, LocalDate targetDate, LocalTime targetTime) {
        this.title = title;
        this.targetDate = targetDate;
        this.targetTime = targetTime;
    }
}
