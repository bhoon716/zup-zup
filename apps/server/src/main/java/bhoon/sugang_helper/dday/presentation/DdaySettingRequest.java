package bhoon.sugang_helper.dday.presentation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "D-Day 설정 생성/수정 요청 DTO")
public class DdaySettingRequest {

    @NotBlank(message = "D-Day 명칭은 필수입니다.")
    @Schema(description = "D-Day 명칭", example = "1학기 종강")
    private String title;

    @NotNull(message = "목표 날짜는 필수입니다.")
    @Schema(description = "목표 날짜", example = "2026-06-19")
    private LocalDate targetDate;

    @Schema(description = "목표 시간 (선택사항)", type = "string", example = "18:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime targetTime;

    public DdaySettingRequest(String title, LocalDate targetDate, LocalTime targetTime) {
        this.title = title;
        this.targetDate = targetDate;
        this.targetTime = targetTime;
    }
}
