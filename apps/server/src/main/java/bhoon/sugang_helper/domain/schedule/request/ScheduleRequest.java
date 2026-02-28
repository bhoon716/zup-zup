package bhoon.sugang_helper.domain.schedule.request;

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

    @NotBlank(message = "일정 이름은 필수입니다.")
    @Schema(description = "일정 이름", example = "2026년 1학기 수강신청")
    private String title;

    @NotNull(message = "일정 날짜는 필수입니다.")
    @Schema(description = "일정 날짜", example = "2026-02-14")
    private LocalDate scheduleDate;

    @Schema(description = "일정 시간 (선택사항)", example = "10:00:00")
    private LocalTime scheduleTime;

    public ScheduleRequest(String title, LocalDate scheduleDate, LocalTime scheduleTime) {
        this.title = title;
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
    }
}
