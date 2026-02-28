package bhoon.sugang_helper.domain.schedule.request;

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
@Schema(description = "일정 생성/수정 요청 DTO")
public class ScheduleRequest {

    @NotBlank(message = "일정 구분은 필수입니다.")
    @Schema(description = "일정 구분", example = "장바구니(예비)")
    private String scheduleType;

    @NotNull(message = "시작일은 필수입니다.")
    @Schema(description = "시작일", example = "2026-02-02")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @Schema(description = "종료일", example = "2026-02-04")
    private LocalDate endDate;

    @Schema(description = "시작 시간 (선택사항)", type = "string", example = "10:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Schema(description = "종료 시간 (선택사항)", type = "string", example = "18:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public ScheduleRequest(String scheduleType, LocalDate startDate, LocalDate endDate,
            LocalTime startTime, LocalTime endTime) {
        this.scheduleType = scheduleType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
